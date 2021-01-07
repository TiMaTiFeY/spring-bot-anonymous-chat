package com.timatifey.springbotanonymouschat.client;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client")
@Data
@ToString
public class ClientConfig {
    private String requestUrlTemplate;
    private String token;
    private long groupId;
    private float versionAPI;
    private String longPollRequestTemplate;
    private String botHook;
}
