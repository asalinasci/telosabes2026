package com.telosabes.telosabes2026.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** Pagina de inicio. */
    @GetMapping("/")
    public String inicio() {
        return "index";
    }
}
