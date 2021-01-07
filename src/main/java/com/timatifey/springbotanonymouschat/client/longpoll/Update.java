package com.timatifey.springbotanonymouschat.client.longpoll;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class Update {
    private String type;
    private MessageNewObj object;
    private Integer group_id;
    private String event_id;
}