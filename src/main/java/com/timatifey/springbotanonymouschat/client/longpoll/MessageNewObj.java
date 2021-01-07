package com.timatifey.springbotanonymouschat.client.longpoll;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.timatifey.springbotanonymouschat.client.Message;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class MessageNewObj {
    private Message message;
    @JsonProperty("client_info")
    private ClientInfo clientInfo;

    @Data
    @ToString
    public class ClientInfo {
        @JsonProperty("button_actions")
        private List<String> buttonActions;
        private boolean keyboard;
        @JsonProperty("inline_keyboard")
        private boolean inlineKeyboard;
        private boolean carousel;
        @JsonProperty("lang_id")
        private long langId;
    }
}