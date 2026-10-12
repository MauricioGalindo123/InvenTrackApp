package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.ProductoProveedorRequest;
import com.inventrack.inventrack.entity.ProductoProveedor;
import com.inventrack.inventrack.service.ProductoProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/producto-proveedor")
@RequiredArgsConstructor
public class ProductoProveedorController {

    private final ProductoProveedorService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoProveedor registrar(@Valid @RequestBody ProductoProveedorRequest r) { return service.registrar(r); }

    @PutMapping("/actualizar")
    public ProductoProveedor actualizar(@Valid @RequestBody ProductoProveedorRequest r) { return service.actualizar(r); }

    @GetMapping("/listar")
    public List<ProductoProveedor> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public ProductoProveedor buscar(@RequestParam Integer idProductoProveedor) {
        return service.buscar(idProductoProveedor);
    }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idProductoProveedor) {
        service.darDeBaja(idProductoProveedor);
        return Map.of("mensaje", "Producto de proveedor dado de baja");
    }
}
