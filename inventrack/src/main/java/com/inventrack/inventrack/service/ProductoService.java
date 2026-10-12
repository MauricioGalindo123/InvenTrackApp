package com.inventrack.inventrack.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.Producto;
import com.inventrack.inventrack.repository.CategoriaRepository;
import com.inventrack.inventrack.repository.MarcaRepository;
import com.inventrack.inventrack.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final MarcaRepository marcaRepo;

    public Producto registrar(Producto p) {
        p.setId(null);
        validar(p);
        if (p.getCodigo() != null && repo.existsByCodigo(p.getCodigo())) {
            throw bad("Ya existe un producto con ese código");
        }
        p.setActivo(true);
        if (p.getFechaCreacion() == null) {
            p.setFechaCreacion(LocalDateTime.now());
        }
        return repo.save(p);
    }

    public Producto actualizar(Producto p) {
        Producto actual = buscar(p.getId());
        validar(p);
        if (p.getCodigo() != null && !p.getCodigo().equals(actual.getCodigo()) && repo.existsByCodigo(p.getCodigo())) {
            throw bad("Ya existe un producto con ese código");
        }
        if (p.getActivo() == null) {
            p.setActivo(actual.getActivo());
        }
        if (p.getFechaCreacion() == null) {
            p.setFechaCreacion(actual.getFechaCreacion());
        }
        return repo.save(p);
    }

    public List<Producto> listar() {
        return repo.findAll();
    }

    public Producto buscar(Integer id) {
        if (id == null) {
            throw bad("El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /** Baja lógica. */
    public void eliminar(Integer id) {
        Producto p = buscar(id);
        p.setActivo(false);
        repo.save(p);
    }

    private void validar(Producto p) {
        if (p.getCategoria() == null || p.getCategoria().getId() == null
                || !categoriaRepo.existsById(p.getCategoria().getId())) {
            throw bad("La categoría no existe");
        }
        if (p.getMarca() != null && p.getMarca().getId() != null && !marcaRepo.existsById(p.getMarca().getId())) {
            throw bad("La marca no existe");
        }
        if (p.getPrecioVenta() != null && p.getPrecioVenta().compareTo(BigDecimal.ZERO) < 0) {
            throw bad("precioVenta no puede ser negativo");
        }
        if (p.getPrecioCosto() != null && p.getPrecioCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw bad("precioCosto no puede ser negativo");
        }
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}