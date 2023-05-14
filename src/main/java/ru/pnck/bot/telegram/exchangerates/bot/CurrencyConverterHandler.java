package ru.pnck.bot.telegram.exchangerates.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.util.MessageCreator;
import ru.pnck.bot.telegram.exchangerates.cbr.CbrRequestHandler;
import ru.pnck.bot.telegram.exchangerates.util.CurrencyHolder;
import ru.pnck.bot.telegram.exchangerates.model.BotUser;
import ru.pnck.bot.telegram.exchangerates.model.ConverterHandlerState;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static ru.pnck.bot.telegram.exchangerates.bot.ExchangeRatesBot.RETURN_TO_MAIN_MENU;

public class CurrencyConverterHandler extends AbstractHandler {
    private static final String RETURN_TO_SELECT_FROM_CURRENCY = "Вернуться к выбору исходной валюты";

    public CurrencyConverterHandler(ExchangeRatesBot bot) {
        super(bot);
    }

    @Override
    void onUpdateReceived(Message inMess, BotUser user)
            throws TelegramApiException, ParserConfigurationException, IOException, SAXException {
        if (inMess.getText().equals(RETURN_TO_SELECT_FROM_CURRENCY)) {
            user.setLastConverterHandlerState(ConverterHandlerState.DEFAULT);
        }
        if (inMess.getText().equals(RETURN_TO_MAIN_MENU)) {
            handleReturnToMainMenu(user);
            return;
        }
        switch (user.getLastConverterHandlerState()) {
            case DEFAULT -> sendSelectFromCurrencyMessage(user);
            case FROM_CURRENCY_SELECTED -> handleFromCurrency(inMess, user);
            case TO_CURRENCY_SELECTED -> handleToCurrency(inMess, user);
            case SUM_RECEIVED -> sendConvertedValue(inMess, user);
        }
    }

    private void sendSelectFromCurrencyMessage(BotUser user) throws TelegramApiException {
        bot.sendMessage(user.getChatId(), MessageCreator.createChooseFromCurrencyMessage(),
                createCurrencyKeyboard(LocalDate.now(), null, true));
        user.setLastConverterHandlerState(ConverterHandlerState.FROM_CURRENCY_SELECTED);
    }

    private void handleFromCurrency(Message inMess, BotUser user) throws TelegramApiException {
        var text = inMess.getText();
        var currency = CurrencyHolder.getCurrencyByCode(text);
        if (currency.isEmpty()) {
            bot.sendUnrecognizedMessage(user.getChatId());
            sendSelectFromCurrencyMessage(user);
            return;
        }
        user.setLastSelectedFromCurrency(text);
        sendSelectToCurrencyMessage(user);
    }

    private void sendSelectToCurrencyMessage(BotUser user) throws TelegramApiException {
        bot.sendMessage(user.getChatId(), MessageCreator.createChooseToCurrencyMessage(),
                createCurrencyKeyboard(LocalDate.now(), RETURN_TO_SELECT_FROM_CURRENCY, true));
        user.setLastConverterHandlerState(ConverterHandlerState.TO_CURRENCY_SELECTED);
    }

    private void handleToCurrency(Message inMess, BotUser user) throws TelegramApiException {
        var text = inMess.getText();
        var currency = CurrencyHolder.getCurrencyByCode(text);
        if (currency.isEmpty()) {
            bot.sendUnrecognizedMessage(user.getChatId());
            sendSelectFromCurrencyMessage(user);
            return;
        }
        user.setLastSelectedToCurrency(text);
        sendSumInputMessage(user);
    }

    private void sendSumInputMessage(BotUser user) throws TelegramApiException {
        bot.sendMessage(user.getChatId(), MessageCreator.createInputSumMessage());
        user.setLastConverterHandlerState(ConverterHandlerState.SUM_RECEIVED);
    }

    private void sendConvertedValue(Message inMess, BotUser user)
            throws TelegramApiException, ParserConfigurationException, IOException, SAXException {
        var text = inMess.getText();
        BigDecimal sum;
        try {
            sum = new BigDecimal(text).setScale(4, RoundingMode.CEILING);
        } catch (NumberFormatException e) {
            bot.sendUnrecognizedMessage(user.getChatId());
            return;
        }
        var fromCurrencyData = CbrRequestHandler.getCurrentRateByCurrency(LocalDate.now(), user.getLastSelectedFromCurrency()).getRight().get();
        var toCurrencyData = CbrRequestHandler.getCurrentRateByCurrency(LocalDate.now(), user.getLastSelectedToCurrency()).getRight().get();

        sum = sum.divide(BigDecimal.valueOf(fromCurrencyData.getNominal()), RoundingMode.CEILING).multiply(fromCurrencyData.getValue());
        sum = sum.multiply(BigDecimal.valueOf(toCurrencyData.getNominal())).divide(toCurrencyData.getValue(), RoundingMode.CEILING);

        bot.sendMessage(user.getChatId(), sum.toString());
        sendSelectFromCurrencyMessage(user);
    }

    @Override
    void sendGreetingMessage(BotUser user) throws TelegramApiException {
        sendSelectFromCurrencyMessage(user);
    }
}
