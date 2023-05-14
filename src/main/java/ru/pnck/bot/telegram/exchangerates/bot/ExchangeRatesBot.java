package ru.pnck.bot.telegram.exchangerates.bot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.util.HibernateUtil;
import ru.pnck.bot.telegram.exchangerates.util.MessageCreator;
import ru.pnck.bot.telegram.exchangerates.model.BotFunction;
import ru.pnck.bot.telegram.exchangerates.model.ConverterHandlerState;
import ru.pnck.bot.telegram.exchangerates.model.ExchangeRatesHandlerState;
import ru.pnck.bot.telegram.exchangerates.model.BotUser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ExchangeRatesBot extends TelegramLongPollingBot {
    public static final String RETURN_TO_MAIN_MENU = "Вернуться в главное меню";
    private final String botUsername;

    public ExchangeRatesBot(DefaultBotOptions options, String botUsername, String botToken) {
        super(options, botToken);
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var em = HibernateUtil.getSessionFactory().createEntityManager();
        if (!update.hasMessage()) {
            return;
        }

        var inMess = update.getMessage();
        var chatId = inMess.getChatId().toString();
        var user = findUser(em, chatId);
        try {
            if (!inMess.hasText()) {
                sendUnrecognizedMessage(chatId);
                return;
            }

            if (inMess.getText().equals("/start")) {
                sendGreetingMessage(user);
                user.setLastBotFunction(BotFunction.DEFAULT);
            }

            switch (user.getLastBotFunction()) {
                case DEFAULT -> sendSelectFunctionMessage(user);
                case FUNCTION_SELECT -> handleFunctionSelect(inMess, user);
                case RATES -> Objects.requireNonNull(createHandler(BotFunction.RATES)).onUpdateReceived(inMess, user);
                case CONVERTER -> Objects.requireNonNull(createHandler(BotFunction.CONVERTER)).onUpdateReceived(inMess, user);
            }
        } catch (TelegramApiException | ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            resetHandlerStates(user);
            user.setLastBotFunction(BotFunction.DEFAULT);
        } finally {
            HibernateUtil.getSessionFactory().inTransaction(s -> s.merge(user));
            em.close();
        }
    }

    private void sendSelectFunctionMessage(BotUser user) throws TelegramApiException {
        var replyKeyboard = new ReplyKeyboardMarkup();
        var row = new KeyboardRow();
        for (var value : BotFunction.values()) {
            if (value.getFunctionString() != null) {
                row.add(value.getFunctionString());
            }
        }
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(true);
        replyKeyboard.setKeyboard(List.of(row));
        sendMessage(user.getChatId(), "Выберите функцию", replyKeyboard);
        user.setLastBotFunction(BotFunction.FUNCTION_SELECT);
    }

    private void handleFunctionSelect(Message inMess, BotUser user) throws TelegramApiException {
        resetHandlerStates(user);
        var text = inMess.getText();
        for (var value : BotFunction.values()) {
            if (text.equals(value.getFunctionString())) {
                user.setLastBotFunction(value);
                Objects.requireNonNull(createHandler(value)).sendGreetingMessage(user);
                return;
            }
        }
        sendUnrecognizedMessage(user.getChatId());
    }

    private void resetHandlerStates(BotUser user) {
        user.setLastExchangeRatesHandlerState(ExchangeRatesHandlerState.DEFAULT);
        user.setLastConverterHandlerState(ConverterHandlerState.DEFAULT);
    }

    private void sendGreetingMessage(BotUser user) throws TelegramApiException {
        sendMessage(user.getChatId(), MessageCreator.createGreetingMessage());
    }

    protected void sendUnrecognizedMessage(String chatId) throws TelegramApiException {
        sendMessage(chatId, MessageCreator.createUnrecognizedMessage());
    }

    void sendMessage(String chatId, String text) throws TelegramApiException {
        sendMessage(chatId, text, null, true);
    }

    void sendMessage(String chatId, String text, ReplyKeyboard keyboardMarkup) throws TelegramApiException {
        sendMessage(chatId, text, keyboardMarkup, false);
    }

    void sendMessage(String chatId, String text, ReplyKeyboard keyboardMarkup, boolean needToRemoveKeyboard)
            throws TelegramApiException {
        var outMess = new SendMessage();
        outMess.setText(text);
        outMess.enableMarkdown(true);
        outMess.setChatId(chatId);
        if (keyboardMarkup == null) {
            var removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(needToRemoveKeyboard);
            outMess.setReplyMarkup(removeKeyboard);
        } else {
            outMess.setReplyMarkup(keyboardMarkup);
        }
        execute(outMess);
    }

    private BotUser findUser(EntityManager em, String chatId) {
        BotUser user;
        try {
            user = em.createQuery("SELECT u FROM BotUser u WHERE chatId = :chatId", BotUser.class)
                    .setParameter("chatId", chatId)
                    .getSingleResult();
        } catch (NoResultException e) {
            var newUser = user = new BotUser(chatId);
            HibernateUtil.getSessionFactory().inTransaction(s -> s.persist(newUser));
        }

        return user;
    }

    private AbstractHandler createHandler(BotFunction function) {
        switch (function) {
            case RATES -> {
                return new ExchangeRatesHandler(this);
            }
            case CONVERTER -> {
                return new CurrencyConverterHandler(this);
            }
        }

        return null;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public void returnToMainMenu(BotUser user) throws TelegramApiException {
        sendSelectFunctionMessage(user);
    }
}
