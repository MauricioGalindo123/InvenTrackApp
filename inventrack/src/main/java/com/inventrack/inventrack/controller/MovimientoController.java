package com.inventrack.inventrack.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.inventrack.inventrack.entity.MovimientoInventario;
import com.inventrack.inventrack.service.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/movimiento")
@RequiredArgsConstructor
public class MovimientoController {

    private final ReporteService service;

    @GetMapping("/listar")
    public List<MovimientoInventario> listar(
            @RequestParam Integer idSucursal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        return service.movimientos(
                idSucursal,
                desde,
                hasta);
    }

    @GetMapping("/kardex")
    public List<Map<String, Object>> kardex(
            @RequestParam Integer idProducto,
            @RequestParam Integer idSucursal) {

        return service.kardex(
                idProducto,
                idSucursal);
    }
}