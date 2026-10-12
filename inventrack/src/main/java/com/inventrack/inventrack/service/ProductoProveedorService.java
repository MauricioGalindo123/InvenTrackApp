package com.inventrack.inventrack.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.dto.ProductoProveedorRequest;
import com.inventrack.inventrack.entity.ProductoProveedor;
import com.inventrack.inventrack.repository.ProductoProveedorRepository;
import com.inventrack.inventrack.repository.ProductoRepository;
import com.inventrack.inventrack.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoProveedorService {

    private final ProductoProveedorRepository repo;
    private final ProductoRepository productoRepo;
    private final ProveedorRepository proveedorRepo;

    public ProductoProveedor registrar(ProductoProveedorRequest r) {
        if (repo.existsByProducto_IdAndProveedor_Id(r.idProducto(), r.idProveedor())) {
            throw bad("Ese producto ya está registrado para el proveedor");
        }
        ProductoProveedor pp = new ProductoProveedor();
        copiar(pp, r);
        pp.setActivo(true);
        return repo.save(pp);
    }

    public ProductoProveedor actualizar(ProductoProveedorRequest r) {
        if (r.idProductoProveedor() == null) {
            throw bad("idProductoProveedor es obligatorio");
        }
        ProductoProveedor pp = buscar(r.idProductoProveedor());
        copiar(pp, r);
        return repo.save(pp);
    }

    public List<ProductoProveedor> listar() {
        return repo.findAll();
    }

    public ProductoProveedor buscar(Integer id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto de proveedor no encontrado"));
    }

    /** Baja lógica: queda con Activo = false. */
    public void darDeBaja(Integer id) {
        ProductoProveedor pp = buscar(id);
        pp.setActivo(false);
        repo.save(pp);
    }

    private void copiar(ProductoProveedor pp, ProductoProveedorRequest r) {
        pp.setProducto(productoRepo.findById(r.idProducto()).orElseThrow(() -> bad("El producto no existe")));
        pp.setProveedor(proveedorRepo.findById(r.idProveedor()).orElseThrow(() -> bad("El proveedor no existe")));
        pp.setCodigoProveedor(r.codigoProveedor());
        pp.setPrecioCompra(r.precioCompra());
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
