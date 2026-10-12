package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.dto.ProveedorRequest;
import com.inventrack.inventrack.entity.Proveedor;
import com.inventrack.inventrack.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository repo;

    public Proveedor registrar(ProveedorRequest r) {
        if (r.rfcNit() != null && repo.existsByRfcNit(r.rfcNit())) {
            throw bad("Ya existe un proveedor con ese RFC/NIT");
        }
        Proveedor p = new Proveedor();
        copiar(p, r);
        p.setActivo(true);
        return repo.save(p);
    }

    public Proveedor actualizar(ProveedorRequest r) {
        if (r.idProveedor() == null) {
            throw bad("idProveedor es obligatorio");
        }
        Proveedor p = buscar(r.idProveedor());
        if (r.rfcNit() != null && !r.rfcNit().equals(p.getRfcNit()) && repo.existsByRfcNit(r.rfcNit())) {
            throw bad("Ya existe un proveedor con ese RFC/NIT");
        }
        copiar(p, r);
        return repo.save(p);
    }

    public List<Proveedor> listar() {
        return repo.findAll();
    }

    public Proveedor buscar(Integer id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    }

    /** Baja lógica: el proveedor queda con Activo = false. */
    public void darDeBaja(Integer id) {
        Proveedor p = buscar(id);
        p.setActivo(false);
        repo.save(p);
    }

    private void copiar(Proveedor p, ProveedorRequest r) {
        p.setNombre(r.nombre());
        p.setRfcNit(r.rfcNit());
        p.setTelefono(r.telefono());
        p.setCorreo(r.correo());
        p.setDireccion(r.direccion());
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
