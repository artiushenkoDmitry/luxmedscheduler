package com.artsiushenka.luxmedscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luxmed.auth")
public record LuxMedAuthProperties(String baseUrl, String login, String password) {
}
