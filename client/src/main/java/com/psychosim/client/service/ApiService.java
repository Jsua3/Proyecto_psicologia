package com.psychosim.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.client.model.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8090";
    private static ApiService instance;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public static ApiService getInstance() {
        if (instance == null) instance = new ApiService();
        return instance;
    }

    public JsonNode get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }

    public JsonNode post(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (UserSession.getInstance().isLoggedIn()) {
            builder.header("Authorization", "Bearer " + UserSession.getInstance().getToken());
        }

        HttpResponse<String> res = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }

    public JsonNode put(String path, Object body) throws Exception {
        String json = body != null ? mapper.writeValueAsString(body) : "{}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }

    public ObjectMapper getMapper() { return mapper; }
}
