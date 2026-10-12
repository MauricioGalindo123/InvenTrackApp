package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.Categoria;
import com.inventrack.inventrack.repository.CategoriaRepository;
import com.inventrack.inventrack.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;
    private final ProductoRepository productoRepo;

    public Categoria registrar(Categoria categoria) {
        categoria.setId(null);
        return repo.save(categoria);
    }

    public Categoria actualizar(Categoria categoria) {
        Categoria actual = buscar(categoria.getId());
        return repo.save(categoria);
    }

    public List<Categoria> listar() {
        return repo.findAll();
    }

    public Categoria buscar(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria no encontrado"));
    }

    public void eliminar(Integer id) {
        if (productoRepo.existsByCategoria_Id(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría tiene productos asociados");
        }
        repo.delete(buscar(id));
    }
}