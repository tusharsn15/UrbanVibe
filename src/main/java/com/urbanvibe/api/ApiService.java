package com.urbanvibe.api;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.utility.AppLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiService {

    private static final String ENDPOINT = "https://api.openweathermap.org/data/2.5/weather";
    private static final Pattern TEMP = Pattern.compile("\"temp\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern FEELS = Pattern.compile("\"feels_like\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern HUMIDITY = Pattern.compile("\"humidity\"\\s*:\\s*(\\d+)");
    private static final Pattern DESCRIPTION = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private String getApiKey() {
        return DatabaseConnection.getSetting("URBANVIBE_WEATHER_KEY", "weather.api.key", "").trim();
    }

    public boolean isConfigured() {
        String key = getApiKey();
        return !key.isEmpty() && !key.startsWith("YOUR_");
    }

    public String getWeather(String city) {
        if (!isConfigured()) {
            return "Weather unavailable (no API key configured - see README).";
        }
        try {
            return fetchWeather(city);
        } catch (IOException e) {
            AppLogger.error("Weather API call failed for " + city, e);
            return "Weather unavailable right now (network problem).";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Weather lookup was interrupted.";
        }
    }

    String fetchWeather(String city) throws IOException, InterruptedException {
        String url = ENDPOINT + "?q=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "&units=metric&appid=" + getApiKey();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 401) {
            return "Weather unavailable (the API key was rejected).";
        }
        if (status == 404) {
            return "Weather unavailable (city '" + city + "' not found).";
        }
        if (status != 200) {
            throw new IOException("Unexpected HTTP status " + status);
        }
        return parseWeather(city, response.body());
    }

    public static String parseWeather(String city, String json) {
        String temp = find(TEMP, json);
        String desc = find(DESCRIPTION, json);
        if (temp == null || desc == null) {
            return "Weather unavailable (unexpected response format).";
        }
        StringBuilder sb = new StringBuilder("Weather in " + city + ": " + temp + " C, " + desc);
        String feels = find(FEELS, json);
        if (feels != null) {
            sb.append(" (feels like ").append(feels).append(" C)");
        }
        String humidity = find(HUMIDITY, json);
        if (humidity != null) {
            sb.append(", humidity ").append(humidity).append("%");
        }
        return sb.toString();
    }

    private static String find(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}