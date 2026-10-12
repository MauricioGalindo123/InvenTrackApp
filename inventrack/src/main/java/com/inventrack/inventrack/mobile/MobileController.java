package com.inventrack.inventrack.mobile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.*;

@RestController @RequestMapping("/api/mobile")
public class MobileController {
 final MobileSession sessions;final WarehouseService warehouse;final ExcelService excel;final MobilePush push;
 final MobileDatabase data;final PasswordEncoder passwords;
 MobileController(MobileSession sessions,WarehouseService warehouse,ExcelService excel,MobilePush push,MobileDatabase data,PasswordEncoder passwords){this.sessions=sessions;this.warehouse=warehouse;this.excel=excel;this.push=push;this.data=data;this.passwords=passwords;}
 MobileSession.User user(HttpServletRequest req){return (MobileSession.User)req.getAttribute("mobileUser");}
 int site(HttpServletRequest req){return data.resolveWarehouse(req.getHeader("X-Almacen-Id"));}
 record Login(String correo,String contrasena){}
 record Setup(String clave,String nombre,String correo,String contrasena){}
 @GetMapping("/public-config") Map<String,Object> config(){var response=new LinkedHashMap<String,Object>(push.publicConfig());response.put("setup",sessions.needsSetup());response.put("catalogoInicial",4566);response.put("version","1.2.0-render");return response;}
 @PostMapping("/setup") Map<String,Object> setup(@RequestBody Setup req,HttpServletResponse res){return sessions.setup(req.clave(),req.nombre(),req.correo(),req.contrasena(),res);}
 @PostMapping("/login") Map<String,Object> login(@RequestBody Login req,HttpServletResponse res){return sessions.login(req.correo(),req.contrasena(),res);}
 @GetMapping("/session") Map<String,Object> session(HttpServletRequest req,HttpServletResponse res){var u=user(req);sessions.renew(req,res,u);return sessions.profile(u);}
 record Logout(String idDispositivo){}
 @PostMapping("/logout") Map<String,Object> logout(@RequestBody(required=false)Logout body,HttpServletRequest req,HttpServletResponse res){sessions.logout(user(req),body==null?null:body.idDispositivo(),res);return Map.of("mensaje","Sesión cerrada");}
 @GetMapping("/warehouses") List<Map<String,Object>> warehouses(){return data.warehouses();}
 @GetMapping("/summary") Map<String,Object> summary(HttpServletRequest req){return warehouse.summary(site(req));}
 @GetMapping("/products") Map<String,Object> products(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="todos")String estado,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="40")int tamano,HttpServletRequest req){return warehouse.products(site(req),q,estado,pagina,tamano);}
 @PostMapping("/products") Map<String,Object> saveProduct(@RequestBody WarehouseService.Product req,HttpServletRequest request){return warehouse.saveProduct(user(request),req,site(request));}
 @GetMapping("/low-stock") List<Map<String,Object>> lowStock(@RequestParam(defaultValue="false")boolean todos,HttpServletRequest req){int selected=site(req);return todos?warehouse.allLowStock():warehouse.lowStock(selected);}
 @GetMapping("/movements") List<Map<String,Object>> history(HttpServletRequest req){return warehouse.history(site(req),100);}
 @PostMapping("/movements") Map<String,Object> movement(@RequestBody WarehouseService.Movement req,HttpServletRequest request){return warehouse.movement(user(request),req,site(request));}
 @PostMapping("/imports/preview") Map<String,Object> preview(@RequestParam("archivo")MultipartFile file,HttpServletRequest req)throws IOException{sessions.admin(user(req));site(req);return excel.preview(file);}
 @PostMapping("/imports/confirm") Map<String,Object> importExcel(@RequestParam("archivo")MultipartFile file,@RequestParam(defaultValue="nuevos")String modo,HttpServletRequest req)throws IOException{return excel.importCatalog(user(req),file,modo,site(req));}
 @GetMapping("/exports/{type}") ResponseEntity<byte[]> export(@PathVariable String type,HttpServletRequest req)throws IOException{if(!Set.of("productos","movimientos").contains(type))throw WarehouseService.bad("Selecciona productos o movimientos");int selected=site(req);return ResponseEntity.ok().header("Content-Disposition","attachment; filename=InvenTrack-"+type+"-almacen-"+selected+".xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excel.export(type,selected));}
 @PostMapping("/devices") Map<String,Object> device(@RequestBody MobilePush.Device req,HttpServletRequest request){push.register(user(request),req);return Map.of("mensaje","Avisos activados en este teléfono");}
 @GetMapping("/users") List<Map<String,Object>> users(HttpServletRequest req){sessions.admin(user(req));return data.db.queryForList("select idusuario as id,nombre,correo,rol,activo from usuario where idsucursal=? order by nombre",data.warehouse());}
 record NewUser(String nombre,String correo,String contrasena,String rol){}
 @PostMapping("/users") Map<String,Object> createUser(@RequestBody NewUser req,HttpServletRequest request){sessions.admin(user(request));sessions.validateAccount(req.nombre(),req.correo(),req.contrasena());if(!Set.of("Administrador","Almacen").contains(Objects.toString(req.rol(),"")))throw WarehouseService.bad("Selecciona un rol válido");int id=data.insert("insert into usuario(idsucursal,nombre,correo,contrasena,rol,activo,fechacreacion) values(?,?,?,?,?,true,?)",data.warehouse(),req.nombre().strip(),req.correo().strip().toLowerCase(Locale.ROOT),passwords.encode(req.contrasena()),req.rol(),LocalDateTime.now());return Map.of("id",id,"mensaje","Usuario creado");}
}
