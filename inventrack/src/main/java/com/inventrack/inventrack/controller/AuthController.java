package com.inventrack.inventrack.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.LoginRequest;
import com.inventrack.inventrack.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService service;

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest r) { return service.login(r); }
}