package ru.pnck.bot.telegram.exchangerates.cbr;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.HibernateUtil;
import ru.pnck.bot.telegram.exchangerates.helper.CurrencyHolder;
import ru.pnck.bot.telegram.exchangerates.model.Currency;
import ru.pnck.bot.telegram.exchangerates.model.CurrencyData;
import ru.pnck.bot.telegram.exchangerates.model.DateCurrencyData;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static ru.pnck.bot.telegram.exchangerates.bot.AbstractHandler.DEFAULT_CURRENCY_CODE;

public class CbrRequestHandler {
    private static final String URL_PATTERN = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=%s";

    public static Pair<LocalDate, Optional<CurrencyData>> getCurrentRateByCurrency(LocalDate date, String currencyCode)
            throws ParserConfigurationException, IOException, SAXException {
        var em = HibernateUtil.getSessionFactory().createEntityManager();
        try {
            var dataList = em.createQuery("SELECT dateData FROM DateCurrencyData dateData " +
                    "WHERE dateData.date = :date", DateCurrencyData.class)
                    .setParameter("date", date)
                    .getResultList();
            Optional<CurrencyData> data;
            LocalDate actualityDate;
            if (dataList.size() == 0) {
                var dateCurrencyData = fillCurrencyDataByDate(date);
                if (dateCurrencyData.isEmpty()) {
                    return Pair.of(null, Optional.empty());
                }
                actualityDate = dateCurrencyData.get().getActualityDate();
                data = dateCurrencyData.get().getDataByCurrencyCode(currencyCode);
            } else {
                var dateCurrencyData = dataList.get(0);
                data = dateCurrencyData.getDataByCurrencyCode(currencyCode);
                actualityDate = dateCurrencyData.getActualityDate();
            }

            return Pair.of(actualityDate, data);
        } finally {
            em.close();
        }
    }

    public static Optional<DateCurrencyData> fillCurrencyDataByDate(LocalDate date)
            throws ParserConfigurationException, IOException, SAXException {
        var data = parseCurrencyDataByDate(date);
        var defaultCurrencyData = addDefaultCurrency();
        data.ifPresent(d -> d.addData(defaultCurrencyData));
        data.ifPresent(dateCurrencyData ->
                HibernateUtil.getSessionFactory().inTransaction(s -> s.persist(dateCurrencyData)));
        return data;
    }

    private static CurrencyData addDefaultCurrency() {
        var currency = CurrencyHolder.getCurrencyByCode(DEFAULT_CURRENCY_CODE).orElse(null);
        if (currency == null) {
            currency = new Currency("643", "RUB", "Российских рублей");
            var curToPersist = currency;
            HibernateUtil.getSessionFactory().inTransaction(em -> em.persist(curToPersist));
            CurrencyHolder.addCurrency(currency);
        }

        var data = new CurrencyData(currency);
        data.setNominal(1);
        data.setValue(BigDecimal.ONE);

        return data;
    }

    private static Optional<DateCurrencyData> parseCurrencyDataByDate(LocalDate date)
            throws ParserConfigurationException, IOException, SAXException {
        var urlString = String.format(URL_PATTERN, date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        var url = new URL(urlString);
        var conn = url.openConnection();

        try (var is = conn.getInputStream()) {
            var dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var doc = dBuilder.parse(is);
            var documentElement = doc.getDocumentElement();
            if (!documentElement.hasAttributes()) {
                return Optional.empty();
            }
            var dateFromXml = LocalDate.parse(documentElement.getAttribute("Date"), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            var nodes = documentElement.getChildNodes();

            return Optional.of(parseDateCurrencyData(nodes, date, dateFromXml));
        }
    }

    private static DateCurrencyData parseDateCurrencyData(NodeList nodes, LocalDate date, LocalDate dateFromXml) {
        var dateCurrencyData = new DateCurrencyData(date, dateFromXml);
        for (var i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            var rateAttrs = node.getChildNodes();
            var data = parseCurrencyData(rateAttrs);

            dateCurrencyData.addData(data);
        }

        return dateCurrencyData;
    }

    private static CurrencyData parseCurrencyData(NodeList rateAttrs) {
        var numCode = "";
        var charCode = "";
        var name = "";
        var nominal = 0;
        var value = BigDecimal.ZERO;
        for (var j = 0; j < rateAttrs.getLength(); j++) {
            var attr = rateAttrs.item(j);
            var val = attr.getChildNodes().item(0).getNodeValue();

            switch (attr.getNodeName()) {
                case "NumCode" -> numCode = val;
                case "CharCode" -> charCode = val;
                case "Nominal" -> nominal = Integer.parseInt(val);
                case "Name" -> name = val;
                case "Value" -> value = new BigDecimal(val.replace(',', '.'));
            }
        }

        var currencyOptional = CurrencyHolder.getCurrencyByCode(charCode);
        if (currencyOptional.isEmpty()) {
            var currency = new Currency(numCode, charCode, name);
            CurrencyHolder.addCurrency(currency);
            HibernateUtil.getSessionFactory().inTransaction(s -> s.persist(currency));
            currencyOptional = Optional.of(currency);
        }

        var currencyData = new CurrencyData(currencyOptional.get());
        currencyData.setNominal(nominal);
        currencyData.setValue(value);

        return currencyData;
    }
}
