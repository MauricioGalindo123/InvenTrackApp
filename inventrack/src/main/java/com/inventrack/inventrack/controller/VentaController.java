package com.inventrack.inventrack.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.VentaRequest;
import com.inventrack.inventrack.entity.Venta;
import com.inventrack.inventrack.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/venta")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody VentaRequest r) {
        Venta v = service.registrar(r);
        return Map.of("idVenta", v.getId(), "total", v.getTotal());
    }

    @GetMapping("/buscar-por-id")
    public Map<String, Object> buscar(@RequestParam Integer idVenta) { return service.buscarConDetalle(idVenta); }

    @GetMapping("/listar")
    public List<Venta> listar(@RequestParam(required = false) Integer idSucursal,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(idSucursal, desde, hasta);
    }

    @PutMapping("/anular")
    public Map<String, String> anular(@RequestParam Integer idVenta) {
        service.anular(idVenta);
        return Map.of("mensaje", "Venta anulada y stock reincorporado");
    }
}