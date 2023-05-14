package ru.pnck.bot.telegram.exchangerates.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.cbr.CbrRequestHandler;
import ru.pnck.bot.telegram.exchangerates.util.CurrencyHolder;
import ru.pnck.bot.telegram.exchangerates.model.BotUser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static ru.pnck.bot.telegram.exchangerates.bot.ExchangeRatesBot.RETURN_TO_MAIN_MENU;

public abstract class AbstractHandler {
    private static final String[] DEFAULT_EXTERNAL_CURRENCY_CODES = new String[]{"USD", "EUR", "GBP", "CNY"};
    public static final String DEFAULT_CURRENCY_CODE = "RUB";
    private static final int KEYBOARD_ROW_SIZE = 5;

    protected final ExchangeRatesBot bot;

    public AbstractHandler(ExchangeRatesBot bot) {
        this.bot = bot;
    }

    abstract void onUpdateReceived(Message inMess, BotUser user) throws TelegramApiException, ParserConfigurationException, IOException, SAXException;

    abstract void sendGreetingMessage(BotUser user) throws TelegramApiException;

    protected ReplyKeyboardMarkup createCurrencyKeyboard(LocalDate date, String oneStepBackButton) {
        return createCurrencyKeyboard(date, oneStepBackButton, false);
    }

    protected ReplyKeyboardMarkup createCurrencyKeyboard(LocalDate date, String oneStepBackButton, boolean needAddDefaultCurrency) {
        var currencies = CurrencyHolder.getAllCurrencies();
        if (currencies.isEmpty()) {
            try {
                CbrRequestHandler.fillCurrencyDataByDate(date);
            } catch (Exception ignored) {
            }
        }
        var keyboardMarkup = new ReplyKeyboardMarkup();
        var keyboard = new ArrayList<KeyboardRow>();

        var firstRow = new KeyboardRow();
        if (oneStepBackButton != null) {
            firstRow.add(oneStepBackButton);
        }
        firstRow.add(RETURN_TO_MAIN_MENU);
        keyboard.add(firstRow);

        var defaultCurrencyRow = new KeyboardRow();
        if (needAddDefaultCurrency) {
            defaultCurrencyRow.add(DEFAULT_CURRENCY_CODE);
        }
        for (var curCode : DEFAULT_EXTERNAL_CURRENCY_CODES) {
            CurrencyHolder.getCurrencyByCode(curCode).ifPresent(c -> defaultCurrencyRow.add(c.getCharCode()));
        }

        if (!defaultCurrencyRow.isEmpty()) {
            keyboard.add(defaultCurrencyRow);
        }

        var row = new KeyboardRow();
        for (var currency : currencies) {
            if (Arrays.stream(DEFAULT_EXTERNAL_CURRENCY_CODES).anyMatch(c -> c.equals(currency.getCharCode()))
                    || DEFAULT_CURRENCY_CODE.equals(currency.getCharCode())) {
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

    protected void handleReturnToMainMenu(BotUser user) throws TelegramApiException {
        bot.returnToMainMenu(user);
    }
}
