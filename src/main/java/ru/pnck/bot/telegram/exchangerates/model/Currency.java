package ru.pnck.bot.telegram.exchangerates.model;

import java.util.Objects;

public class Currency {
    private String numCode;
    private String charCode;
    private String name;

    public Currency(String numCode, String charCode, String name) {
        this.numCode = numCode;
        this.charCode = charCode;
        this.name = name;
    }

    public String getNumCode() {
        return numCode;
    }

    public void setNumCode(String numCode) {
        this.numCode = numCode;
    }

    public String getCharCode() {
        return charCode;
    }

    public void setCharCode(String charCode) {
        this.charCode = charCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Currency currency = (Currency) o;
        return Objects.equals(numCode, currency.numCode) && Objects.equals(charCode,
                currency.charCode) && Objects.equals(name, currency.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numCode, charCode, name);
    }
}
