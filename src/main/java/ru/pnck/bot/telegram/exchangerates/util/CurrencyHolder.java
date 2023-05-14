package ru.pnck.bot.telegram.exchangerates.util;

import ru.pnck.bot.telegram.exchangerates.util.HibernateUtil;
import ru.pnck.bot.telegram.exchangerates.model.Currency;

import java.util.*;

public class CurrencyHolder {
    private static final Map<String, Currency> currencyMap = new HashMap<>();

    static {
        var em = HibernateUtil.getSessionFactory().createEntityManager();
        var currencies = em.createQuery("SELECT c FROM Currency c", Currency.class).getResultList();
        for (var currency : currencies) {
            currencyMap.put(currency.getCharCode(), currency);
        }
        em.close();
    }

    public static void addCurrency(Currency currency) {
        currencyMap.put(currency.getCharCode(), currency);
    }

    public static Optional<Currency> getCurrencyByCode(String code) {
        return Optional.ofNullable(currencyMap.get(code));
    }

    public static Collection<Currency> getAllCurrencies() {
        return currencyMap.values();
    }
}
