package com.loganalyzer.integration.service;

import java.time.Duration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

final class ClientHttpRequestFactories {
    private ClientHttpRequestFactories() {}

    static ClientHttpRequestFactory withTimeout(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
