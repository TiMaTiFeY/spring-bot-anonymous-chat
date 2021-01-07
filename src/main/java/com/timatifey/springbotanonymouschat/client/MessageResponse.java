package com.timatifey.springbotanonymouschat.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MessageResponse {
    @JsonProperty("peer_id")
    private long peerId;
}
