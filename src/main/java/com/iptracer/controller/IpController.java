package com.iptracer.controller;

import com.iptracer.model.IpInfo;
import com.iptracer.service.IpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

}
