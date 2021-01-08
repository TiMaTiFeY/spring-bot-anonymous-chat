package com.timatifey.springbotanonymouschat.client;

import com.timatifey.springbotanonymouschat.client.longpoll.MessageNewObj;
import com.timatifey.springbotanonymouschat.client.parser.MessageParser;
import com.timatifey.springbotanonymouschat.client.parser.TypeMessage;
import com.timatifey.springbotanonymouschat.repository.User;
import com.timatifey.springbotanonymouschat.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class MessagesHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessagesHandler.class);

    private final BlockingQueue<MessageNewObj> queue = new LinkedBlockingQueue<>();

    private volatile boolean stop;

    private final VKClient client;
    private final ClientConfig config;
    private final RestTemplate template;
    private final UsersService usersService;
    private final ChatMessagesQueue chatMessagesQueue;

    public MessagesHandler(VKClient client, ClientConfig config, RestTemplate template,
                           UsersService usersService, ChatMessagesQueue chatMessagesQueue) {
        this.client = client;
       this.config = config;
        this.template = template;
        this.usersService = usersService;
        this.chatMessagesQueue = chatMessagesQueue;
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
                if (parser.getType() == TypeMessage.MESSAGE) {
                    //template.postForObject(config.getBotHook()+"/send", msgObj, String.class);
                    long peerId = msgObj.getMessage().getPeerId();
                    User user = usersService.findUserById(peerId);
                    if (user == null || !user.isInChat()) {
                        client.sendMessage(
                                "Вы не находитесь ни в каком чате! Чтобы узнать подробнее напиши /help",
                                peerId);
                    } else {
                        chatMessagesQueue.callMessage(new ChatMessage(
                                user.getChat().getId(),
                                user.getUserId(),
                                user.getName(),
                                msgObj.getMessage().getText(),
                                msgObj.getMessage().getAttachments().stream()
                                        .filter(Objects::nonNull)
                                        .map(Object::toString)
                                        .collect(Collectors.joining()),
                                false
                        ));
                    }
                } else {
                    final StringBuilder requestURL = new StringBuilder(config.getBotHook());
                    requestURL.append(parser.getCommand());
                    logger.info("trying to run " + msg.getText());
                    logger.info("trying POST: " + requestURL);
                    try {
                        template.postForObject(requestURL.toString(), msgObj, String.class);
                    } catch (ResourceAccessException | HttpClientErrorException err) {
                        client.sendMessage("Invalid command, please use /help for get the command list.",
                                msg.getPeerId());
                    }
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
