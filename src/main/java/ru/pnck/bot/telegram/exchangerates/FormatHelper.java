package ru.pnck.bot.telegram.exchangerates;

import ru.pnck.bot.telegram.exchangerates.model.CurrencyData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormatHelper {
    public static String formatCurrencyDataToString(LocalDate date, CurrencyData data) {
        return String.format("""
                        *Актуальный курс валюты на сегодня: *
                        									
                        *Дата:* %s
                        *Код валюты:* %s
                        *Наименование:* %s
                        *Курс:* %d = %.4f""", date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                data.getCurrency().getCharCode(), data.getCurrency().getName(), data.getNominal(), data.getValue());
    }
}
