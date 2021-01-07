package com.timatifey.springbotanonymouschat.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class Message {
    private long date;
    @JsonProperty("from_id")
    private long fromId;
    private long id;
    private long out;
    @JsonProperty("peer_id")
    private long peerId;
    private String text;
    @JsonProperty("conversation_message_id")
    private long conversationMessageId;
    private List<Object> attachments;
}
