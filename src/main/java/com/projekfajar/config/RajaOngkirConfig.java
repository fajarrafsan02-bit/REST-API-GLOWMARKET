package com.projekfajar.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

/**
 * Timeout sengaja lebih pendek dari XenditConfig (10 detik, bukan 30) — ini
 * dipanggil sinkron di tengah alur checkout/estimasi Keranjang, jadi kalau
 * RajaOngkir lambat, sistem harus cepat menyerah dan jatuh ke tarif tetap
 * (lihat OngkirCalculationService), bukan membuat pembeli menunggu lama.
 */
@Configuration
public class RajaOngkirConfig {

    @Value("${rajaongkir.api-url}")
    private String apiUrl;

    @Value("${rajaongkir.api-key:}")
    private String apiKey;

    @Bean
    public WebClient rajaOngkirWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("key", apiKey)
                .build();
    }
}
