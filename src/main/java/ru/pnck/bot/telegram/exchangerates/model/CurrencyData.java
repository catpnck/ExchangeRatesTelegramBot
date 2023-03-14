package ru.pnck.bot.telegram.exchangerates.model;

import java.math.BigDecimal;
import java.util.Objects;

public class CurrencyData {
    private final Currency currency;
    private int nominal;
    private BigDecimal value;

    public CurrencyData(Currency currency) {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getNominal() {
        return nominal;
    }

    public void setNominal(int nominal) {
        this.nominal = nominal;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CurrencyData that = (CurrencyData) o;
        return nominal == that.nominal && Objects.equals(currency, that.currency) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, nominal, value);
    }
}
