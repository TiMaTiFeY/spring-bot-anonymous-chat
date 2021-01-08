package com.timatifey.springbotanonymouschat.client;

import com.timatifey.springbotanonymouschat.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatMessagesQueue implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessagesHandler.class);

    private final BlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();

    private volatile boolean stop;

    private final VKClient client;
    private final UsersService usersService;

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
                usersService.getUsersByChatId(msg.getChatId()).stream()
                        .filter(user -> user.getUserId() != (msg.getFromId()))
                        .forEach(user -> {
                            final String text = String.format("%s\uD83D\uDCAC: %s",
                                    msg.getNameOfUser(), msg.getText());
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
