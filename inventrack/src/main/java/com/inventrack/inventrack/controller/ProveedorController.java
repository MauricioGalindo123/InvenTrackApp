package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.ProveedorRequest;
import com.inventrack.inventrack.entity.Proveedor;
import com.inventrack.inventrack.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/proveedor")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Proveedor registrar(@Valid @RequestBody ProveedorRequest r) { return service.registrar(r); }

    @PutMapping("/actualizar")
    public Proveedor actualizar(@Valid @RequestBody ProveedorRequest r) { return service.actualizar(r); }

    @GetMapping("/listar")
    public List<Proveedor> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Proveedor buscar(@RequestParam Integer idProveedor) { return service.buscar(idProveedor); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idProveedor) {
        service.darDeBaja(idProveedor);
        return Map.of("mensaje", "Proveedor dado de baja");
    }
}
