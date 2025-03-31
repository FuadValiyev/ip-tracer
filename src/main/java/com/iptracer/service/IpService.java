package com.iptracer.service;

import com.iptracer.aspect.Monitored;
import com.iptracer.model.IpInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;

@Slf4j
@Service
@Monitored
@RequiredArgsConstructor
public class IpService {

    @Value("${ip.api-url}")
    private String ipApiUrl;

    private final RestTemplate restTemplate;

//    @Cacheable("ipCache")
//    public IpInfo trace(String rawInput) throws Exception {
//        String ipOrDomain = normalizeInput(rawInput);
//
//        if (!isValidInput(ipOrDomain)) {
//            throw new IllegalArgumentException("Invalid IP address or domain name.");
//        }
//
//        String apiUrl = ipApiUrl + ipOrDomain + "?fields=66846719";
//        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
//        conn.setRequestMethod("GET");
//        conn.setConnectTimeout(5000);
//        conn.setReadTimeout(5000);
//
//        if (conn.getResponseCode() != 200) {
//            throw new RuntimeException("HTTP error: " + conn.getResponseCode());
//        }
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        StringBuilder response = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) response.append(line);
//        reader.close();
//
//        IpInfo info = new Gson().fromJson(response.toString(), IpInfo.class);
//        if (!"success".equalsIgnoreCase(info.getStatus())) {
//            throw new RuntimeException("API Error: " + info.getMessage());
//        }
//
//        return info;
//    }

    @Cacheable("ipCache")
    public IpInfo trace(String rawInput) {
        String ipOrDomain = normalizeInput(rawInput);
        if (!isValidInput(ipOrDomain)) {
            throw new IllegalArgumentException("Invalid IP or domain: " + rawInput);
        }

        try {
            IpInfo info = restTemplate.getForObject(ipApiUrl + ipOrDomain + "?fields=66846719", IpInfo.class);

            if (info == null) {
                log.error("No data returned from IP API for input: {}", ipOrDomain);
                throw new RuntimeException("No data returned from IP API");
            }

            if (!"success".equalsIgnoreCase(info.getStatus())) {
                throw new RuntimeException("API Error: " + info.getMessage());
            }
            return info;
        } catch (RestClientException e) {
            log.error("API request failed for IP: {}", ipOrDomain, e);
            throw new RuntimeException("Failed to fetch IP data: " + e.getMessage());
        }
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

    private boolean isValidInput(String input) {
        String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String domainRegex = "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)([A-Za-z]{2,})$";

        input = input.trim().toLowerCase();
        return input.matches(ipRegex) || input.matches(domainRegex);
    }

}
