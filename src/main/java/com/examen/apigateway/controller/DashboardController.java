package com.examen.apigateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final WebClient.Builder lbClient;
    private final WebClient localstackClient;
    private final ObjectMapper objectMapper;

    public DashboardController(WebClient.Builder loadBalancedWebClientBuilder,
                               WebClient localstackWebClient,
                               ObjectMapper objectMapper) {
        this.lbClient = loadBalancedWebClientBuilder;
        this.localstackClient = localstackWebClient;
        this.objectMapper = objectMapper;
    }

    // ── Health aggregator ────────────────────────────────────────────────────

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return Mono.zip(
                directHealth("http://eureka-server:8761"),
                serviceHealth("http://productos-service"),
                serviceHealth("http://ordenes-service"),
                serviceHealth("http://pagos-service"),
                localstackHealth()
        ).map(tuple -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("eureka-server",     tuple.getT1());
            result.put("productos-service", tuple.getT2());
            result.put("ordenes-service",   tuple.getT3());
            result.put("pagos-service",     tuple.getT4());
            result.put("localstack",        tuple.getT5());
            return result;
        });
    }

    private Mono<Map<String, Object>> directHealth(String baseUrl) {
        return WebClient.create(baseUrl)
                .get().uri("/actuator/health").retrieve()
                .bodyToMono(Map.class)
                .map(body -> Map.of("status", body.getOrDefault("status", "UNKNOWN")))
                .onErrorResume(e -> Mono.just(Map.of("status", "DOWN", "error", e.getMessage())));
    }

    private Mono<Map<String, Object>> serviceHealth(String baseUrl) {
        return lbClient.build()
                .get().uri(baseUrl + "/actuator/health").retrieve()
                .bodyToMono(Map.class)
                .map(body -> Map.of("status", body.getOrDefault("status", "UNKNOWN")))
                .onErrorResume(e -> Mono.just(Map.of("status", "DOWN", "error", e.getMessage())));
    }

    private Mono<Map<String, Object>> localstackHealth() {
        return WebClient.create("http://localstack:4566")
                .get().uri("/_localstack/health").retrieve()
                .bodyToMono(Map.class)
                .map(body -> Map.of("status", "UP", "services", body.getOrDefault("services", Map.of())))
                .onErrorResume(e -> Mono.just(Map.of("status", "DOWN", "error", e.getMessage())));
    }

    // ── CloudWatch Logs proxy ────────────────────────────────────────────────

    @GetMapping("/logs/groups")
    public Mono<Object> describeLogGroups() {
        return cwCall("Logs_20140328.DescribeLogGroups", "{}");
    }

    @GetMapping("/logs/streams")
    public Mono<Object> describeLogStreams(@RequestParam String group) {
        return cwCall("Logs_20140328.DescribeLogStreams",
                "{\"logGroupName\":\"" + group + "\"}");
    }

    @GetMapping("/logs/events")
    public Mono<Object> getLogEvents(@RequestParam String group, @RequestParam String stream) {
        return cwCall("Logs_20140328.GetLogEvents",
                "{\"logGroupName\":\"" + group + "\",\"logStreamName\":\"" + stream + "\",\"startFromHead\":true}");
    }

    @PostMapping("/logs/streams")
    public Mono<Object> createLogStream(@RequestBody Map<String, String> req) {
        return cwCall("Logs_20140328.CreateLogStream",
                "{\"logGroupName\":\"" + req.get("group") + "\",\"logStreamName\":\"" + req.get("stream") + "\"}");
    }

    @PostMapping("/logs/events")
    public Mono<Object> putLogEvents(@RequestBody Map<String, String> req) {
        long ts = System.currentTimeMillis();
        String body = "{\"logGroupName\":\"" + req.get("group") + "\"," +
                      "\"logStreamName\":\"" + req.get("stream") + "\"," +
                      "\"logEvents\":[{\"timestamp\":" + ts + ",\"message\":\"" + req.get("message") + "\"}]}";
        return cwCall("Logs_20140328.PutLogEvents", body);
    }

    /**
     * All LocalStack CloudWatch calls share the same POST / endpoint,
     * differentiated only by X-Amz-Target.
     * We read the response as String (bypassing content-type negotiation)
     * and parse it with Jackson so the controller can return proper JSON.
     */
    private Mono<Object> cwCall(String target, String requestBody) {
        return localstackClient.post()
                .uri("/")
                .header("X-Amz-Target", target)
                .contentType(MediaType.APPLICATION_JSON)   // send as application/json
                .accept(MediaType.ALL)                     // accept any content-type back
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)                  // read raw String — avoids codec issue
                .map(json -> {
                    try {
                        return (Object) objectMapper.readValue(json, Object.class);
                    } catch (Exception e) {
                        return Map.of("raw", json);
                    }
                })
                .switchIfEmpty(Mono.just(Map.of("result", "ok")))
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
