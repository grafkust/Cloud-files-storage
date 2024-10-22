package com.project.cloud.files.storage.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@Data
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    private String host;
    private Integer port;
    private String username;
    private String password;

    private Properties properties;
}

