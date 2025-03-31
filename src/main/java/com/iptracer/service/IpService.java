package com.iptracer.service;

import com.google.gson.Gson;
import com.iptracer.model.IpInfo;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class IpService {

    public IpInfo trace(String rawInput) throws Exception {
        String ipOrDomain = normalizeInput(rawInput);

        String apiUrl = "http://ip-api.com/json/" + ipOrDomain + "?fields=66846719";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("HTTP error: " + conn.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        IpInfo info = new Gson().fromJson(response.toString(), IpInfo.class);
        if (!"success".equalsIgnoreCase(info.getStatus())) {
            throw new RuntimeException("API Error: " + info.getMessage());
        }

        return info;
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) return input;

        try {
            if (input.startsWith("http://") || input.startsWith("https://")) {
                URL url = new URL(input);
                return url.getHost().replaceFirst("^www\\.", "");
            }

            if (input.startsWith("www.")) {
                URL url = new URL("http://" + input);
                return url.getHost().replaceFirst("^www\\.", "");
            }
            return input.trim();

        } catch (Exception e) {
            return input.trim().replaceFirst("^www\\.", "").replaceAll("/$", "");
        }
    }

}
