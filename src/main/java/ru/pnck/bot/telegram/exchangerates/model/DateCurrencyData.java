package ru.pnck.bot.telegram.exchangerates.model;

import jakarta.persistence.*;
import ru.pnck.bot.telegram.exchangerates.helper.CurrencyHolder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "date_currency_data")
public class DateCurrencyData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date;
    private LocalDate actualityDate;
    @OneToMany(cascade = CascadeType.PERSIST)
    private Map<Currency, CurrencyData> data;

    protected DateCurrencyData() {

    }

    public DateCurrencyData(LocalDate date, LocalDate actualityDate) {
        this.data = new HashMap<>();
        this.date = date;
        this.actualityDate = actualityDate;
    }

    public void addData(CurrencyData currencyData) {
        data.put(currencyData.getCurrency(), currencyData);
    }

    public Optional<CurrencyData> getDataByCurrencyCode(String code) {
        var currency = CurrencyHolder.getCurrencyByCode(code);
        return currency.map(data::get);
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getActualityDate() {
        return actualityDate;
    }

    public Long getId() {
        return id;
    }
}
