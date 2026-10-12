package com.inventrack.inventrack.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventrack.inventrack.dto.UbicacionRequest;
import com.inventrack.inventrack.entity.Inventario;
import com.inventrack.inventrack.exception.BadRequestException;
import com.inventrack.inventrack.exception.NotFoundException;
import com.inventrack.inventrack.repository.InventarioRepository;

import lombok.RequiredArgsConstructor;

/** Consultas y ubicación del inventario correspondientes a HU-INV-004. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private final InventarioRepository inventarioRepo;

    public List<Inventario> listar(Integer idSucursal) {
        validarId(idSucursal, "idSucursal");
        return inventarioRepo.findBySucursal_Id(idSucursal);
    }

    public Inventario buscar(Integer idSucursal, Integer idProducto) {
        validarId(idSucursal, "idSucursal");
        validarId(idProducto, "idProducto");
        return inventarioRepo.findBySucursal_IdAndProducto_Id(idSucursal, idProducto)
                .orElseThrow(() -> new NotFoundException("No hay inventario para ese producto"));
    }

    @Transactional
    public Inventario actualizarUbicacion(UbicacionRequest request) {
        int actualizados = inventarioRepo.actualizarUbicacion(
                request.getIdInventario(), request.getUbicacion().strip(), LocalDateTime.now());

        if (actualizados == 0) {
            throw new NotFoundException("Inventario no encontrado");
        }

        return inventarioRepo.findById(request.getIdInventario())
                .orElseThrow(() -> new NotFoundException("Inventario no encontrado"));
    }

    private void validarId(Integer id, String campo) {
        if (id == null || id <= 0) {
            throw new BadRequestException(campo + " debe ser mayor que cero");
        }
    }
}
