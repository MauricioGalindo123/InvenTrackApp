package com.inventrack.inventrack.service;

import com.inventrack.inventrack.dto.CompraRequest;
import com.inventrack.inventrack.entity.*;
import com.inventrack.inventrack.exception.BadRequestException;
import com.inventrack.inventrack.exception.ConflictException;
import com.inventrack.inventrack.exception.NotFoundException;
import com.inventrack.inventrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository,
                              DetalleCompraRepository detalleCompraRepository,
                              ProveedorRepository proveedorRepository,
                              SucursalRepository sucursalRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              InventarioRepository inventarioRepository,
                              MovimientoInventarioRepository movimientoInventarioRepository) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.sucursalRepository = sucursalRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional(readOnly = true)
    public List<OrdenCompra> listar(Integer idSucursal, String estado) {
        if (idSucursal != null && estado != null && !estado.isBlank()) {
            return ordenCompraRepository.findBySucursal_IdAndEstadoIgnoreCase(idSucursal, estado);
        } else if (idSucursal != null) {
            return ordenCompraRepository.findBySucursal_Id(idSucursal);
        } else if (estado != null && !estado.isBlank()) {
            return ordenCompraRepository.findByEstadoIgnoreCase(estado);
        }
        return ordenCompraRepository.findAll();
    }

    @Transactional
    public Map<String, Object> registrar(CompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.idProveedor())
                .orElseThrow(() -> new NotFoundException("Proveedor no encontrado"));

        if (Boolean.FALSE.equals(proveedor.getActivo())) {
            throw new BadRequestException("El proveedor especificado no se encuentra habilitado");
        }

        Sucursal sucursal = sucursalRepository.findById(request.idSucursal())
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));

        Usuario usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleCompra> detalles = new ArrayList<>();

        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setProveedor(proveedor);
        ordenCompra.setSucursal(sucursal);
        ordenCompra.setUsuario(usuario);
        ordenCompra.setFechaCompra(LocalDateTime.now());
        ordenCompra.setEstado("Pendiente");
        ordenCompra.setObservacion(request.observacion());

        for (CompraRequest.Item item : request.detalle()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

            BigDecimal subtotal = item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad()));
            total = total.add(subtotal);

            DetalleCompra det = new DetalleCompra();
            det.setOrdenCompra(ordenCompra);
            det.setProducto(producto);
            det.setCantidad(item.cantidad());
            det.setPrecioUnitario(item.precioUnitario());
            det.setSubtotal(subtotal);

            detalles.add(det);
        }

        ordenCompra.setTotal(total);
        ordenCompra = ordenCompraRepository.save(ordenCompra);

        for (DetalleCompra det : detalles) {
            detalleCompraRepository.save(det);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("idCompra", ordenCompra.getId());
        response.put("total", ordenCompra.getTotal());
        response.put("estado", ordenCompra.getEstado());
        return response;
    }

    @Transactional
    public Map<String, String> recibir(Integer idCompra) {
        if (idCompra == null) {
            throw new BadRequestException("idCompra es obligatorio");
        }

        OrdenCompra ordenCompra = ordenCompraRepository.findById(idCompra)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));

        if ("Recibida".equalsIgnoreCase(ordenCompra.getEstado())) {
            throw new ConflictException("La orden ya ha sido recibida previamente");
        }

        ordenCompra.setEstado("Recibida");
        ordenCompraRepository.save(ordenCompra);

        List<DetalleCompra> detalles = detalleCompraRepository.findByOrdenCompra_Id(idCompra);

        Sucursal sucursal = ordenCompra.getSucursal();
        Usuario usuario = ordenCompra.getUsuario();
        LocalDateTime ahora = LocalDateTime.now();

        for (DetalleCompra det : detalles) {
            Producto producto = det.getProducto();
            Integer cantidad = det.getCantidad();

            // Actualizar o crear fila en inventario
            Inventario inv = inventarioRepository.findBySucursal_IdAndProducto_Id(sucursal.getId(), producto.getId())
                    .orElseGet(() -> {
                        Inventario nuevo = new Inventario();
                        nuevo.setSucursal(sucursal);
                        nuevo.setProducto(producto);
                        nuevo.setCantidadDisponible(0);
                        nuevo.setUbicacion("Por asignar");
                        return nuevo;
                    });

            inv.setCantidadDisponible(inv.getCantidadDisponible() + cantidad);
            inv.setActualizacion(ahora);
            inventarioRepository.save(inv);

            // Registrar movimiento de Entrada en kárdex
            MovimientoInventario mov = new MovimientoInventario();
            mov.setProducto(producto);
            mov.setUsuario(usuario);
            mov.setSucursal(sucursal);
            mov.setDetalleCompra(det);
            mov.setTipoMovimiento("Entrada");
            mov.setCantidad(cantidad);
            mov.setMotivo("Compra");
            mov.setFecha(ahora);

            movimientoInventarioRepository.save(mov);
        }

        return Map.of("mensaje", "Mercadería recibida con éxito");
    }
}