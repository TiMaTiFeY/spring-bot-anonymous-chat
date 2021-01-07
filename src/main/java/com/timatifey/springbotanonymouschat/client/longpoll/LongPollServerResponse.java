package com.timatifey.springbotanonymouschat.client.longpoll;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LongPollServerResponse {
    private Response response;

    @Data
    @ToString
    public class Response {
        private String key;
        private String server;
        private String ts;
    }
}
