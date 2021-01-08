package com.timatifey.springbotanonymouschat.client.parser;

import lombok.Data;
import lombok.ToString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@ToString
public class MessageParser {
    private String body;
    private String command;
    private List<String> args;
    private TypeMessage type;

    public MessageParser(String body) {
        if (body.isEmpty()) {
            this.body = "";
            this.command = "";
            this.type = TypeMessage.EMPTY;
            return;
        }
        this.body = body;
        String[] parts = body.split("\\s+");
        if (parts[0].startsWith("/")) {
            this.command = parts[0];
            if (parts.length > 1) {
                this.args = Arrays.stream(parts)
                        .skip(1)
                        .collect(Collectors.toList());
                this.type = TypeMessage.COMMAND_WITH_ARGS;
            } else {
                this.type = TypeMessage.COMMAND;
            }
        } else {
            this.command = "";
            this.type = TypeMessage.MESSAGE;
        }
    }
}
