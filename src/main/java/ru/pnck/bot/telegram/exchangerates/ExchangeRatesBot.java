package ru.pnck.bot.telegram.exchangerates;

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
import ru.pnck.bot.telegram.exchangerates.cbr.CbrRequestHandler;
import ru.pnck.bot.telegram.exchangerates.helper.CurrencyHolder;
import ru.pnck.bot.telegram.exchangerates.model.BotState;
import ru.pnck.bot.telegram.exchangerates.model.BotUser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExchangeRatesBot extends TelegramLongPollingBot {
    private static final int KEYBOARD_ROW_SIZE = 5;
    private static final String TODAY = "Сегодня";
    private static final String[] DEFAULT_CURRENCY_CODES = new String[]{"USD", "EUR", "GBP", "CNY"};
    private final String botUsername;

    public ExchangeRatesBot(DefaultBotOptions options, String botUsername, String botToken) {
        super(options, botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var em = HibernateUtil.getSessionFactory().createEntityManager();
        try {
            onUpdateReceived(em, update);
        } finally {
            em.close();
        }
    }

    private void onUpdateReceived(EntityManager em, Update update) {
        try {
            if (!update.hasMessage()) {
                return;
            }

            if (update.hasMessage()) {
                var inMess = update.getMessage();
                var chatId = inMess.getChatId().toString();
                var user = findUser(em, chatId);
                if (!inMess.hasText()) {
                    sendUnrecognizedMessage(chatId);
                    return;
                }
                if (inMess.getText().equals("/start")) {
                    sendGreetingMessage(user);
                }
                switch (user.getLastBotState()) {
                    case UNAUTHORIZED -> sendSelectDataMessage(user);
                    case DATE_SELECT -> processDateFromUser(inMess, user);
                    case CURRENCY_SELECT -> sendExchangeRate(inMess, user);
                }
                HibernateUtil.getSessionFactory().inTransaction(s -> s.merge(user));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendExchangeRate(Message inMess, BotUser user)
            throws ParserConfigurationException, IOException, SAXException, TelegramApiException {
        try {
            var text = inMess.getText();
            var data = CbrRequestHandler.getCurrentRateByCurrency(user.getLastSelectedDate(), text);
            if (data.getRight().isEmpty()) {
                sendMessage(user.getChatId(), MessageCreator.createErrorMessage(user.getLastSelectedDate()));
                return;
            }
            sendMessage(user.getChatId(),
                    MessageCreator.formatCurrencyDataToString(user.getLastSelectedDate(), data.getLeft(), data.getRight().get()));
        } finally {
            sendSelectDataMessage(user);
        }
    }

    private void sendCurrenciesKeyboard(BotUser user) throws TelegramApiException {
        sendMessage(user.getChatId(), MessageCreator.createChooseCurrencyMessage(), createCurrencyKeyboard());
        user.setLastBotState(BotState.CURRENCY_SELECT);
    }

    private void sendGreetingMessage(BotUser user) throws TelegramApiException {
        sendMessage(user.getChatId(), MessageCreator.createGreetingMessage());
    }

    private void sendSelectDataMessage(BotUser user) throws TelegramApiException {
        var replyKeyboard = new ReplyKeyboardMarkup();
        var row = new KeyboardRow();
        row.add(TODAY);
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(true);
        replyKeyboard.setKeyboard(List.of(row));
        sendMessage(user.getChatId(), MessageCreator.createChooseDateMessage(), replyKeyboard);
        user.setLastBotState(BotState.DATE_SELECT);
    }

    private void processDateFromUser(Message inMess, BotUser user) throws TelegramApiException {
        var text = inMess.getText();
        LocalDate date;
        if (text.equals(TODAY)) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (DateTimeParseException e) {
                sendDateNotRecognizedMessage(inMess.getChatId().toString());
                return;
            }
        }
        user.setLastSelectedDate(date);
        sendCurrenciesKeyboard(user);
    }


    private void sendDateNotRecognizedMessage(String chatId) throws TelegramApiException {
        sendMessage(chatId, MessageCreator.createDateNotRecognizedMessage());
    }

    private void sendUnrecognizedMessage(String chatId) throws TelegramApiException {
        sendMessage(chatId, MessageCreator.createUnrecognizedMessage());
    }

    private void sendMessage(String chatId, String text) throws TelegramApiException {
        sendMessage(chatId, text, null, true);
    }

    private void sendMessage(String chatId, String text, boolean needToRemoveKeyboard) throws TelegramApiException {
        sendMessage(chatId, text, null, needToRemoveKeyboard);
    }

    private void sendMessage(String chatId, String text, ReplyKeyboard keyboardMarkup) throws TelegramApiException {
        sendMessage(chatId, text, keyboardMarkup, false);
    }

    private void sendMessage(String chatId, String text, ReplyKeyboard keyboardMarkup, boolean needToRemoveKeyboard)
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

    private ReplyKeyboardMarkup createCurrencyKeyboard() {
        var currencies = CurrencyHolder.getAllCurrencies();
        var keyboardMarkup = new ReplyKeyboardMarkup();
        var keyboard = new ArrayList<KeyboardRow>();

        var defaultCurrencyRow = new KeyboardRow();
        for (var curCode : DEFAULT_CURRENCY_CODES) {
            CurrencyHolder.getCurrencyByCode(curCode).ifPresent(c -> defaultCurrencyRow.add(c.getCharCode()));
        }

        if (!defaultCurrencyRow.isEmpty()) {
            keyboard.add(defaultCurrencyRow);
        }

        var row = new KeyboardRow();
        for (var currency : currencies) {
            if (Arrays.stream(DEFAULT_CURRENCY_CODES).anyMatch(c -> c.equals(currency.getCharCode()))) {
                continue;
            }
            if (row.size() == KEYBOARD_ROW_SIZE) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
            row.add(currency.getCharCode());
        }
        if (row.size() > 0) {
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setOneTimeKeyboard(true);

        return keyboardMarkup;
    }
}
