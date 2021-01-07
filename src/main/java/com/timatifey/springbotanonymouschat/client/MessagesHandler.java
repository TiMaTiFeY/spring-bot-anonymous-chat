package com.timatifey.springbotanonymouschat.client;

import com.timatifey.springbotanonymouschat.client.longpoll.MessageNewObj;
import com.timatifey.springbotanonymouschat.client.parser.MessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessagesHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessagesHandler.class);

    private final BlockingQueue<MessageNewObj> queue = new LinkedBlockingQueue<>();

    private volatile boolean stop;

    private final VKClient client;
    private final ClientConfig config;
    private final RestTemplate template;

    public MessagesHandler(VKClient client, ClientConfig config, RestTemplate template) {
        this.client = client;
        this.config = config;
        this.template = template;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                MessageNewObj msgObj = queue.take();
                Message msg = msgObj.getMessage();
                logger.info(msg.toString());
                MessageParser parser = new MessageParser(msg.getText());
                logger.info(parser.toString());

                final StringBuilder requestURL = new StringBuilder(config.getBotHook());
                requestURL.append(parser.getCommand());
                logger.info("trying to run "+ msg.getText());
                logger.info("trying POST: " + requestURL);
                try {
                    template.postForObject(requestURL.toString(), msgObj, String.class);
                } catch (ResourceAccessException | HttpClientErrorException err) {
                    client.sendMessage("Invalid command, please use /help for get the command list.",
                            msg.getPeerId());
                }
            } catch (InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    public void callMessage(MessageNewObj msg) {
        queue.add(msg);
    }

    public void setStop() {
        stop = true;
    }
}
