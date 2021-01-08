package com.timatifey.springbotanonymouschat.client;

import com.timatifey.springbotanonymouschat.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ChatMessagesQueue implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessagesHandler.class);

    private final BlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();

    private volatile boolean stop;

    private final VKClient client;
    private final UsersService usersService;

    @Autowired
    public ChatMessagesQueue(VKClient client,UsersService usersService) {
        this.client = client;
        this.usersService = usersService;
    }

    @Override
    public void run() {
        logger.info("starting for msg");
        while (!stop) {
            try {
                ChatMessage msg = queue.take();
                usersService.getUsersInChatByChatId(msg.getChatId()).stream()
                        .filter(user -> (user.getUserId() != (msg.getFromId()) || msg.isSystem()))
                        .forEach(user -> {
                            String text;
                            if (msg.isSystem()) {
                                text = String.format("\uD83D\uDCAC%s\uD83D\uDCAC",
                                        msg.getText());
                            } else {
                                text = String.format("%s\uD83D\uDCAC: %s",
                                        msg.getNameOfUser(), msg.getText());
                            }
                            client.sendMessage(text, user.getUserId());
                        });
            } catch (InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    public void callMessage(ChatMessage msg) {
        queue.add(msg);
    }

    public void setStop() {
        stop = true;
    }

}
