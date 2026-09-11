package com.bettergametracker.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/library",
            "/stats",
            "/settings",
            "/help",
            "/about",
            "/games/{gameId}",
            "/games/{gameId}/playthroughs/{playId}"
    })
    public String frontend() {
        return "forward:/index.html";
    }
}
