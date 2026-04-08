package com.castorama.atg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards the root path to the SPA index.html.
 * All navigation within the app is client-side.
 */
@Controller
public class SpaController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
