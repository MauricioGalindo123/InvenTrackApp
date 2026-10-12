package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.Marca;
import com.inventrack.inventrack.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository repo;

    public Marca registrar(Marca marca) {
        marca.setId(null);
        marca.setActivo(true);
        return repo.save(marca);
    }

    public Marca actualizar(Marca marca) {
        Marca actual = buscar(marca.getId());
        if (marca.getActivo() == null) {
            marca.setActivo(actual.getActivo());
        }
        return repo.save(marca);
    }

    public List<Marca> listar() {
        return repo.findAll();
    }

    public Marca buscar(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrado"));
    }

    public void eliminar(Integer id) {
        Marca e = buscar(id);
        e.setActivo(false);
        repo.save(e);
    }
}