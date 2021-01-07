package com.timatifey.springbotanonymouschat.client;

import com.timatifey.springbotanonymouschat.client.longpoll.LongPoll;
import com.timatifey.springbotanonymouschat.client.longpoll.LongPollRequestResponse;
import com.timatifey.springbotanonymouschat.client.longpoll.LongPollServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class VKClient {
    private static final Logger logger = LoggerFactory.getLogger(VKClient.class);

    private final RestTemplate template;
    private final ClientConfig config;

    @Autowired
    VKClient(RestTemplate template, ClientConfig config) {
        this.template = template;
        this.config = config;
    }

    @PostConstruct
    void init() {
        MessagesHandler messagesHandler = new MessagesHandler(this, config, template);
        LongPollServerResponse.Response longPollServer = getLongPollServer().getResponse();
        logger.info("server info: " + longPollServer.toString());
        LongPoll longPoll = new LongPoll(
                this,
                messagesHandler,
                longPollServer.getKey(),
                longPollServer.getServer(),
                longPollServer.getTs()
        );
        new Thread(longPoll).start();
    }

    private String getUrlRequest(String methodName, Map<String, Object> props) {
        StringBuilder properties = new StringBuilder();
        for (Map.Entry<String, Object> property : props.entrySet()) {
            properties.append("&");
            properties.append(property.getKey());
            properties.append("=");
            properties.append(property.getValue());
        }
        if (properties.length() > 0) {
            properties.deleteCharAt(0);
        }
        final StringBuilder requestURL = new StringBuilder(String.format(config.getRequestUrlTemplate(), methodName));
        if (!props.isEmpty()) {
            requestURL.append("?");
            requestURL.append(properties.toString());
        }
        return requestURL.toString();
    }

    public LongPollServerResponse getLongPollServer() {
        final String request = getUrlRequest(
                "groups.getLongPollServer",
                Map.of(
                        "group_id", config.getGroupId(),
                        "access_token", config.getToken(),
                        "v", config.getVersionAPI()
                )
        );
        logger.info("trying get request: " + request);
        final LongPollServerResponse response = template.getForObject(request, LongPollServerResponse.class);
        return response;
    }

    public LongPollRequestResponse getLongPollResponse(String server, String key, String ts) {
        final String request = String.format(
                config.getLongPollRequestTemplate(),
                server,
                key,
                ts
        );
        logger.info("trying get request: " + request);
        final LongPollRequestResponse response = template.getForObject(request, LongPollRequestResponse.class);
        return response;
    }

    public MessageResponse sendMessage(String message, long peerId) {
        final String request = getUrlRequest(
                "messages.send",
                Map.of(
                        "message", message,
                        "peer_id", peerId,
                        "random_id", System.currentTimeMillis(),
                        "access_token", config.getToken(),
                        "v", config.getVersionAPI()
                )
        );
        logger.info("trying get request: " + request);
        final MessageResponse response = template.getForObject(request, MessageResponse.class);
        return response;
    }
}
