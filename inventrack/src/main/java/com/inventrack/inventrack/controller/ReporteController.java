package com.inventrack.inventrack.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.inventrack.inventrack.service.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reporte")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;

    @GetMapping("/stock-bajo")
    public List<Map<String, Object>> stockBajo(
            @RequestParam Integer idSucursal) {

        return service.stockBajo(idSucursal);
    }

    @GetMapping("/ventas")
    public List<Map<String, Object>> ventas(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(required = false)
            Integer idSucursal) {

        return service.ventasPorDia(
                desde,
                hasta,
                idSucursal);
    }
}