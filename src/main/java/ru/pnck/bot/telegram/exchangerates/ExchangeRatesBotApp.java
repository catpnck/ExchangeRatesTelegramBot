package ru.pnck.bot.telegram.exchangerates;

import org.apache.log4j.BasicConfigurator;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.logging.Logger;

public class ExchangeRatesBotApp {
    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            var token = System.getenv("EXCHANGE_RATES_BOT_TOKEN");
            if (token == null) {
                System.out.println("The EXCHANGE_RATES_BOT_TOKEN environment variable is not set."); // TODO logger
                return;
            }
            var name = System.getenv("EXCHANGE_RATES_BOT_NAME");
            if (name == null) {
                System.out.println("Cannot find the EXCHANGE_RATES_BOT_NAME environment variable.");
                return;
            }
            var telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new ExchangeRatesBot(new DefaultBotOptions(), name, token));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
