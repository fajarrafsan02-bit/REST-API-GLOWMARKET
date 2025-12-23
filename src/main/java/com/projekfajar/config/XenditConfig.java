package com.projekfajar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Configuration
public class XenditConfig {
    
    @Value("${xendit.api-key}")
    private String apiKey;
    
    @Value("${xendit.api-url}")
    private String apiUrl;
    
    @Bean
    public WebClient xenditWebClient() {
        String encodedApiKey = Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
        
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));
        
        return WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Basic " + encodedApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
