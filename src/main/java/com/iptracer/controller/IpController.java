package com.iptracer.controller;

import com.google.gson.Gson;
import com.iptracer.model.IpInfo;
import com.iptracer.service.IpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class IpController {

    @Autowired
    private IpService ipService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        List<IpInfo> history = (List<IpInfo>) session.getAttribute("history");
        model.addAttribute("history", history);
        return "index";
    }

    @PostMapping("/trace")
    public String trace(@RequestParam String ip, Model model, HttpSession session) {
        try {
            IpInfo info = ipService.trace(ip);
            model.addAttribute("info", info);

            List<IpInfo> history = (List<IpInfo>) session.getAttribute("history");
            if (history == null) history = new ArrayList<>();

            history.add(info);
            session.setAttribute("history", history);
            session.setAttribute("lastResult", info);

            model.addAttribute("history", history);

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "index";
    }

    @GetMapping("/clear")
    public String clearHistory(HttpSession session) {
        session.removeAttribute("history");
        return "redirect:/";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCurrentIpInfo(HttpSession session) {
        IpInfo info = (IpInfo) session.getAttribute("lastResult");
        if (info == null) {
            return ResponseEntity.badRequest().body("No IP result found.".getBytes());
        }

        String json = new Gson().toJson(info);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("ip-info.json").build());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @GetMapping("/export-history")
    public ResponseEntity<byte[]> exportHistory(HttpSession session) {
        List<IpInfo> history = (List<IpInfo>) session.getAttribute("history");
        if (history == null || history.isEmpty()) {
            return ResponseEntity.badRequest().body("No history found.".getBytes());
        }

        String json = new Gson().toJson(history);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("ip-history.json").build());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @GetMapping("/export-history-csv")
    public ResponseEntity<byte[]> exportHistoryCsv(HttpSession session) {
        List<IpInfo> history = (List<IpInfo>) session.getAttribute("history");
        if (history == null || history.isEmpty()) {
            return ResponseEntity.badRequest().body("No history found.".getBytes());
        }

        StringBuilder csv = new StringBuilder("IP,Country,Region,City,ISP\n");
        for (IpInfo info : history) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    info.getQuery(), info.getCountry(), info.getRegionName(), info.getCity(), info.getIsp()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("ip-history.csv").build());
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(csv.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }


    @PostMapping("/batch")
    public String batchTrace(@RequestParam("file") MultipartFile file, Model model) {
        List<IpInfo> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    IpInfo info = ipService.trace(line.trim());
                    results.add(info);
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            model.addAttribute("error", "Failed to read file: " + e.getMessage());
        }

        model.addAttribute("batchResults", results);
        return "index";
    }


    @GetMapping("/api/trace")
    @ResponseBody
    public ResponseEntity<?> traceApi(@RequestParam String ip) {
        try {
            IpInfo info = ipService.trace(ip);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
