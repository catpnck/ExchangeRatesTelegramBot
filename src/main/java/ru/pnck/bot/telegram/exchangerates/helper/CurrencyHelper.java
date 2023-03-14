package ru.pnck.bot.telegram.exchangerates.helper;

import ru.pnck.bot.telegram.exchangerates.model.Currency;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CurrencyHelper {
    private static final Map<String, Currency> currencyMap = new HashMap<>();

    public static void addCurrency(Currency currency) {
        currencyMap.put(currency.getCharCode(), currency);
    }

    public static Optional<Currency> getCurrencyByCode(String code) {
        return Optional.ofNullable(currencyMap.get(code));
    }
}
