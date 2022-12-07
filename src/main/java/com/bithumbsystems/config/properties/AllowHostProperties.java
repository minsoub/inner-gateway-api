package com.bithumbsystems.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "allow-hosts")
@ConfigurationPropertiesScan
public class AllowHostProperties {
    public final List<String> mng;
}
