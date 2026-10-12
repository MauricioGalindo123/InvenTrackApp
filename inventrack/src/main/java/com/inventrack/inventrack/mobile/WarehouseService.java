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
  i.cantidaddisponible as cantidad,i.conteoreal as contado,coalesce(i.ubicacion,'') as ubicacion
  from inventario i join producto p on p.idproducto=i.idproducto
  join categorias c on c.idcategorias=p.idcategorias left join marcas m on m.idmarca=p.idmarca
  """;
 Map<String,Object> summary(){
  var r=data.db.queryForMap("""
   select count(*) as productos,coalesce(sum(i.cantidaddisponible),0) as unidades,
   coalesce(sum(case when not i.conteoreal then 1 else 0 end),0) as pendientes,
   coalesce(sum(case when i.conteoreal and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo then 1 else 0 end),0) as bajos
   from inventario i join producto p on p.idproducto=i.idproducto where i.idsucursal=? and p.activo=true
   """,data.warehouse());
  r.put("recientes",history(6));return r;
 }
 Map<String,Object> products(String search,String status,int page,int size){
  if(page<0||size<1||size>100||search.length()>100)throw bad("La búsqueda no es válida");
  String where=" where i.idsucursal=? and p.activo=true and (lower(p.nombre) like ? or lower(p.codigo) like ?)";
  if(status.equals("bajos"))where+=" and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo";
  else if(status.equals("pendientes"))where+=" and i.conteoreal=false";
  else if(!status.equals("todos"))throw bad("El filtro no es válido");
  String q="%"+search.strip().toLowerCase(Locale.ROOT)+"%";
  var rows=data.db.queryForList(PRODUCT_SELECT+where+" order by p.nombre,p.idproducto limit ? offset ?",data.warehouse(),q,q,size,(long)page*size);
  int count=data.db.queryForObject("select count(*) from inventario i join producto p on p.idproducto=i.idproducto"+where,Integer.class,data.warehouse(),q,q);
  return Map.of("items",rows,"total",count,"pagina",page,"tamano",size);
 }
 List<Map<String,Object>> lowStock(){return data.db.queryForList(PRODUCT_SELECT+" where i.idsucursal=? and p.activo=true and i.conteoreal=true and p.stockminimo>0 and i.cantidaddisponible<=p.stockminimo order by i.cantidaddisponible,p.nombre",data.warehouse());}
 List<Map<String,Object>> history(int limit){var rows=data.db.queryForList("""
  select o.id,o.tipo,o.destino,o.observacion,o.fecha,u.nombre as usuario,o.resultado
  from mobile_operacion o left join usuario u on u.idusuario=o.idusuario
  where o.idsucursal=? and o.tipo in ('Entrada','Salida','Conteo','Importacion','Producto')
  order by o.fecha desc limit ?
  """,data.warehouse(),Math.max(1,Math.min(limit,100)));for(var row:rows)if(row.get("fecha") instanceof java.sql.Timestamp t)row.put("fecha",t.toInstant().toString());return rows;}
 record Line(Integer idProducto,Integer cantidad){}
 record Movement(String idOperacion,String tipo,String destino,String observacion,List<Line> productos){}
 @Transactional Map<String,Object> movement(MobileSession.User actor,Movement req){
  if(req.idOperacion()==null||!req.idOperacion().matches("[a-zA-Z0-9_-]{16,100}")||!Set.of("Entrada","Salida","Conteo").contains(Objects.toString(req.tipo(),"")))throw bad("El movimiento no es válido");
  if(req.productos()==null||req.productos().isEmpty()||req.productos().size()>200)throw bad("Selecciona de 1 a 200 productos");
  String destination=clean(req.destino(),120),note=clean(req.observacion(),500);
  String fingerprint=MobileSession.hash(data.json.writeValueAsString(req));
  var saved=data.db.queryForList("select idusuario,huella,resultado from mobile_operacion where id=?",req.idOperacion());
  if(!saved.isEmpty())return replay(saved.getFirst(),actor,fingerprint);
  // Serializes operation IDs and inventory changes within this warehouse, including imports.
  data.db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  saved=data.db.queryForList("select idusuario,huella,resultado from mobile_operacion where id=?",req.idOperacion());
  if(!saved.isEmpty())return replay(saved.getFirst(),actor,fingerprint);
  Set<Integer> unique=new HashSet<>();List<Map<String,Object>> result=new ArrayList<>();
  var lines=req.productos().stream().sorted(Comparator.comparing(l->Objects.requireNonNullElse(l.idProducto(),0))).toList();
  for(var line:lines){
   if(line.idProducto()==null||line.cantidad()==null||line.cantidad()<0||line.cantidad()>1000000||!unique.add(line.idProducto())||(!req.tipo().equals("Conteo")&&line.cantidad()==0))throw bad("Revisa las cantidades y los productos repetidos");
   var found=data.db.queryForList("select i.idinventario,i.cantidaddisponible,i.conteoreal,p.nombre from inventario i join producto p on p.idproducto=i.idproducto where i.idsucursal=? and i.idproducto=? and p.activo=true for update",data.warehouse(),line.idProducto());
   if(found.isEmpty())throw bad("Un producto no pertenece a este almacén");
   var row=found.getFirst();int before=((Number)row.get("cantidaddisponible")).intValue();boolean counted=Boolean.TRUE.equals(row.get("conteoreal"));
   if(req.tipo().equals("Salida")&&!counted)throw conflict("Registra primero el conteo de "+row.get("nombre"));
   if(req.tipo().equals("Conteo")&&counted)sessions.admin(actor);
   int after=switch(req.tipo()){case "Entrada"->Math.addExact(before,line.cantidad());case "Salida"->before-line.cantidad();default->line.cantidad();};
   if(after<0)throw conflict("Stock insuficiente para "+row.get("nombre")+". Disponible: "+before);
   data.db.update("update inventario set cantidaddisponible=?,conteoreal=true,actualizacion=? where idinventario=?",after,LocalDateTime.now(),row.get("idinventario"));
   int delta=after-before;
   if(delta!=0){String reason=req.tipo().equals("Conteo")?"Conteo físico":req.tipo();if(!destination.isBlank())reason+=" → "+destination;reason=reason.substring(0,Math.min(100,reason.length()));
    data.db.update("insert into movimiento_inventario(idproducto,idusuario,idsucursal,tipomovimiento,cantidad,motivo,fecha,idoperacion) values(?,?,?,?,?,?,?,?)",line.idProducto(),actor.id(),data.warehouse(),delta>0?"Entrada":"Salida",Math.abs(delta),reason,LocalDateTime.now(),req.idOperacion());}
   result.add(Map.of("idProducto",line.idProducto(),"producto",row.get("nombre"),"antes",before,"despues",after,"cantidad",line.cantidad()));
  }
  var response=Map.<String,Object>of("idOperacion",req.idOperacion(),"tipo",req.tipo(),"productos",result,"destino",destination,"mensaje","Movimiento registrado");
  data.db.update("insert into mobile_operacion(id,idusuario,idsucursal,tipo,destino,observacion,fecha,huella,resultado) values(?,?,?,?,?,?,?,?,?)",req.idOperacion(),actor.id(),data.warehouse(),req.tipo(),destination,note,LocalDateTime.now(),fingerprint,data.json.writeValueAsString(response));
  return response;
 }
 @SuppressWarnings("unchecked") Map<String,Object> replay(Map<String,Object> saved,MobileSession.User actor,String fingerprint){
  if(((Number)saved.get("idusuario")).intValue()!=actor.id()||!saved.get("huella").equals(fingerprint))throw conflict("El identificador ya se utilizó para otro movimiento");
  return data.json.readValue(saved.get("resultado").toString(),Map.class);
 }
 record Product(Integer id,String codigo,String nombre,BigDecimal precio,Integer minimo,String categoria,String marca,String ubicacion){}
 @Transactional Map<String,Object> saveProduct(MobileSession.User actor,Product req){
  sessions.admin(actor);
  String code=clean(req.codigo(),50),name=clean(req.nombre(),300),category=clean(req.categoria(),200),brand=clean(req.marca(),100),location=clean(req.ubicacion(),100);
  if(code.isBlank()||name.isBlank()||req.precio()==null||req.precio().signum()<0||req.precio().compareTo(new BigDecimal("99999999.99"))>0||req.minimo()==null||req.minimo()<0||req.minimo()>1000000)throw bad("Completa código, nombre, precio y stock mínimo");
  data.db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  int categoryId=data.category(category.isBlank()?"Sin clasificar":category);Integer brandId=null;
  if(!brand.isBlank()){var b=data.db.queryForList("select idmarca from marcas where lower(nombre)=lower(?)",brand);brandId=b.isEmpty()?data.insert("insert into marcas(nombre,activo) values(?,true)",brand):((Number)b.getFirst().get("idmarca")).intValue();}
  int id;
  if(req.id()==null){id=data.insert("insert into producto(idcategorias,idmarca,codigo,nombre,precioventa,stockminimo,activo,fechacreacion) values(?,?,?,?,?,?,?,?)",categoryId,brandId,code,name,req.precio(),req.minimo(),true,LocalDateTime.now());data.db.update("insert into inventario(idsucursal,idproducto,cantidaddisponible,conteoreal,ubicacion,actualizacion) values(?,?,0,false,?,?)",data.warehouse(),id,location,LocalDateTime.now());}
  else {id=req.id();if(data.db.queryForObject("select count(*) from inventario where idsucursal=? and idproducto=?",Integer.class,data.warehouse(),id)==0)throw bad("Producto no disponible");
   data.db.update("update producto set idcategorias=?,idmarca=?,codigo=?,nombre=?,precioventa=?,stockminimo=? where idproducto=?",categoryId,brandId,code,name,req.precio(),req.minimo(),id);
   data.db.update("update inventario set ubicacion=?,actualizacion=? where idsucursal=? and idproducto=?",location,LocalDateTime.now(),data.warehouse(),id);}
  String op=UUID.randomUUID().toString();var response=Map.<String,Object>of("id",id,"codigo",code,"nombre",name);
  data.db.update("insert into mobile_operacion(id,idusuario,idsucursal,tipo,fecha,huella,resultado) values(?,?,?,?,?,?,?)",op,actor.id(),data.warehouse(),"Producto",LocalDateTime.now(),MobileSession.hash(data.json.writeValueAsString(req)),data.json.writeValueAsString(response));return response;
 }
 static String clean(String s,int max){String value=Objects.toString(s,"").strip();if(value.length()>max)throw bad("Un campo supera el tamaño permitido");return value;}
 static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
