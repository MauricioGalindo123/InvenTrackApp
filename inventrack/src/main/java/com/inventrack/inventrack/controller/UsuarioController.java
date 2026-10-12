package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.UsuarioRequest;
import com.inventrack.inventrack.entity.Usuario;
import com.inventrack.inventrack.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario registrar(@Valid @RequestBody UsuarioRequest r) { return service.registrar(r); }

    @PutMapping("/actualizar")
    public Usuario actualizar(@Valid @RequestBody UsuarioRequest r) { return service.actualizar(r); }

    @GetMapping("/listar")
    public List<Usuario> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Usuario buscar(@RequestParam Integer idUsuario) { return service.buscar(idUsuario); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idUsuario) {
        service.darDeBaja(idUsuario);
        return Map.of("mensaje", "Usuario desactivado");
    }
}