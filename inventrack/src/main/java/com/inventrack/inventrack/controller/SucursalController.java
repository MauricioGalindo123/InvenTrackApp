package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.Sucursal;
import com.inventrack.inventrack.service.SucursalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sucursal")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Sucursal registrar(@RequestBody Sucursal sucursal) { return service.registrar(sucursal); }

    @PutMapping("/actualizar")
    public Sucursal actualizar(@RequestBody Sucursal sucursal) { return service.actualizar(sucursal); }

    @GetMapping("/listar")
    public List<Sucursal> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Sucursal buscar(@RequestParam Integer idSucursal) { return service.buscar(idSucursal); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idSucursal) {
        service.eliminar(idSucursal);
        return Map.of("mensaje", "Sucursal eliminado");
    }
}