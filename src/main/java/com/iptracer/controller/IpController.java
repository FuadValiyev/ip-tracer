package com.iptracer.controller;

import com.google.gson.Gson;
import com.iptracer.model.IpInfo;
import com.iptracer.service.IpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class IpController {

    @Autowired
    private IpService ipService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        List<IpInfo> history = (List<IpInfo>) session.getAttribute("history");
        if (history != null) {
            List<IpInfo> reversed = new ArrayList<>(history);
            Collections.reverse(reversed);
            model.addAttribute("history", reversed);
        }
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

            List<IpInfo> reversed = new ArrayList<>(history);
            Collections.reverse(reversed);
            model.addAttribute("history", reversed);

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

        StringBuilder csv = new StringBuilder();
        csv.append("IP,Country,Region,City,ZIP,Coordinates,ISP,Organization,AS,Timezone,Status,Proxy,Hosting,Mobile\n");
        for (IpInfo info : history) {
            csv.append(String.format("%s,%s,%s,%s,%s,\"%s,%s\",%s,%s,%s,%s,%s,%s,%s\n",
                    info.getQuery(), info.getCountry(), info.getRegionName(), info.getCity(),
                    info.getZip(), info.getLat(), info.getLon(), info.getIsp(),
                    info.getOrg(), info.getAs(), info.getTimezone(),
                    info.getStatus(), info.isProxy(), info.isHosting(), info.isMobile()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("ip-history.csv").build());
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(csv.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }


    @PostMapping("/batch")
    public String batchTrace(@RequestParam("file") MultipartFile file, Model model, HttpSession session) {
        List<IpInfo> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    IpInfo info = ipService.trace(line.trim());
                    results.add(info);
                } catch (Exception ignored) {
                }
            }
        } catch (IOException e) {
            model.addAttribute("error", "Failed to read file: " + e.getMessage());
        }

        model.addAttribute("batchResults", results);
        session.setAttribute("batchResults", results);

        return "index";
    }


    @ResponseBody
    @GetMapping("/api/trace")
    public ResponseEntity<?> traceApi(@RequestParam String ip) {
        try {
            IpInfo info = ipService.trace(ip);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export-single-json")
    public ResponseEntity<byte[]> exportSingleJson(@RequestParam String ip) {
        try {
            IpInfo info = ipService.trace(ip);
            String json = new Gson().toJson(info);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ip-info.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/export-single-csv")
    public ResponseEntity<byte[]> exportSingleCsv(@RequestParam String ip) {
        try {
            IpInfo info = ipService.trace(ip);

            StringBuilder csv = new StringBuilder();
            csv.append("IP,Country,Region,City,ZIP,Coordinates,ISP,Organization,AS,Timezone,Status,Proxy,Hosting,Mobile\n");
            csv.append(String.format("%s,%s,%s,%s,%s,\"%s,%s\",%s,%s,%s,%s,%s,%s,%s",
                    info.getQuery(), info.getCountry(), info.getRegionName(), info.getCity(),
                    info.getZip(), info.getLat(), info.getLon(), info.getIsp(),
                    info.getOrg(), info.getAs(), info.getTimezone(),
                    info.isProxy(), info.isHosting(), info.isMobile()
            ));

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ip-info.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/export-batch-json")
    public ResponseEntity<byte[]> exportBatchJson(HttpSession session) {
        List<IpInfo> batchResults = (List<IpInfo>) session.getAttribute("batchResults");
        if (batchResults == null || batchResults.isEmpty()) {
            return ResponseEntity.badRequest().body("No batch results found.".getBytes());
        }

        String json = new Gson().toJson(batchResults);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("batch-results.json").build());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @GetMapping("/export-batch-csv")
    public ResponseEntity<byte[]> exportBatchCsv(HttpSession session) {
        List<IpInfo> batchResults = (List<IpInfo>) session.getAttribute("batchResults");
        if (batchResults == null || batchResults.isEmpty()) {
            return ResponseEntity.badRequest().body("No batch results found.".getBytes());
        }

        StringBuilder csv = new StringBuilder();
        csv.append("IP,Country,Region,City,ZIP,Coordinates,ISP,Organization,AS,Timezone,Status,Proxy,Hosting,Mobile\n");
        for (IpInfo info : batchResults) {
            csv.append(String.format("%s,%s,%s,%s,%s,\"%s,%s\",%s,%s,%s,%s,%s,%s,%s\n",
                    info.getQuery(), info.getCountry(), info.getRegionName(), info.getCity(),
                    info.getZip(), info.getLat(), info.getLon(), info.getIsp(),
                    info.getOrg(), info.getAs(), info.getTimezone(),
                    info.isProxy(), info.isHosting(), info.isMobile()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("batch-results.csv").build());
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(csv.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }


}
