package ru.pnck.bot.telegram.exchangerates.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.MessageCreator;
import ru.pnck.bot.telegram.exchangerates.cbr.CbrRequestHandler;
import ru.pnck.bot.telegram.exchangerates.model.BotUser;
import ru.pnck.bot.telegram.exchangerates.model.ExchangeRatesHandlerState;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static ru.pnck.bot.telegram.exchangerates.bot.ExchangeRatesBot.RETURN_TO_MAIN_MENU;

public class ExchangeRatesHandler extends AbstractHandler {
    private static final String TODAY = "Сегодня";
    private static final String RETURN_TO_DATE_SELECT = "Вернуться к выбору даты";

    public ExchangeRatesHandler(ExchangeRatesBot bot) {
        super(bot);
    }

    @Override
    public void onUpdateReceived(Message inMess, BotUser user)
            throws TelegramApiException, ParserConfigurationException, IOException, SAXException {
        if (inMess.getText().equals(RETURN_TO_DATE_SELECT)) {
            user.setLastExchangeRatesHandlerState(ExchangeRatesHandlerState.DEFAULT);
        }
        if (inMess.getText().equals(RETURN_TO_MAIN_MENU)) {
            handleReturnToMainMenu(user);
            return;
        }
        switch (user.getLastExchangeRatesHandlerState()) {
            case DEFAULT -> sendSelectDataMessage(user);
            case DATE_SELECT -> processDateFromUser(inMess, user);
            case CURRENCY_SELECT -> sendExchangeRate(inMess, user);
        }
    }

    private void sendExchangeRate(Message inMess, BotUser user)
            throws ParserConfigurationException, IOException, SAXException, TelegramApiException {
        var text = inMess.getText();

        try {
            var data = CbrRequestHandler.getCurrentRateByCurrency(user.getLastSelectedDate(), text);
            if (data.getRight().isEmpty()) {
                bot.sendMessage(user.getChatId(), MessageCreator.createErrorMessage(user.getLastSelectedDate()));
                return;
            }
            bot.sendMessage(user.getChatId(),
                    MessageCreator.formatCurrencyDataToString(user.getLastSelectedDate(), data.getLeft(), data.getRight().get()));
        } finally {
            sendCurrenciesKeyboard(user);
        }
    }

    private void sendCurrenciesKeyboard(BotUser user) throws TelegramApiException {
        bot.sendMessage(user.getChatId(), MessageCreator.createChooseCurrencyMessage(),
                createCurrencyKeyboard(user.getLastSelectedDate(), RETURN_TO_DATE_SELECT));
        user.setLastExchangeRatesHandlerState(ExchangeRatesHandlerState.CURRENCY_SELECT);
    }

    private void sendSelectDataMessage(BotUser user) throws TelegramApiException {
        var replyKeyboard = new ReplyKeyboardMarkup();
        var row = new KeyboardRow();
        row.add(TODAY);
        var returnRow = new KeyboardRow();
        returnRow.add(RETURN_TO_MAIN_MENU);
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(true);
        replyKeyboard.setKeyboard(List.of(row, returnRow));
        bot.sendMessage(user.getChatId(), MessageCreator.createChooseDateMessage(), replyKeyboard);
        user.setLastExchangeRatesHandlerState(ExchangeRatesHandlerState.DATE_SELECT);
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
        bot.sendMessage(chatId, MessageCreator.createDateNotRecognizedMessage());
    }

    @Override
    public void sendGreetingMessage(BotUser user) throws TelegramApiException {
        sendSelectDataMessage(user);
    }
}
