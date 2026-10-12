package com.inventrack.inventrack.mobile;

import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Component @Order(10)
public class MobileDatabase implements ApplicationRunner {
 public static final String WAREHOUSE = "Almacén M&M";
 final JdbcTemplate db;
 final JsonMapper json;
 private final DataSource source;
 private final TransactionTemplate tx;
 private final boolean enabled, seedCatalog;
 final int defaultMinimum;
 private volatile int warehouse;

 public MobileDatabase(JdbcTemplate db, JsonMapper json, DataSource source, TransactionTemplate tx,
  @Value("${mobile.enabled:true}") boolean enabled, @Value("${mobile.seed-catalog:true}") boolean seedCatalog,
  @Value("${mobile.default-minimum:3}") int defaultMinimum) {
  this.db=db; this.json=json;this.source=source;this.tx=tx;this.enabled=enabled;this.seedCatalog=seedCatalog;this.defaultMinimum=Math.max(0,defaultMinimum);
 }
 @Override public void run(ApplicationArguments args) throws Exception {
  if (!enabled) return;
  try(var con=source.getConnection()) {
   boolean exists;
   try(var tables=con.getMetaData().getTables(null,null,"categorias",new String[]{"TABLE"})) {exists=tables.next();}
   if(!exists) ScriptUtils.executeSqlScript(con,new ClassPathResource("mobile/base-schema.sql"));
   ScriptUtils.executeSqlScript(con,new ClassPathResource("mobile/mobile-schema.sql"));
  }
  tx.executeWithoutResult(status -> {
   var found=db.queryForList("select idsucursal from sucursales where nombre=?",WAREHOUSE);
   warehouse=found.isEmpty()?insert("insert into sucursales(nombre,direccion,telefono,activo) values(?,?,?,?)",WAREHOUSE,"Almacén",null,true):((Number)found.getFirst().get("idsucursal")).intValue();
   if(seedCatalog) seed();
  });
 }
 @SuppressWarnings("unchecked") private void seed() {
  try(var in=new ClassPathResource("mobile/catalogo-keyfacil.json").getInputStream()) {
   byte[] bytes=in.readAllBytes();String fingerprint=MobileSession.hash(bytes);
   String op="catalogo-inicial-"+fingerprint.substring(0,24);
   if(db.queryForObject("select count(*) from mobile_operacion where id=?",Integer.class,op)>0)return;
   var rows=(List<Map<String,Object>>)(List<?>)json.readValue(bytes,List.class);
   int category=category("Sin clasificar");
   Set<String> codes=new HashSet<>(db.queryForList("select codigo from producto",String.class));
   List<Object[]> additions=new ArrayList<>();
   for(var row:rows) if(codes.add(row.get("codigo").toString())) additions.add(new Object[]{category,row.get("codigo"),row.get("nombre"),row.get("precioVenta"),defaultMinimum,true,LocalDateTime.now()});
   db.batchUpdate("insert into producto(idcategorias,codigo,nombre,precioventa,stockminimo,activo,fechacreacion) values(?,?,?,?,?,?,?)",additions);
   Map<String,Integer> ids=new HashMap<>();db.query("select codigo,idproducto from producto",rs->{ids.put(rs.getString(1),rs.getInt(2));});
   Set<Integer> present=new HashSet<>(db.queryForList("select idproducto from inventario where idsucursal=?",Integer.class,warehouse));
   List<Object[]> stocks=new ArrayList<>();
   for(var row:rows) {int id=ids.get(row.get("codigo").toString());if(present.add(id))stocks.add(new Object[]{warehouse,id,0,false,LocalDateTime.now()});}
   db.batchUpdate("insert into inventario(idsucursal,idproducto,cantidaddisponible,conteoreal,actualizacion) values(?,?,?,?,?)",stocks);
   db.update("insert into mobile_operacion(id,idsucursal,tipo,fecha,huella,resultado) values(?,?,?,?,?,?)",op,warehouse,"CatalogoInicial",LocalDateTime.now(),fingerprint,json.writeValueAsString(Map.of("productos",rows.size(),"cantidadInicial",0)));
  }catch(Exception ex){throw new IllegalStateException("No se pudo cargar el catálogo inicial",ex);}
 }
 int category(String name) {
  var rows=db.queryForList("select idcategorias from categorias where lower(nombre)=lower(?)",name);
  return rows.isEmpty()?insert("insert into categorias(nombre) values(?)",name):((Number)rows.getFirst().get("idcategorias")).intValue();
 }
 int insert(String sql,Object...args) {
  String table=sql.substring("insert into ".length(),sql.indexOf('(')).strip();
  // PostgreSQL devuelve la fila completa si se usa RETURN_GENERATED_KEYS sin columnas.
  String identity=switch(table){
   case "sucursales" -> "idsucursal";
   case "categorias" -> "idcategorias";
   case "producto" -> "idproducto";
   case "marcas" -> "idmarca";
   case "usuario" -> "idusuario";
   default -> throw new IllegalArgumentException("Tabla sin identidad configurada");
  };
  var key=new GeneratedKeyHolder();
  db.update(con->{var stmt=con.prepareStatement(sql,new String[]{identity});for(int n=0;n<args.length;n++)stmt.setObject(n+1,args[n]);return stmt;},key);
  return Objects.requireNonNull(key.getKey()).intValue();
 }
 int warehouse(){if(warehouse==0)throw new IllegalStateException("El almacén aún no está preparado");return warehouse;}
}
