package com.inventrack.inventrack.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inventrack.inventrack.dto.TrasladoRequest;
import com.inventrack.inventrack.entity.DetalleTraslado;
import com.inventrack.inventrack.entity.Inventario;
import com.inventrack.inventrack.entity.MovimientoInventario;
import com.inventrack.inventrack.entity.Sucursal;
import com.inventrack.inventrack.entity.Traslado;
import com.inventrack.inventrack.exception.BadRequestException;
import com.inventrack.inventrack.exception.ConflictException;
import com.inventrack.inventrack.exception.NotFoundException;
import com.inventrack.inventrack.repository.DetalleTrasladoRepository;
import com.inventrack.inventrack.repository.InventarioRepository;
import com.inventrack.inventrack.repository.MovimientoInventarioRepository;
import com.inventrack.inventrack.repository.ProductoRepository;
import com.inventrack.inventrack.repository.SucursalRepository;
import com.inventrack.inventrack.repository.TrasladoRepository;
import com.inventrack.inventrack.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrasladoService {

    private static final String PENDIENTE = "Pendiente";
    private static final String ENVIADO = "Enviado";
    private static final String RECIBIDO = "Recibido";

    private final TrasladoRepository trasladoRepo;
    private final DetalleTrasladoRepository detalleRepo;
    private final SucursalRepository sucursalRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;

    /** Registra el traslado en estado Pendiente. El stock no cambia todavía. */
    @Transactional
    public Traslado registrar(TrasladoRequest r) {
        if (r.idSucursalOrigen().equals(r.idSucursalDestino())) {
            throw new BadRequestException("La sucursal de origen y destino deben ser distintas");
        }
        Traslado t = new Traslado();
        t.setSucursalOrigen(sucursal(r.idSucursalOrigen()));
        t.setSucursalDestino(sucursal(r.idSucursalDestino()));
        t.setUsuario(usuarioRepo.findById(r.idUsuario()).orElseThrow(() -> new NotFoundException("Usuario no existe")));
        t.setFechaSolicitud(LocalDateTime.now());
        t.setEstado(PENDIENTE);
        t.setObservacion(r.observacion());
        trasladoRepo.save(t);

        for (TrasladoRequest.Item it : r.detalle()) {
            DetalleTraslado d = new DetalleTraslado();
            d.setTraslado(t);
            d.setProducto(productoRepo.findById(it.idProducto())
                    .orElseThrow(() -> new NotFoundException("Producto no existe")));
            d.setCantidad(it.cantidad());
            detalleRepo.save(d);
        }
        return t;
    }

    /** Envía el traslado: descuenta el stock del origen y registra la Salida. */
    @Transactional
    public void enviar(Integer idTraslado) {
        Traslado t = buscar(idTraslado);
        if (!PENDIENTE.equals(t.getEstado())) {
            throw new ConflictException("El traslado ya fue enviado");
        }
        List<DetalleTraslado> detalle = detalleRepo.findByTraslado_Id(t.getId());
        for (DetalleTraslado d : detalle) {
            Inventario inv = inventarioRepo
                    .findBySucursal_IdAndProducto_Id(t.getSucursalOrigen().getId(), d.getProducto().getId())
                    .orElse(null);
            if (inv == null || inv.getCantidadDisponible() < d.getCantidad()) {
                throw new BadRequestException("Stock insuficiente en la sucursal de origen");
            }
        }
        for (DetalleTraslado d : detalle) {
            Inventario inv = inventarioRepo
                    .findBySucursal_IdAndProducto_Id(t.getSucursalOrigen().getId(), d.getProducto().getId())
                    .orElseThrow();
            inv.setCantidadDisponible(inv.getCantidadDisponible() - d.getCantidad());
            inv.setActualizacion(LocalDateTime.now());
            inventarioRepo.save(inv);
            registrarMovimiento(t, d, t.getSucursalOrigen(), "Salida");
        }
        t.setEstado(ENVIADO);
        t.setFechaEnvio(LocalDateTime.now());
        trasladoRepo.save(t);
    }

    /** Recibe el traslado: suma el stock en el destino y registra la Entrada. */
    @Transactional
    public void recibir(Integer idTraslado) {
        Traslado t = buscar(idTraslado);
        if (PENDIENTE.equals(t.getEstado())) {
            throw new ConflictException("El traslado aún no ha sido enviado");
        }
        if (RECIBIDO.equals(t.getEstado())) {
            throw new ConflictException("El traslado ya fue recibido");
        }
        for (DetalleTraslado d : detalleRepo.findByTraslado_Id(t.getId())) {
            Inventario inv = inventarioRepo
                    .findBySucursal_IdAndProducto_Id(t.getSucursalDestino().getId(), d.getProducto().getId())
                    .orElseGet(() -> {
                        Inventario nuevo = new Inventario();
                        nuevo.setSucursal(t.getSucursalDestino());
                        nuevo.setProducto(d.getProducto());
                        nuevo.setCantidadDisponible(0);
                        nuevo.setUbicacion("Por asignar");
                        return nuevo;
                    });
            inv.setCantidadDisponible(inv.getCantidadDisponible() + d.getCantidad());
            inv.setActualizacion(LocalDateTime.now());
            inventarioRepo.save(inv);
            registrarMovimiento(t, d, t.getSucursalDestino(), "Entrada");
        }
        t.setEstado(RECIBIDO);
        t.setFechaRecepcion(LocalDateTime.now());
        trasladoRepo.save(t);
    }

    /** Lista los traslados; idSucursal filtra por origen o destino y estado es opcional. */
    public List<Traslado> listar(Integer idSucursal, String estado) {
        List<Traslado> lista = idSucursal == null
                ? trasladoRepo.findAll()
                : trasladoRepo.findBySucursalOrigen_IdOrSucursalDestino_Id(idSucursal, idSucursal);
        return lista.stream()
                .filter(t -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(t.getEstado()))
                .toList();
    }

    private void registrarMovimiento(Traslado t, DetalleTraslado d, Sucursal sucursal, String tipo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(d.getProducto());
        m.setUsuario(t.getUsuario());
        m.setSucursal(sucursal);
        m.setDetalleTraslado(d);
        m.setTipoMovimiento(tipo);
        m.setCantidad(d.getCantidad());
        m.setMotivo("Traslado");
        m.setFecha(LocalDateTime.now());
        movimientoRepo.save(m);
    }

    private Traslado buscar(Integer id) {
        return trasladoRepo.findById(id).orElseThrow(() -> new NotFoundException("Traslado no encontrado"));
    }

    private Sucursal sucursal(Integer id) {
        return sucursalRepo.findById(id).orElseThrow(() -> new NotFoundException("Sucursal no existe"));
    }
}
