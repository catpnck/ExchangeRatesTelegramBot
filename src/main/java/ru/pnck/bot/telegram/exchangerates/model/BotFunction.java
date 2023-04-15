package ru.pnck.bot.telegram.exchangerates.model;

public enum BotFunction {
    DEFAULT(null),
    FUNCTION_SELECT(null),
    RATES("Курс валют"),
    CONVERTER("Конвертер валют");

    private final String functionString;

    BotFunction(String functionString) {
        this.functionString = functionString;
    }

    public String getFunctionString() {
        return functionString;
    }
}
