package com.inventrack.inventrack.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventrack.inventrack.dto.UbicacionRequest;
import com.inventrack.inventrack.entity.Inventario;
import com.inventrack.inventrack.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService service;

    @GetMapping("/listar")
    public List<Inventario> listar(@RequestParam("idSucursal") Integer idSucursal) {
        return service.listar(idSucursal);
    }

    @GetMapping("/buscar")
    public Inventario buscar(
            @RequestParam("idSucursal") Integer idSucursal,
            @RequestParam("idProducto") Integer idProducto) {
        return service.buscar(idSucursal, idProducto);
    }

    @PutMapping("/actualizar-ubicacion")
    public Inventario actualizarUbicacion(@Valid @RequestBody UbicacionRequest request) {
        return service.actualizarUbicacion(request);
    }
}
