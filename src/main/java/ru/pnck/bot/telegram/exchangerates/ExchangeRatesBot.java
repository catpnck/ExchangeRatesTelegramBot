package ru.pnck.bot.telegram.exchangerates;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.xml.sax.SAXException;
import ru.pnck.bot.telegram.exchangerates.cbr.CbrRequestHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDate;

public class ExchangeRatesBot extends TelegramLongPollingBot {

    public ExchangeRatesBot(DefaultBotOptions options, String botToken) {
        super(options, botToken);
    }

    @Override
    public String getBotUsername() {
        return "ExchangeRatesPnckBot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.hasMessage()) {
                return;
            }
            if (update.hasMessage() && update.getMessage().hasText()) {
                var inMess = update.getMessage();
                var chatId = inMess.getChatId().toString();
                var outMess = new SendMessage();

                var data = CbrRequestHandler.getCurrentRateByCurrency(LocalDate.now(), update.getMessage().getText());
                var currencyData = data.getDataByCurrencyCode("USD");

                String messageText;
                if (currencyData.isPresent()) {
                    messageText = FormatHelper.formatCurrencyDataToString(data.getDate(), currencyData.get());
                } else {
                    messageText = createErrorMessage();
                }

                outMess.enableMarkdown(true);
                outMess.setChatId(chatId);
                outMess.setText(messageText);

                execute(outMess);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private String createErrorMessage() {
        return "Не найдена информация о курсе валют на сегодня";
    }
}
