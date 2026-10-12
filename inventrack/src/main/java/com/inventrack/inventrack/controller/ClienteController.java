package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.Cliente;
import com.inventrack.inventrack.service.ClienteService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente registrar(@RequestBody Cliente cliente) { return service.registrar(cliente); }

    @PutMapping("/actualizar")
    public Cliente actualizar(@RequestBody Cliente cliente) { return service.actualizar(cliente); }

    @GetMapping("/listar")
    public List<Cliente> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Cliente buscar(@RequestParam Integer idCliente) { return service.buscar(idCliente); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idCliente) {
        service.eliminar(idCliente);
        return Map.of("mensaje", "Cliente eliminado");
    }
}