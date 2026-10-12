package com.inventrack.inventrack.controller;

import com.inventrack.inventrack.dto.CompraRequest;
import com.inventrack.inventrack.entity.OrdenCompra;
import com.inventrack.inventrack.service.OrdenCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orden-compra")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping("/listar")
    public ResponseEntity<List<OrdenCompra>> listar(
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) String estado
    ) {
        return ResponseEntity.ok(ordenCompraService.listar(idSucursal, estado));
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody CompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenCompraService.registrar(request));
    }

    @PutMapping("/recibir")
    public ResponseEntity<Map<String, String>> recibir(@RequestParam Integer idCompra) {
        return ResponseEntity.ok(ordenCompraService.recibir(idCompra));
    }
}