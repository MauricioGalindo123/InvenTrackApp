package com.inventrack.inventrack.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.inventrack.inventrack.entity.Inventario;
import com.inventrack.inventrack.entity.MovimientoInventario;
import com.inventrack.inventrack.entity.Venta;
import com.inventrack.inventrack.repository.InventarioRepository;
import com.inventrack.inventrack.repository.MovimientoInventarioRepository;
import com.inventrack.inventrack.repository.VentaReporteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final VentaReporteRepository ventaRepo;

    public List<Inventario> inventarioPorSucursal(Integer idSucursal) {
        return inventarioRepo.findBySucursal_Id(idSucursal);
    }

    public Inventario inventarioDeProducto(Integer idSucursal, Integer idProducto) {
        return inventarioRepo
                .findBySucursal_IdAndProducto_Id(idSucursal, idProducto)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No hay inventario para ese producto"));
    }

    public Inventario actualizarUbicacion(Integer idInventario, String ubicacion) {
        Inventario inv = inventarioRepo.findById(idInventario)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Inventario no encontrado"));

        inv.setUbicacion(ubicacion);
        inv.setActualizacion(java.time.LocalDateTime.now());

        return inventarioRepo.save(inv);
    }

    public List<MovimientoInventario> movimientos(
            Integer idSucursal,
            LocalDate desde,
            LocalDate hasta) {

        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "desde no puede ser mayor que hasta");
        }

        return movimientoRepo.findBySucursal_IdAndFechaBetweenOrderByFechaAsc(
                idSucursal,
                desde.atStartOfDay(),
                hasta.atTime(23, 59, 59));
    }

    public List<Map<String, Object>> kardex(
            Integer idProducto,
            Integer idSucursal) {

        List<MovimientoInventario> lista =
                movimientoRepo.findByProducto_IdAndSucursal_IdOrderByFechaAsc(
                        idProducto,
                        idSucursal);

        if (lista.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No hay movimientos para ese producto en la sucursal");
        }

        return lista.stream().map(m -> {
            Map<String, Object> fila = new LinkedHashMap<>();

            fila.put("fecha", m.getFecha());
            fila.put("tipo", m.getTipoMovimiento());
            fila.put("cantidad", m.getCantidad());
            fila.put("motivo", m.getMotivo());
            fila.put("usuario", m.getUsuario().getNombre());

            return fila;
        }).toList();
    }

    public List<Map<String, Object>> stockBajo(Integer idSucursal) {

        return inventarioRepo.stockBajo(idSucursal)
                .stream()
                .map(i -> {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("idProducto", i.getProducto().getId());
                    fila.put("producto", i.getProducto().getNombre());
                    fila.put("disponible", i.getCantidadDisponible());
                    fila.put("minimo", i.getProducto().getStockMinimo());

                    return fila;
                }).toList();
    }

    public List<Map<String, Object>> ventasPorDia(
            LocalDate desde,
            LocalDate hasta,
            Integer idSucursal) {

        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "desde no puede ser mayor que hasta");
        }

        List<Venta> ventas =
                ventaRepo.findByEstadoAndFechaVentaBetween(
                        "Registrada",
                        desde.atStartOfDay(),
                        hasta.atTime(23, 59, 59));

        Map<LocalDate, BigDecimal> porDia =
                ventas.stream()
                        .filter(v ->
                                idSucursal == null
                                        || v.getSucursal().getId().equals(idSucursal))
                        .collect(Collectors.groupingBy(
                                v -> v.getFechaVenta().toLocalDate(),
                                TreeMap::new,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Venta::getTotal,
                                        BigDecimal::add)));

        return porDia.entrySet().stream().map(e -> {
            Map<String, Object> fila = new LinkedHashMap<>();

            fila.put("dia", e.getKey());
            fila.put("total", e.getValue());

            return fila;
        }).toList();
    }
}