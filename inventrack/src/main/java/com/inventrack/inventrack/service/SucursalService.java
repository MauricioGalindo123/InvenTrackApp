package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.Sucursal;
import com.inventrack.inventrack.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository repo;

    public Sucursal registrar(Sucursal sucursal) {
        sucursal.setId(null);
        sucursal.setActivo(true);
        return repo.save(sucursal);
    }

    public Sucursal actualizar(Sucursal sucursal) {
        Sucursal actual = buscar(sucursal.getId());
        if (sucursal.getActivo() == null) {
            sucursal.setActivo(actual.getActivo());
        }
        return repo.save(sucursal);
    }

    public List<Sucursal> listar() {
        return repo.findAll();
    }

    public Sucursal buscar(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrado"));
    }

    public void eliminar(Integer id) {
        Sucursal e = buscar(id);
        e.setActivo(false);
        repo.save(e);
    }
}