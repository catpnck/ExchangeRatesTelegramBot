package ru.pnck.bot.telegram.exchangerates.cbr;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.helper.CurrencyHelper;
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
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeMap;

public class CbrRequestHandler {
    private static final String URL_PATTERN = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=%s";

    private static final TreeMap<LocalDate, DateCurrencyData> dataMap = new TreeMap<>(Comparator.reverseOrder());

    public static DateCurrencyData getCurrentRateByCurrency(LocalDate date, String currencyCode)
            throws ParserConfigurationException, IOException, SAXException {
        if (!dataMap.containsKey(date)) {
            parseCurrencyDataByDate(date);
        }
        return dataMap.get(date);
    }

    private static void parseCurrencyDataByDate(LocalDate date)
            throws ParserConfigurationException, IOException, SAXException {
        var urlString = String.format(URL_PATTERN, date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        var url = new URL(urlString);
        var conn = url.openConnection();

        try (var is = conn.getInputStream()) {
            var dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var doc = dBuilder.parse(is);
            var documentElement = doc.getDocumentElement();
            var dateFromXml = LocalDate.parse(documentElement.getAttribute("Date"), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            var nodes = documentElement.getChildNodes();

            dataMap.put(date, parseDateCurrencyData(nodes, dateFromXml));
        }
    }

    private static DateCurrencyData parseDateCurrencyData(NodeList nodes, LocalDate date) {
        var dateCurrencyData = new DateCurrencyData(date);
        for (var i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            var rateAttrs = node.getChildNodes();
            var data = parseCurrencyData(rateAttrs, date);

            dateCurrencyData.addData(data);
        }

        return dateCurrencyData;
    }

    private static CurrencyData parseCurrencyData(NodeList rateAttrs, LocalDate date) {
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

        var currencyOptional = CurrencyHelper.getCurrencyByCode(charCode);
        if (currencyOptional.isEmpty()) {
            var currency = new Currency(numCode, charCode, name);
            CurrencyHelper.addCurrency(currency);
            currencyOptional = Optional.of(currency);
        }

        var currencyData = new CurrencyData(currencyOptional.get());
        currencyData.setNominal(nominal);
        currencyData.setValue(value);

        return currencyData;
    }
}
