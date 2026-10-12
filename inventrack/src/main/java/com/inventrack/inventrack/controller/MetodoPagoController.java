package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.MetodoPago;
import com.inventrack.inventrack.service.MetodoPagoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/metodo-pago")
@RequiredArgsConstructor
public class MetodoPagoController {

    private final MetodoPagoService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public MetodoPago registrar(@RequestBody MetodoPago metodoPago) { return service.registrar(metodoPago); }

    @PutMapping("/actualizar")
    public MetodoPago actualizar(@RequestBody MetodoPago metodoPago) { return service.actualizar(metodoPago); }

    @GetMapping("/listar")
    public List<MetodoPago> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public MetodoPago buscar(@RequestParam Integer idMetodoPago) { return service.buscar(idMetodoPago); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idMetodoPago) {
        service.eliminar(idMetodoPago);
        return Map.of("mensaje", "MetodoPago eliminado");
    }
}