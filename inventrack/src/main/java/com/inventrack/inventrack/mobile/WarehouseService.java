package com.inventrack.inventrack.mobile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarehouseService {
 final MobileDatabase data;
 final MobileSession sessions;
 public WarehouseService(MobileDatabase data,MobileSession sessions){this.data=data;this.sessions=sessions;}
 static final String PRODUCT_SELECT="""
  select p.idproducto as id,p.codigo,p.nombre,p.precioventa as precio,p.stockminimo as minimo,
  p.imagenurl as imagen,c.nombre as categoria,coalesce(m.nombre,'') as marca,
  i.cantidaddisponible as cantidad,i.conteoreal as contado,coalesce(i.ubicacion,'') as ubicacion,
  i.idsucursal as almacenid,coalesce(s.nombremovil,s.nombre) as almacen
  from inventario i join producto p on p.idproducto=i.idproducto
  join sucursales s on s.idsucursal=i.idsucursal
  join categorias c on c.idcategorias=p.idcategorias left join marcas m on m.idmarca=p.idmarca
  """;
 Map<String,Object> summary(int site){
  var r=data.db.queryForMap("""
   select count(*) as productos,coalesce(sum(i.cantidaddisponible),0) as unidades,
   coalesce(sum(case when not i.conteoreal then 1 else 0 end),0) as pendientes,
   coalesce(sum(case when i.conteoreal and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo then 1 else 0 end),0) as bajos
   from inventario i join producto p on p.idproducto=i.idproducto where i.idsucursal=? and p.activo=true
   """,site);
  r.put("recientes",history(site,6));r.put("almacen",Map.of("id",site,"nombre",data.warehouseName(site)));
  r.put("bajosTotal",data.db.queryForObject("select count(*) from inventario i join producto p on p.idproducto=i.idproducto join sucursales s on s.idsucursal=i.idsucursal where s.codigomovil in ('general','beauty','mm') and s.activo=true and p.activo=true and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo",Integer.class));return r;
 }
 Map<String,Object> products(int site,String search,String status,int page,int size){
  if(page<0||size<1||size>100||search.length()>100)throw bad("La búsqueda no es válida");
  String where=" where i.idsucursal=? and p.activo=true and (lower(p.nombre) like ? or lower(p.codigo) like ?)";
  if(status.equals("bajos"))where+=" and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo";
  else if(status.equals("pendientes"))where+=" and i.conteoreal=false";
  else if(!status.equals("todos"))throw bad("El filtro no es válido");
  String q="%"+search.strip().toLowerCase(Locale.ROOT)+"%";
  var rows=data.db.queryForList(PRODUCT_SELECT+where+" order by p.nombre,p.idproducto limit ? offset ?",site,q,q,size,(long)page*size);
  int count=data.db.queryForObject("select count(*) from inventario i join producto p on p.idproducto=i.idproducto"+where,Integer.class,site,q,q);
  return Map.of("items",rows,"total",count,"pagina",page,"tamano",size);
 }
 List<Map<String,Object>> lowStock(int site){return data.db.queryForList(PRODUCT_SELECT+" where i.idsucursal=? and p.activo=true and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo order by i.cantidaddisponible,p.nombre",site);}
 List<Map<String,Object>> allLowStock(){return data.db.queryForList(PRODUCT_SELECT+" where s.codigomovil in ('general','beauty','mm') and s.activo=true and p.activo=true and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo order by s.nombre,i.cantidaddisponible,p.nombre");}
 List<Map<String,Object>> history(int site,int limit){var rows=data.db.queryForList("""
  select o.id,o.tipo,o.destino,o.observacion,o.fecha,u.nombre as usuario,o.resultado
  from mobile_operacion o left join usuario u on u.idusuario=o.idusuario
  where o.idsucursal=? and o.tipo in ('Entrada','Salida','Conteo','Importacion','Producto')
  order by o.fecha desc limit ?
  """,site,Math.max(1,Math.min(limit,100)));for(var row:rows)if(row.get("fecha") instanceof java.sql.Timestamp t)row.put("fecha",t.toInstant().toString());return rows;}
 record Line(Integer idProducto,Integer cantidad){}
 record Movement(String idOperacion,String tipo,String destino,String observacion,List<Line> productos){}
 @Transactional Map<String,Object> movement(MobileSession.User actor,Movement req,int site){
  if(req.idOperacion()==null||!req.idOperacion().matches("[a-zA-Z0-9_-]{16,100}")||!Set.of("Entrada","Salida","Conteo").contains(Objects.toString(req.tipo(),"")))throw bad("El movimiento no es válido");
  if(req.productos()==null||req.productos().isEmpty()||req.productos().size()>200)throw bad("Selecciona de 1 a 200 productos");
  String destination=clean(req.destino(),120),note=clean(req.observacion(),500);
  String fingerprint=MobileSession.hash(data.json.writeValueAsString(req));
  var saved=data.db.queryForList("select idusuario,idsucursal,huella,resultado from mobile_operacion where id=?",req.idOperacion());
  if(!saved.isEmpty())return replay(saved.getFirst(),actor,fingerprint,site);
  // Operation IDs and the catalog are shared: coordinate with movements in every site and imports.
  data.db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  saved=data.db.queryForList("select idusuario,idsucursal,huella,resultado from mobile_operacion where id=?",req.idOperacion());
  if(!saved.isEmpty())return replay(saved.getFirst(),actor,fingerprint,site);
  Set<Integer> unique=new HashSet<>();List<Map<String,Object>> result=new ArrayList<>();
  var lines=req.productos().stream().sorted(Comparator.comparing(l->Objects.requireNonNullElse(l.idProducto(),0))).toList();
  for(var line:lines){
   if(line.idProducto()==null||line.cantidad()==null||line.cantidad()<0||line.cantidad()>1000000||!unique.add(line.idProducto())||(!req.tipo().equals("Conteo")&&line.cantidad()==0))throw bad("Revisa las cantidades y los productos repetidos");
   var found=data.db.queryForList("select i.idinventario,i.cantidaddisponible,i.conteoreal,p.nombre from inventario i join producto p on p.idproducto=i.idproducto where i.idsucursal=? and i.idproducto=? and p.activo=true for update",site,line.idProducto());
   if(found.isEmpty())throw bad("Un producto no pertenece a este almacén");
   var row=found.getFirst();int before=((Number)row.get("cantidaddisponible")).intValue();boolean counted=Boolean.TRUE.equals(row.get("conteoreal"));
   if(req.tipo().equals("Salida")&&!counted)throw conflict("Registra primero el conteo de "+row.get("nombre"));
   if(req.tipo().equals("Conteo")&&counted)sessions.admin(actor);
   int after=switch(req.tipo()){case "Entrada"->Math.addExact(before,line.cantidad());case "Salida"->before-line.cantidad();default->line.cantidad();};
   if(after<0)throw conflict("Stock insuficiente para "+row.get("nombre")+". Disponible: "+before);
   data.db.update("update inventario set cantidaddisponible=?,conteoreal=true,actualizacion=? where idinventario=?",after,LocalDateTime.now(),row.get("idinventario"));
   int delta=after-before;
   if(delta!=0){String reason=req.tipo().equals("Conteo")?"Conteo físico":req.tipo();if(!destination.isBlank())reason+=" → "+destination;reason=reason.substring(0,Math.min(100,reason.length()));
    data.db.update("insert into movimiento_inventario(idproducto,idusuario,idsucursal,tipomovimiento,cantidad,motivo,fecha,idoperacion) values(?,?,?,?,?,?,?,?)",line.idProducto(),actor.id(),site,delta>0?"Entrada":"Salida",Math.abs(delta),reason,LocalDateTime.now(),req.idOperacion());}
   result.add(Map.of("idProducto",line.idProducto(),"producto",row.get("nombre"),"antes",before,"despues",after,"cantidad",line.cantidad()));
  }
  var response=Map.<String,Object>of("idOperacion",req.idOperacion(),"tipo",req.tipo(),"productos",result,"destino",destination,"almacen",Map.of("id",site,"nombre",data.warehouseName(site)),"mensaje","Movimiento registrado");
  data.db.update("insert into mobile_operacion(id,idusuario,idsucursal,tipo,destino,observacion,fecha,huella,resultado) values(?,?,?,?,?,?,?,?,?)",req.idOperacion(),actor.id(),site,req.tipo(),destination,note,LocalDateTime.now(),fingerprint,data.json.writeValueAsString(response));
  return response;
 }
 @SuppressWarnings("unchecked") Map<String,Object> replay(Map<String,Object> saved,MobileSession.User actor,String fingerprint,int site){
  if(!(saved.get("idusuario") instanceof Number id)||id.intValue()!=actor.id()||((Number)saved.get("idsucursal")).intValue()!=site||!saved.get("huella").equals(fingerprint))throw conflict("El identificador ya se utilizó para otro movimiento o almacén");
  return data.json.readValue(saved.get("resultado").toString(),Map.class);
 }
 record Product(Integer id,String codigo,String nombre,BigDecimal precio,Integer minimo,String categoria,String marca,String ubicacion){}
 @Transactional Map<String,Object> saveProduct(MobileSession.User actor,Product req,int site){
  sessions.admin(actor);
  String code=clean(req.codigo(),50),name=clean(req.nombre(),300),category=clean(req.categoria(),200),brand=clean(req.marca(),100),location=clean(req.ubicacion(),100);
  if(code.isBlank()||name.isBlank()||req.precio()==null||req.precio().signum()<0||req.precio().compareTo(new BigDecimal("99999999.99"))>0||req.minimo()==null||req.minimo()<0||req.minimo()>1000000)throw bad("Completa código, nombre, precio y stock mínimo");
  data.db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  int categoryId=data.category(category.isBlank()?"Sin clasificar":category);Integer brandId=null;
  if(!brand.isBlank()){var b=data.db.queryForList("select idmarca from marcas where lower(nombre)=lower(?)",brand);brandId=b.isEmpty()?data.insert("insert into marcas(nombre,activo) values(?,true)",brand):((Number)b.getFirst().get("idmarca")).intValue();}
  int id;
  if(req.id()==null){id=data.insert("insert into producto(idcategorias,idmarca,codigo,nombre,precioventa,stockminimo,activo,fechacreacion) values(?,?,?,?,?,?,?,?)",categoryId,brandId,code,name,req.precio(),req.minimo(),true,LocalDateTime.now());data.ensureStocks();data.db.update("update inventario set ubicacion=? where idsucursal=? and idproducto=?",location,site,id);}
  else {id=req.id();if(data.db.queryForObject("select count(*) from inventario where idsucursal=? and idproducto=?",Integer.class,site,id)==0)throw bad("Producto no disponible");
   data.db.update("update producto set idcategorias=?,idmarca=?,codigo=?,nombre=?,precioventa=?,stockminimo=? where idproducto=?",categoryId,brandId,code,name,req.precio(),req.minimo(),id);
   data.db.update("update inventario set ubicacion=?,actualizacion=? where idsucursal=? and idproducto=?",location,LocalDateTime.now(),site,id);}
  String op=UUID.randomUUID().toString();var response=Map.<String,Object>of("id",id,"codigo",code,"nombre",name);
  data.db.update("insert into mobile_operacion(id,idusuario,idsucursal,tipo,fecha,huella,resultado) values(?,?,?,?,?,?,?)",op,actor.id(),site,"Producto",LocalDateTime.now(),MobileSession.hash(data.json.writeValueAsString(req)),data.json.writeValueAsString(response));return response;
 }
 static String clean(String s,int max){String value=Objects.toString(s,"").strip();if(value.length()>max)throw bad("Un campo supera el tamaño permitido");return value;}
 static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
