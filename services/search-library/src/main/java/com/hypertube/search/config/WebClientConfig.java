package com.hypertube.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient configuration for external API calls
 */
@Configuration
public class WebClientConfig {

    @Value("${external-apis.yts.base-url}")
    private String ytsBaseUrl;

    @Value("${external-apis.eztv.base-url}")
    private String eztvBaseUrl;

    @Value("${external-apis.tmdb.base-url}")
    private String tmdbBaseUrl;

    @Value("${external-apis.yts.timeout:10000}")
    private int ytsTimeout;

    @Value("${external-apis.eztv.timeout:10000}")
    private int eztvTimeout;

    @Value("${external-apis.tmdb.timeout:10000}")
    private int tmdbTimeout;

    /**
     * WebClient for YTS API
     */
    @Bean(name = "ytsWebClient")
    public WebClient ytsWebClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(ytsTimeout));

        return WebClient.builder()
            .baseUrl(ytsBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * WebClient for EZTV API
     */
    @Bean(name = "eztvWebClient")
    public WebClient eztvWebClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(eztvTimeout));

        return WebClient.builder()
            .baseUrl(eztvBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * WebClient for TMDB API
     */
    @Bean(name = "tmdbWebClient")
    public WebClient tmdbWebClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(tmdbTimeout));

        return WebClient.builder()
            .baseUrl(tmdbBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
