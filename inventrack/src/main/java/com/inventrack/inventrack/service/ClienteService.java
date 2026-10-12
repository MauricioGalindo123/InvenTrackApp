package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.entity.Cliente;
import com.inventrack.inventrack.repository.ClienteRepository;
import com.inventrack.inventrack.repository.VentaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repo;
    private final VentaRepository ventaRepo;

    public Cliente registrar(Cliente cliente) {
        cliente.setId(null);
        if (cliente.getFechaRegistro() == null) {
            cliente.setFechaRegistro(java.time.LocalDateTime.now());
        }
        return repo.save(cliente);
    }

    public Cliente actualizar(Cliente cliente) {
        Cliente actual = buscar(cliente.getId());
        return repo.save(cliente);
    }

    public List<Cliente> listar() {
        return repo.findAll();
    }

    public Cliente buscar(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    public void eliminar(Integer id) {
        if (ventaRepo.existsByCliente_Id(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente tiene ventas asociadas");
        }
        repo.delete(buscar(id));
    }
}