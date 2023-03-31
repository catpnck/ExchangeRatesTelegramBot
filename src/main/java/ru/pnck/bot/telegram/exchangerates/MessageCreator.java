package ru.pnck.bot.telegram.exchangerates;

import ru.pnck.bot.telegram.exchangerates.model.CurrencyData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MessageCreator {
    public static String formatCurrencyDataToString(LocalDate date, LocalDate actualityDate, CurrencyData data) {
        var dateFormatted = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        var actualityDateFormatted = actualityDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return String.format("""
                        *Актуальный курс валюты на %s: *

                        *Дата:* %s
                        *Код валюты:* %s
                        *Курс:* %d %s = %.4f руб.""", dateFormatted, actualityDateFormatted,
                data.getCurrency().getCharCode(), data.getNominal(), data.getCurrency().getName(), data.getValue());
    }

    public static String createErrorMessage(LocalDate date) {
        return String.format("Не нашел курс валют на %s",
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }

    public static String createUnrecognizedMessage() {
        return "Не смог понять ваше сообщение :(";
    }

    public static String createGreetingMessage() {
        return "Привет! Я могу найти официальный курс валют ЦБ РФ на любую дату.";
    }

    public static String createDateNotRecognizedMessage() {
        return "Не смог распознать дату. Попробуйте еще раз";
    }

    public static String createChooseCurrencyMessage() {
        return "Выберите валюту";
    }

    public static String createChooseDateMessage() {
        return "Введите любую дату, например 31.03.2023 или нажмите на кнопку, чтобы получить актуальный курс.";
    }
}
