package com.bithumbsystems.config;

import com.bithumbsystems.config.properties.AllowHostProperties;
import lombok.Getter;

@Getter
public class Config {
    private final String baseMessage;
    private final boolean preLogger;
    private final boolean postLogger;

    private final AllowHostProperties allowHostProperties;

    public Config(String baseMessage, AllowHostProperties allowHostProperties, boolean preLogger, boolean postLogger) {
        this.baseMessage = baseMessage;
        this.allowHostProperties = allowHostProperties;
        this.preLogger = preLogger;
        this.postLogger = postLogger;
    }
}
