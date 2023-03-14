package ru.pnck.bot.telegram.exchangerates.model;

import ru.pnck.bot.telegram.exchangerates.helper.CurrencyHelper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DateCurrencyData {
    private final LocalDate date;
    private final Map<Currency, CurrencyData> data;

    public DateCurrencyData(LocalDate date) {
        this.data = new HashMap<>();
        this.date = date;
    }

    public void addData(CurrencyData currencyData) {
        data.put(currencyData.getCurrency(), currencyData);
    }

    public Optional<CurrencyData> getDataByCurrencyCode(String code) {
        var currency = CurrencyHelper.getCurrencyByCode(code);
        return currency.map(data::get);
    }

    public LocalDate getDate() {
        return date;
    }
}
