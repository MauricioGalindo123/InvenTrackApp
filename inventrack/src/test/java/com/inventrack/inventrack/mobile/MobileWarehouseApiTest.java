package com.inventrack.inventrack.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import jakarta.servlet.http.Cookie;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import com.inventrack.inventrack.config.DatabaseInitializer;

/** Prueba la API móvil real, las transacciones y el catálogo completo, sin servicios push externos. */
@SpringBootTest(properties={
 "mobile.enabled=true", "mobile.setup-token=ClaveAlmacenPrueba", "mobile.cookie-secure=false",
 "spring.datasource.url=jdbc:h2:mem:mobile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class MobileWarehouseApiTest {
 @Autowired MockMvc mvc;
 @Autowired MobileDatabase data;
 @Autowired MobileSession sessions;
 @Autowired WarehouseService warehouse;
 Cookie cookie;
 String csrf;
 int owner, first, second;
 String originalName;
 static final java.util.concurrent.atomic.AtomicInteger setupIp=new java.util.concurrent.atomic.AtomicInteger();

 @TestBean(enforceOverride=true) DatabaseInitializer databaseInitializer;
 static DatabaseInitializer databaseInitializer(){return new DatabaseInitializer(null){@Override public void run(ApplicationArguments args){}};}

 @BeforeEach void owner() throws Exception {
  MvcResult result=mvc.perform(post("/api/mobile/setup").with(req->{req.setRemoteAddr("192.0.2."+setupIp.incrementAndGet());return req;}).contentType(MediaType.APPLICATION_JSON)
    .content("{\"clave\":\"ClaveAlmacenPrueba\",\"nombre\":\"Propietario\",\"correo\":\"dueno@example.com\",\"contrasena\":\"AlmacenPrueba123\"}"))
    .andExpect(status().isOk()).andReturn();
  var user=json(result);owner=((Number)user.get("id")).intValue();csrf=user.get("csrf").toString();
  cookie=new Cookie(MobileSession.COOKIE,result.getResponse().getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1]);
  var ids=data.db.queryForList("select idproducto from inventario where idsucursal=? order by idproducto limit 2",Integer.class,data.warehouse());first=ids.get(0);second=ids.get(1);
  originalName=data.db.queryForObject("select nombre from producto where idproducto=?",String.class,first);
 }
 @AfterEach void resetTestWarehouse(){
  data.db.update("delete from movimiento_inventario where idsucursal in (select idsucursal from sucursales where codigomovil in ('general','beauty','mm'))");
  data.db.update("delete from mobile_operacion where idsucursal in (select idsucursal from sucursales where codigomovil in ('general','beauty','mm')) and tipo<>'CatalogoInicial'");
  data.db.update("delete from mobile_dispositivo where idusuario in (select idusuario from usuario where idsucursal=?)",data.warehouse());
  data.db.update("delete from mobile_sesion where idusuario in (select idusuario from usuario where idsucursal=?)",data.warehouse());
  data.db.update("delete from usuario where idsucursal=?",data.warehouse());
  data.db.update("delete from inventario where idproducto in (select idproducto from producto where codigo='NEW-ZERO')");
  data.db.update("delete from producto where codigo='NEW-ZERO'");
  data.db.update("update inventario set cantidaddisponible=0,conteoreal=false,ubicacion=null where idsucursal in (select idsucursal from sucursales where codigomovil in ('general','beauty','mm'))");
  if(originalName!=null)data.db.update("update producto set nombre=? where idproducto=?",originalName,first);
 }
 @SuppressWarnings("unchecked") Map<String,Object> json(MvcResult result){return data.json.readValue(result.getResponse().getContentAsByteArray(),Map.class);}
 <T extends AbstractMockHttpServletRequestBuilder<T>> T auth(T req){return req.cookie(cookie).header("X-CSRF-Token",csrf);}
 int stock(int id){return data.db.queryForObject("select cantidaddisponible from inventario where idsucursal=? and idproducto=?",Integer.class,data.warehouse(),id);}
 String movement(String id,String type,String destination,int... values){
  List<Map<String,Integer>> lines=new ArrayList<>();for(int n=0;n<values.length;n+=2)lines.add(Map.of("idProducto",values[n],"cantidad",values[n+1]));
  return data.json.writeValueAsString(Map.of("idOperacion",id,"tipo",type,"destino",destination,"observacion","Prueba de almacén","productos",lines));
 }
 MvcResult move(String body) throws Exception{return mvc.perform(auth(post("/api/mobile/movements")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();}
 byte[] book(String code,String name,int reportedStock,boolean duplicate) throws Exception{
  try(var book=new XSSFWorkbook();var bytes=new ByteArrayOutputStream()){
   var sheet=book.createSheet("REPORTE");sheet.createRow(0).createCell(0).setCellValue("REPORTE DE STOCK");
   var head=sheet.createRow(1);String[] columns={"CÓDIGO","PRODUCTO","PRECIO UNITARIO","SUCURSAL 1","TOTAL"};for(int c=0;c<5;c++)head.createCell(c).setCellValue(columns[c]);
   for(int n=0;n<(duplicate?2:1);n++){var row=sheet.createRow(n+2);row.createCell(0).setCellValue(code);row.createCell(1).setCellValue(name);row.createCell(2).setCellValue(12.50);row.createCell(3).setCellValue(reportedStock);row.createCell(4).setCellValue(reportedStock);}
   book.write(bytes);return bytes.toByteArray();
  }
 }
 MockMultipartFile file(byte[] bytes){return new MockMultipartFile("archivo","reporte.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes);}

 @Test void abreLaInterfazYConfiguracionMovilSinExigirJWT() throws Exception {
  mvc.perform(get("/index.html")).andExpect(status().isOk())
   .andExpect(result -> assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
     .contains("InvenTrack · Mi almacén"))
   .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Prueba los endpoints"))));
  for(String path:List.of("/app.js","/app.css","/sw.js","/manifest.webmanifest","/icon.svg","/icon-192.png","/icon-512.png"))
   mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-cache"));
  mvc.perform(get("/api/mobile/public-config")).andExpect(status().isOk())
   .andExpect(jsonPath("$.version").value("1.2.0-render"))
   .andExpect(jsonPath("$.catalogoInicial").value(4566));
  mvc.perform(get("/api/mobile/public-config").header("Authorization","Bearer token-falso"))
   .andExpect(status().isOk());
  mvc.perform(get("/api/mobile/summary").header("Authorization","Bearer token-falso"))
   .andExpect(status().isUnauthorized());
  mvc.perform(get("/academico.html")).andExpect(status().isNotFound());
 }

 @Test void reiniciarElServidorConservaLosConteosYElCatalogo() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,23));
  data.run(null);
  assertThat(stock(first)).isEqualTo(23);
  mvc.perform(auth(get("/api/mobile/summary"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.productos").value(4566))
   .andExpect(jsonPath("$.pendientes").value(4565));
 }

 @Test void todosLos4566ProductosEmpiezanEnCeroYNoGeneranAlertas() throws Exception {
  mvc.perform(auth(get("/api/mobile/summary"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.productos").value(4566)).andExpect(jsonPath("$.unidades").value(0))
   .andExpect(jsonPath("$.pendientes").value(4566)).andExpect(jsonPath("$.bajos").value(0));
  assertThat(data.db.queryForObject("select count(*) from inventario where idsucursal=? and (cantidaddisponible<>0 or conteoreal=true)",Integer.class,data.warehouse())).isZero();
  assertThat(data.db.queryForObject("select count(*) from producto where stockminimo=3",Integer.class)).isEqualTo(4566);
  mvc.perform(auth(get("/api/mobile/low-stock"))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(auth(get("/api/mobile/products").param("q","DM001"))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].codigo").value("DM001"));
 }

 @Test void conservaSesionYBloqueaAccesoSinUsuarioOCSRF() throws Exception {
  mvc.perform(get("/api/mobile/summary")).andExpect(status().isUnauthorized());
  mvc.perform(get("/producto/listar")).andExpect(status().isNotFound());
  mvc.perform(get("/producto;version=1/listar")).andExpect(status().isNotFound());
  mvc.perform(get("/api/mobile/session").cookie(cookie)).andExpect(status().isOk())
   .andExpect(jsonPath("$.correo").value("dueno@example.com"))
   .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("HttpOnly"),org.hamcrest.Matchers.containsString("Max-Age=2592000"))));
  String body=movement(UUID.randomUUID().toString(),"Entrada","",first,10);
  mvc.perform(post("/api/mobile/movements").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  mvc.perform(auth(post("/api/mobile/movements")).header("Origin","https://ajeno.example.com").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  assertThat(stock(first)).isZero();
  mvc.perform(auth(post("/api/mobile/logout"))).andExpect(status().isOk());
  mvc.perform(get("/api/mobile/session").cookie(cookie)).andExpect(status().isUnauthorized());
 }

 @Test void registraIngresoSalidaConDestinoYSinDuplicarPorReintento() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Entrada","",first,10,second,9));
  String id=UUID.randomUUID().toString();String exit=movement(id,"Salida","Tienda 2",first,4,second,2);
  var firstResponse=json(move(exit));var retry=json(move(exit));assertThat(retry).isEqualTo(firstResponse);
  assertThat(stock(first)).isEqualTo(6);assertThat(stock(second)).isEqualTo(7);
  assertThat(data.db.queryForObject("select count(*) from movimiento_inventario where idoperacion=?",Integer.class,id)).isEqualTo(2);
  assertThat(data.db.queryForMap("select idusuario,destino from mobile_operacion where id=?",id)).containsEntry("idusuario",owner).containsEntry("destino","Tienda 2");
  mvc.perform(auth(post("/api/mobile/movements")).contentType(MediaType.APPLICATION_JSON).content(movement(id,"Salida","Tienda 1",first,1)))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").exists());
  assertThat(stock(first)).isEqualTo(6);
 }

 @Test void revierteTodaLaSalidaSiUnProductoNoAlcanza() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Entrada","",first,10,second,3));
  String id=UUID.randomUUID().toString();
  mvc.perform(auth(post("/api/mobile/movements")).contentType(MediaType.APPLICATION_JSON).content(movement(id,"Salida","Tienda 1",first,2,second,9)))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
  assertThat(stock(first)).isEqualTo(10);assertThat(stock(second)).isEqualTo(3);
  assertThat(data.db.queryForObject("select count(*) from mobile_operacion where id=?",Integer.class,id)).isZero();
  assertThat(data.db.queryForObject("select count(*) from movimiento_inventario where idoperacion=?",Integer.class,id)).isZero();
 }

 @Test void dosSalidasSimultaneasNoPuedenDescontarMasDeLoDisponible() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,5));
  CountDownLatch start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)) {
   List<Future<Integer>> responses=new ArrayList<>();
   for(int n=0;n<2;n++) {
    String body=movement(UUID.randomUUID().toString(),"Salida","Tienda 1",first,4);
    responses.add(pool.submit(()->{start.await();return mvc.perform(auth(post("/api/mobile/movements"))
     .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();}));
   }
   start.countDown();
   assertThat(List.of(responses.get(0).get(20,TimeUnit.SECONDS),responses.get(1).get(20,TimeUnit.SECONDS)))
    .containsExactlyInAnyOrder(200,409);
  }
  assertThat(stock(first)).isEqualTo(1);
 }

 @Test void elMismoMovimientoSimultaneoSeRegistraUnaSolaVez() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,5));
  String id=UUID.randomUUID().toString(),body=movement(id,"Salida","Tienda 2",first,4);
  CountDownLatch start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)) {
   List<Future<String>> responses=new ArrayList<>();
   for(int n=0;n<2;n++) responses.add(pool.submit(()->{start.await();return mvc.perform(auth(post("/api/mobile/movements"))
    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();}));
   start.countDown();
   assertThat(responses.get(0).get(20,TimeUnit.SECONDS)).isEqualTo(responses.get(1).get(20,TimeUnit.SECONDS));
  }
  assertThat(stock(first)).isEqualTo(1);
  assertThat(data.db.queryForObject("select count(*) from mobile_operacion where id=?",Integer.class,id)).isEqualTo(1);
 }

 @Test void soloLosProductosContadosFormanElResumenDeReposicion() throws Exception {
  mvc.perform(auth(post("/api/mobile/movements")).contentType(MediaType.APPLICATION_JSON).content(movement(UUID.randomUUID().toString(),"Salida","",first,1)))
   .andExpect(status().isConflict());
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,0,second,3));
  mvc.perform(auth(get("/api/mobile/summary"))).andExpect(jsonPath("$.bajos").value(2)).andExpect(jsonPath("$.pendientes").value(4564));
  mvc.perform(auth(get("/api/mobile/low-stock"))).andExpect(jsonPath("$.length()").value(2));
  move(movement(UUID.randomUUID().toString(),"Entrada","",first,5));
  mvc.perform(auth(get("/api/mobile/low-stock"))).andExpect(jsonPath("$.length()").value(1));
 }

 @Test void elEquipoPuedeMoverStockPeroNoCambiarCatalogoOAjustarConteoConfirmado() throws Exception {
  mvc.perform(auth(post("/api/mobile/users")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"nombre\":\"Equipo\",\"correo\":\"equipo@example.com\",\"contrasena\":\"AlmacenPrueba123\",\"rol\":\"Almacen\"}")).andExpect(status().isOk());
  var result=mvc.perform(post("/api/mobile/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"equipo@example.com\",\"contrasena\":\"AlmacenPrueba123\"}")).andExpect(status().isOk()).andReturn();
  csrf=json(result).get("csrf").toString();cookie=new Cookie(MobileSession.COOKIE,result.getResponse().getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1]);
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,10));
  move(movement(UUID.randomUUID().toString(),"Salida","Tienda 1",first,2));
  mvc.perform(auth(post("/api/mobile/movements")).contentType(MediaType.APPLICATION_JSON).content(movement(UUID.randomUUID().toString(),"Conteo","",first,100)))
   .andExpect(status().isForbidden());
  mvc.perform(auth(get("/api/mobile/users"))).andExpect(status().isForbidden());
  mvc.perform(auth(multipart("/api/mobile/imports/preview").file(file(book("NEW001","Nuevo",90,false))))).andExpect(status().isForbidden());
  assertThat(stock(first)).isEqualTo(8);
 }

 @Test void importaExcelIgnorandoLasCantidadesYConservaStockAlActualizar() throws Exception {
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,19));
  String code=data.db.queryForObject("select codigo from producto where idproducto=?",String.class,first);
  var existing=file(book(code,"Nombre actualizado",9999,false));
  mvc.perform(auth(multipart("/api/mobile/imports/preview").file(existing))).andExpect(status().isOk()).andExpect(jsonPath("$.existentes").value(1)).andExpect(jsonPath("$.cantidadesIniciales").value(0));
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(existing).param("modo","actualizar"))).andExpect(status().isOk()).andExpect(jsonPath("$.actualizados").value(1));
  assertThat(stock(first)).isEqualTo(19);
  var newFile=file(book("NEW-ZERO","Producto agregado",-100,false));
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(newFile).param("modo","nuevos"))).andExpect(status().isOk()).andExpect(jsonPath("$.nuevos").value(1));
  int id=data.db.queryForObject("select idproducto from producto where codigo='NEW-ZERO'",Integer.class);assertThat(stock(id)).isZero();
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(newFile).param("modo","nuevos"))).andExpect(status().isOk()).andExpect(jsonPath("$.repetido").value(true));
  assertThat(data.db.queryForObject("select count(*) from producto where codigo='NEW-ZERO'",Integer.class)).isEqualTo(1);
 }

 @Test void noAceptaCodigosDuplicadosEnExcelYExportaCantidadesReales() throws Exception {
  var duplicate=file(book("REPETIDO","Producto",8,true));
  mvc.perform(auth(multipart("/api/mobile/imports/preview").file(duplicate))).andExpect(status().isOk()).andExpect(jsonPath("$.errores.length()").value(1));
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(duplicate).param("modo","nuevos"))).andExpect(status().isBadRequest());
  var result=mvc.perform(auth(get("/api/mobile/exports/productos"))).andExpect(status().isOk()).andReturn();
  try(var book=new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){var sheet=book.getSheetAt(0);assertThat(sheet.getLastRowNum()).isEqualTo(4566);for(int row=1;row<=4566;row++)assertThat(sheet.getRow(row).getCell(3).getNumericCellValue()).isZero();}
 }

 int site(String code){return data.db.queryForObject("select idsucursal from sucursales where codigomovil=?",Integer.class,code);}
 int stockAt(int site,int product){return data.db.queryForObject("select cantidaddisponible from inventario where idsucursal=? and idproducto=?",Integer.class,site,product);}
 MvcResult moveAt(int site,String body)throws Exception{return mvc.perform(auth(post("/api/mobile/movements")).header("X-Almacen-Id",site).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();}

 @Test void separaConteosIngresosSalidasResumenEHistorialEntreTresAlmacenes()throws Exception {
  int general=site("general"),beauty=site("beauty"),mm=site("mm");
  mvc.perform(auth(get("/api/mobile/warehouses"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].nombre").value(MobileDatabase.GENERAL))
   .andExpect(jsonPath("$[1].nombre").value("Altillo Beauty")).andExpect(jsonPath("$[2].nombre").value("Altillo M&M"));
  moveAt(general,movement(UUID.randomUUID().toString(),"Conteo","",first,24));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Conteo","",first,8));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Entrada","",first,2));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Salida","Mostrador Beauty",first,3));
  assertThat(stockAt(general,first)).isEqualTo(24);assertThat(stockAt(beauty,first)).isEqualTo(7);assertThat(stockAt(mm,first)).isZero();
  for(int selected:List.of(general,beauty,mm))mvc.perform(auth(get("/api/mobile/summary")).header("X-Almacen-Id",selected))
   .andExpect(jsonPath("$.productos").value(4566)).andExpect(jsonPath("$.unidades").value(selected==general?24:selected==beauty?7:0))
   .andExpect(jsonPath("$.pendientes").value(selected==mm?4566:4565));
  mvc.perform(auth(get("/api/mobile/movements")).header("X-Almacen-Id",general)).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(auth(get("/api/mobile/movements")).header("X-Almacen-Id",beauty)).andExpect(jsonPath("$.length()").value(3));
  mvc.perform(auth(get("/api/mobile/movements")).header("X-Almacen-Id",mm)).andExpect(jsonPath("$.length()").value(0));
 }

 @Test void migrarElAlmacenAnteriorConservaIdsCuentaSesionConteosEHistorial()throws Exception {
  int previous=data.warehouse(),beauty=site("beauty"),mm=site("mm");
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,23));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Conteo","",first,5));
  data.db.update("update sucursales set codigomovil=null,nombremovil=null where idsucursal=?",previous);
  data.run(null);data.run(null);
  assertThat(data.warehouse()).isEqualTo(previous);assertThat(site("general")).isEqualTo(previous);
  assertThat(site("beauty")).isEqualTo(beauty);assertThat(site("mm")).isEqualTo(mm);
  assertThat(stock(first)).isEqualTo(23);assertThat(stockAt(beauty,first)).isEqualTo(5);assertThat(stockAt(mm,first)).isZero();
  assertThat(data.db.queryForObject("select nombre from sucursales where idsucursal=?",String.class,previous)).isEqualTo(MobileDatabase.WAREHOUSE);
  mvc.perform(get("/api/mobile/session").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner));
  mvc.perform(auth(get("/api/mobile/movements"))).andExpect(jsonPath("$.length()").value(1));
  assertThat(data.db.queryForObject("select count(*) from (select idsucursal,idproducto from inventario group by idsucursal,idproducto having count(*)>1) duplicates",Integer.class)).isZero();
 }

 @Test void noAdmiteAlmacenesAjenosNiReutilizaUnaOperacionEnOtroAlmacen()throws Exception {
  int beauty=site("beauty");String id=UUID.randomUUID().toString(),body=movement(id,"Conteo","",first,8);
  var original=json(move(body));
  mvc.perform(auth(post("/api/mobile/movements")).header("X-Almacen-Id",beauty).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
  assertThat(stock(first)).isEqualTo(8);assertThat(stockAt(beauty,first)).isZero();assertThat(json(move(body))).isEqualTo(original);
  int foreign=data.insert("insert into sucursales(nombre,activo) values(?,true)","Sucursal ajena para prueba");
  try {
   for(String header:List.of(Integer.toString(foreign),"9999999","otro")){
    mvc.perform(auth(get("/api/mobile/products")).header("X-Almacen-Id",header)).andExpect(status().isBadRequest());
    mvc.perform(auth(get("/api/mobile/low-stock").param("todos","true")).header("X-Almacen-Id",header)).andExpect(status().isBadRequest());
   }
  }finally{data.db.update("delete from sucursales where idsucursal=?",foreign);}
  mvc.perform(get("/api/mobile/warehouses")).andExpect(status().isUnauthorized());
 }

 @Test void unaOperacionSimultaneaNoSePuedeAplicarEnDosAlmacenes()throws Exception {
  String id=UUID.randomUUID().toString(),body=movement(id,"Conteo","",first,8);
  CountDownLatch start=new CountDownLatch(1);List<Future<Integer>> responses=new ArrayList<>();
  try(var pool=Executors.newFixedThreadPool(2)){
   for(int selected:List.of(data.warehouse(),site("beauty")))responses.add(pool.submit(()->{
    start.await();return mvc.perform(auth(post("/api/mobile/movements")).header("X-Almacen-Id",selected).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
   }));
   start.countDown();
   assertThat(List.of(responses.get(0).get(20,TimeUnit.SECONDS),responses.get(1).get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
  }
  assertThat(stock(first)+stockAt(site("beauty"),first)).isEqualTo(8);
  assertThat(data.db.queryForObject("select count(*) from mobile_operacion where id=?",Integer.class,id)).isEqualTo(1);
 }

 @Test void importarDesdeUnAltilloAgregaUnSoloCodigoGlobalYConservaLosTresStocks()throws Exception {
  int beauty=site("beauty"),mm=site("mm");
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,19));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Conteo","",first,7));
  moveAt(mm,movement(UUID.randomUUID().toString(),"Conteo","",first,2));
  String code=data.db.queryForObject("select codigo from producto where idproducto=?",String.class,first);
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(file(book(code,"No sobrescribir",9999,false))).param("modo","nuevos")).header("X-Almacen-Id",beauty))
   .andExpect(status().isOk()).andExpect(jsonPath("$.nuevos").value(0)).andExpect(jsonPath("$.omitidos").value(1));
  assertThat(data.db.queryForObject("select nombre from producto where idproducto=?",String.class,first)).isEqualTo(originalName);
  var incoming=file(book("NEW-ZERO","Nuevo producto compartido",9999,false));
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(incoming).param("modo","nuevos")).header("X-Almacen-Id",beauty))
   .andExpect(status().isOk()).andExpect(jsonPath("$.nuevos").value(1));
  int added=data.db.queryForObject("select idproducto from producto where codigo='NEW-ZERO'",Integer.class);
  for(int selected:List.of(data.warehouse(),beauty,mm)){
   assertThat(stockAt(selected,added)).isZero();
   assertThat(data.db.queryForObject("select conteoreal from inventario where idsucursal=? and idproducto=?",Boolean.class,selected,added)).isFalse();
   mvc.perform(auth(get("/api/mobile/products").param("q","NEW-ZERO")).header("X-Almacen-Id",selected)).andExpect(jsonPath("$.total").value(1));
  }
  mvc.perform(auth(multipart("/api/mobile/imports/preview").file(incoming)).header("X-Almacen-Id",mm)).andExpect(jsonPath("$.nuevos").value(0)).andExpect(jsonPath("$.existentes").value(1));
  mvc.perform(auth(multipart("/api/mobile/imports/confirm").file(incoming).param("modo","nuevos")).header("X-Almacen-Id",mm)).andExpect(jsonPath("$.repetido").value(true));
  assertThat(data.db.queryForObject("select count(*) from producto where codigo='NEW-ZERO'",Integer.class)).isEqualTo(1);
  assertThat(data.db.queryForObject("select count(*) from inventario where idproducto=?",Integer.class,added)).isEqualTo(3);
  assertThat(stock(first)).isEqualTo(19);assertThat(stockAt(beauty,first)).isEqualTo(7);assertThat(stockAt(mm,first)).isEqualTo(2);
 }

 @Test void elExcelExportadoContieneSoloElStockDelAlmacenElegidoYSuNombre()throws Exception {
  int beauty=site("beauty");move(movement(UUID.randomUUID().toString(),"Conteo","",first,23));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Conteo","",first,9));
  for(int selected:List.of(data.warehouse(),beauty)){
   var result=mvc.perform(auth(get("/api/mobile/exports/productos")).header("X-Almacen-Id",selected)).andExpect(status().isOk()).andReturn();
   try(var book=new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){
    var sheet=book.getSheetAt(0);assertThat(sheet.getRow(0).getCell(9).getStringCellValue()).isEqualTo("ALMACEN");double total=0;
    for(int row=1;row<=sheet.getLastRowNum();row++){total+=sheet.getRow(row).getCell(3).getNumericCellValue();assertThat(sheet.getRow(row).getCell(9).getStringCellValue()).isEqualTo(data.warehouseName(selected));}
    assertThat(total).isEqualTo(selected==beauty?9:23);
   }
  }
 }

 @Test void editarUbicacionEnUnAltilloNoCambiaLaUbicacionNiStockDeLosDemas()throws Exception {
  int beauty=site("beauty"),mm=site("mm");move(movement(UUID.randomUUID().toString(),"Conteo","",first,9));
  moveAt(beauty,movement(UUID.randomUUID().toString(),"Conteo","",first,4));
  var product=data.db.queryForMap("select codigo,nombre,precioventa,stockminimo from producto where idproducto=?",first);
  for(int selected:List.of(data.warehouse(),beauty)){
   String body=data.json.writeValueAsString(Map.of("id",first,"codigo",product.get("codigo"),"nombre",product.get("nombre"),"precio",product.get("precioventa"),"minimo",product.get("stockminimo"),"categoria","Sin clasificar","marca","","ubicacion",selected==beauty?"Altillo B":"Estante A"));
   mvc.perform(auth(post("/api/mobile/products")).header("X-Almacen-Id",selected).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  }
  assertThat(data.db.queryForObject("select ubicacion from inventario where idsucursal=? and idproducto=?",String.class,data.warehouse(),first)).isEqualTo("Estante A");
  assertThat(data.db.queryForObject("select ubicacion from inventario where idsucursal=? and idproducto=?",String.class,beauty,first)).isEqualTo("Altillo B");
  assertThat(data.db.queryForObject("select ubicacion from inventario where idsucursal=? and idproducto=?",String.class,mm,first)).isNull();
  assertThat(stock(first)).isEqualTo(9);assertThat(stockAt(beauty,first)).isEqualTo(4);assertThat(stockAt(mm,first)).isZero();
 }

 MobilePush testPush(List<Map<String,Object>> sent){return new MobilePush(data,warehouse,"public-test","private-test","mailto:test@example.com","","","","","",true){
  @Override boolean sendGrouped(Map<String,Object> device,String title,String body,int count){sent.add(Map.of("title",title,"body",body,"count",count));return true;}
 };}
 MobilePush.Device registerPush(MobilePush push){var device=new MobilePush.Device(UUID.randomUUID().toString(),"web","https://fcm.googleapis.com/fcm/send/test","public-key-test","auth-test");push.register(new MobileSession.User(owner,"Propietario","dueno@example.com","Administrador",csrf,""),device);return device;}

 @Test void unAvisoAgrupaElMismoProductoConBajoStockEnDistintosAlmacenes()throws Exception {
  List<Map<String,Object>> sent=new ArrayList<>();var push=testPush(sent);var device=registerPush(push);
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,2));
  moveAt(site("beauty"),movement(UUID.randomUUID().toString(),"Conteo","",first,1));
  push.deliver();push.deliver();assertThat(sent).hasSize(1);assertThat(sent.getFirst()).containsEntry("count",2);assertThat(sent.getFirst().get("body").toString()).contains("2 almacenes");
  mvc.perform(auth(get("/api/mobile/low-stock"))).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(auth(get("/api/mobile/low-stock").param("todos","true"))).andExpect(jsonPath("$.length()").value(2));
  moveAt(site("mm"),movement(UUID.randomUUID().toString(),"Conteo","",first,0));push.deliver();assertThat(sent).hasSize(1);
  data.db.update("update mobile_dispositivo set ultimoenvio=? where id=?",java.time.LocalDateTime.now().minusMinutes(2),device.id());push.deliver();
  assertThat(sent).hasSize(2);assertThat(sent.get(1)).containsEntry("count",3);assertThat(sent.get(1).get("body").toString()).contains("3 almacenes");
 }

 @Test void conservaLosAvisosAnterioresYDetectaOtroAlmacenParaElMismoCodigo()throws Exception {
  List<Map<String,Object>> sent=new ArrayList<>();var push=testPush(sent);var device=registerPush(push);
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,2));
  data.db.update("update mobile_dispositivo set notificados=? where id=?","["+first+"]",device.id());
  push.deliver();assertThat(sent).isEmpty();
  moveAt(site("beauty"),movement(UUID.randomUUID().toString(),"Conteo","",first,1));push.deliver();
  assertThat(sent).hasSize(1);assertThat(sent.getFirst()).containsEntry("count",2);
 }

 @Test void enviaUnSoloAvisoAgrupadoSinRepetirAlAbrirLaAplicacion()throws Exception {
  List<Map<String,Object>> sent=new ArrayList<>();
  var push=new MobilePush(data,warehouse,"public-test","private-test","mailto:test@example.com","","","","","",true){
   @Override boolean sendGrouped(Map<String,Object> device,String title,String body,int count){sent.add(Map.of("title",title,"body",body,"count",count));return true;}
  };
  var actor=new MobileSession.User(owner,"Propietario","dueno@example.com","Administrador",csrf,"");
  var device=new MobilePush.Device(UUID.randomUUID().toString(),"web","https://fcm.googleapis.com/fcm/send/test","public-key-test","auth-test");
  push.register(actor,device);
  move(movement(UUID.randomUUID().toString(),"Conteo","",first,0,second,2));
  push.deliver();push.deliver();push.register(actor,device);push.deliver();
  assertThat(sent).hasSize(1);assertThat(sent.getFirst()).containsEntry("title","Reposición pendiente").containsEntry("count",2);assertThat(sent.getFirst().get("body").toString()).contains("2 productos");
  int third=data.db.queryForObject("select min(idproducto) from inventario where idsucursal=? and idproducto>?",Integer.class,data.warehouse(),second);
  move(movement(UUID.randomUUID().toString(),"Conteo","",third,1));push.deliver();
  assertThat(sent).hasSize(1);
  data.db.update("update mobile_dispositivo set ultimoenvio=? where id=?",java.time.LocalDateTime.now().minusMinutes(2),device.id());push.deliver();
  assertThat(sent).hasSize(2);assertThat(sent.get(1)).containsEntry("count",3);assertThat(sent.get(1).get("body").toString()).contains("3 productos");
 }
}
