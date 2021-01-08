package com.timatifey.springbotanonymouschat.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class ChatMessage {
    private long chatId;
    private long fromId;
    private String nameOfUser;
    private String text;
    private String attachments;
}
