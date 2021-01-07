package com.timatifey.springbotanonymouschat;

import com.timatifey.springbotanonymouschat.client.VKClient;
import com.timatifey.springbotanonymouschat.client.longpoll.MessageNewObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private final VKClient client;

    @Autowired
    Controller(VKClient client) {
        this.client = client;
    }

    @PostMapping("/ping")
    public void ping(@RequestBody MessageNewObj msg) {
        client.sendMessage("pong", msg.getMessage().getPeerId());
    }
}
