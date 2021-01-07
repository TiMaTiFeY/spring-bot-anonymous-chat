package com.timatifey.springbotanonymouschat.client.longpoll;

import com.timatifey.springbotanonymouschat.client.MessagesHandler;
import com.timatifey.springbotanonymouschat.client.VKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
public class LongPoll implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LongPoll.class);

    private final String key;
    private final String server;
    private String ts;

    private volatile boolean stop;

    private final VKClient client;
    private final MessagesHandler messagesHandler;


    public LongPoll(VKClient client, MessagesHandler messagesHandler, String key, String server, String ts) {
        this.client = client;
        this.messagesHandler = messagesHandler;
        this.key = key;
        this.server = server;
        this.ts = ts;

        new Thread(this.messagesHandler).start();
    }

    @Override
    public void run() {
        while (!stop) {
            LongPollRequestResponse response = client.getLongPollResponse(server, key, ts);
            logger.info(response.toString());
            this.ts = response.getTs();
            response.getUpdates().stream()
                    .map(Update::getObject)
                    .collect(Collectors.toSet())
                    .forEach(messagesHandler::callMessage);
        }
    }

    public void setStop() {
        stop = true;
    }
}
