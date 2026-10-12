package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.Producto;
import com.inventrack.inventrack.service.ProductoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/producto")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Producto registrar(@RequestBody Producto producto) { return service.registrar(producto); }

    @PutMapping("/actualizar")
    public Producto actualizar(@RequestBody Producto producto) { return service.actualizar(producto); }

    @GetMapping("/listar")
    public List<Producto> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Producto buscar(@RequestParam Integer idProducto) { return service.buscar(idProducto); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idProducto) {
        service.eliminar(idProducto);
        return Map.of("mensaje", "Producto eliminado");
    }
}