package ru.pnck.bot.telegram.exchangerates;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class ExchangeRatesBotApp {
    public static void main(String[] args) {
        try {
            var telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new ExchangeRatesBot(new DefaultBotOptions(),
                    "")); //TODO: config
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
