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
    @Column(name = "last_bot_state", columnDefinition = "SMALLINT DEFAULT NULL")
    private BotState lastBotState;

    @Column(name = "last_selected_date")
    private LocalDate lastSelectedDate;

    protected BotUser() {
    }

    public BotUser(String chatId) {
        this.chatId = chatId;
        this.lastBotState = BotState.UNAUTHORIZED;
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

    public BotState getLastBotState() {
        return lastBotState;
    }

    public void setLastBotState(BotState lastBotState) {
        this.lastBotState = lastBotState;
    }

    public LocalDate getLastSelectedDate() {
        return lastSelectedDate;
    }

    public void setLastSelectedDate(LocalDate lastSelectedDate) {
        this.lastSelectedDate = lastSelectedDate;
    }
}
