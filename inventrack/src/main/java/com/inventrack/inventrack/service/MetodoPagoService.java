package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.MetodoPago;
import com.inventrack.inventrack.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetodoPagoService {

    private final MetodoPagoRepository repo;

    public MetodoPago registrar(MetodoPago metodoPago) {
        metodoPago.setId(null);
        metodoPago.setActivo(true);
        return repo.save(metodoPago);
    }

    public MetodoPago actualizar(MetodoPago metodoPago) {
        MetodoPago actual = buscar(metodoPago.getId());
        if (metodoPago.getActivo() == null) {
            metodoPago.setActivo(actual.getActivo());
        }
        return repo.save(metodoPago);
    }

    public List<MetodoPago> listar() {
        return repo.findAll();
    }

    public MetodoPago buscar(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "MetodoPago no encontrado"));
    }

    public void eliminar(Integer id) {
        MetodoPago e = buscar(id);
        e.setActivo(false);
        repo.save(e);
    }
}