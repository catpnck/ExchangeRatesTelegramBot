package ru.pnck.bot.telegram.exchangerates.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "bot_user")
public class BotUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_id", unique = true, nullable = false, length = 255)
    private String chatId;

    @Enumerated
    @Column(name = "last_bot_function", columnDefinition = "SMALLINT DEFAULT NULL")
    private BotFunction lastBotFunction;

    @Enumerated
    @Column(name = "last_exchange_rates_handler_state", columnDefinition = "SMALLINT DEFAULT NULL")
    private ExchangeRatesHandlerState lastExchangeRatesHandlerState;

    @Column(name = "last_selected_date")
    private LocalDate lastSelectedDate;

    @Enumerated
    @Column(name = "last_converter_handler_stater", columnDefinition = "SMALLINT DEFAULT NULL")
    private ConverterHandlerState lastConverterHandlerState;

    @Column(name = "last_selected_from_currency", unique = true, length = 3)
    private String lastSelectedFromCurrency;

    @Column(name = "last_selected_to_currency", unique = true, length = 3)
    private String lastSelectedToCurrency;

    protected BotUser() {
    }

    public BotUser(String chatId) {
        this.chatId = chatId;
        this.lastBotFunction = BotFunction.DEFAULT;
        this.lastExchangeRatesHandlerState = ExchangeRatesHandlerState.DEFAULT;
        this.lastConverterHandlerState = ConverterHandlerState.DEFAULT;
    }

    public Integer getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public BotFunction getLastBotFunction() {
        return lastBotFunction;
    }

    public void setLastBotFunction(BotFunction lastBotFunction) {
        this.lastBotFunction = lastBotFunction;
    }

    public ExchangeRatesHandlerState getLastExchangeRatesHandlerState() {
        return lastExchangeRatesHandlerState;
    }

    public void setLastExchangeRatesHandlerState(ExchangeRatesHandlerState lastExchangeRatesHandlerState) {
        this.lastExchangeRatesHandlerState = lastExchangeRatesHandlerState;
    }

    public LocalDate getLastSelectedDate() {
        return lastSelectedDate;
    }

    public void setLastSelectedDate(LocalDate lastSelectedDate) {
        this.lastSelectedDate = lastSelectedDate;
    }

    public ConverterHandlerState getLastConverterHandlerState() {
        return lastConverterHandlerState;
    }

    public void setLastConverterHandlerState(ConverterHandlerState lastConverterHandlerState) {
        this.lastConverterHandlerState = lastConverterHandlerState;
    }

    public String getLastSelectedFromCurrency() {
        return lastSelectedFromCurrency;
    }

    public void setLastSelectedFromCurrency(String lastSelectedFromCurrency) {
        this.lastSelectedFromCurrency = lastSelectedFromCurrency;
    }

    public String getLastSelectedToCurrency() {
        return lastSelectedToCurrency;
    }

    public void setLastSelectedToCurrency(String lastSelectedToCurrency) {
        this.lastSelectedToCurrency = lastSelectedToCurrency;
    }
}
