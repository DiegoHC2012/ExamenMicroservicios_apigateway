package com.examen.apigateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Load-balanced WebClient for calling microservices via Eureka (lb://service-name). */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /** Plain WebClient for calling LocalStack (no service discovery needed). */
    @Bean
    public WebClient localstackWebClient() {
        return WebClient.builder()
                .baseUrl("http://localstack:4566")
                .defaultHeader("Authorization",
                        "AWS4-HMAC-SHA256 Credential=test/20200101/us-east-1/logs/aws4_request, " +
                        "SignedHeaders=content-type;host;x-amz-date;x-amz-target, Signature=test")
                .defaultHeader("X-Amz-Date", "20200101T000000Z")
                .build();
    }
}
