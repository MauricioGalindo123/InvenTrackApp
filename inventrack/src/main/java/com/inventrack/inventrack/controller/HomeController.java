package com.inventrack.inventrack.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Conserva la página de pruebas para el modo académico y abre el almacén en la APK. */
@Controller
public class HomeController {
    private final boolean mobileEnabled;
    public HomeController(@Value("${mobile.enabled:true}") boolean mobileEnabled) {
        this.mobileEnabled = mobileEnabled;
    }
    @GetMapping("/")
    public String inicio() {
        return mobileEnabled ? "forward:/index.html" : "forward:/academico.html";
    }
}
