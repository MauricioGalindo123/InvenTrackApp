package com.inventrack.inventrack.mobile;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.*;

@Service
public class MobileSession {
 static final String COOKIE="INVENTRACK_SESSION";
 final MobileDatabase data;
 final JdbcTemplate db;
 final PasswordEncoder passwords;
 final boolean secure;
 final String setupToken;
 public MobileSession(MobileDatabase data,PasswordEncoder passwords,
  @Value("${mobile.cookie-secure:true}") boolean secure,@Value("${mobile.setup-token:${MOBILE_SETUP_TOKEN:}}") String setupToken){this.data=data;this.db=data.db;this.passwords=passwords;this.secure=secure;this.setupToken=setupToken;}
 static String token(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
 static String hash(String s){return hash(s.getBytes(StandardCharsets.UTF_8));}
 record User(int id,String nombre,String correo,String rol,String csrf,String hash){}
 User require(HttpServletRequest req) {
  String raw=null;if(req.getCookies()!=null)for(var c:req.getCookies())if(COOKIE.equals(c.getName()))raw=c.getValue();
  if(raw==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Inicia sesión para continuar");
  var found=db.queryForList("select u.idusuario,u.nombre,u.correo,u.rol,s.csrf,s.tokenhash from mobile_sesion s join usuario u on u.idusuario=s.idusuario where s.tokenhash=? and s.expira>? and u.activo=true and u.idsucursal=?",hash(raw),LocalDateTime.now(),data.warehouse());
  if(found.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tu sesión ya no está disponible");
  var r=found.getFirst();return new User(((Number)r.get("idusuario")).intValue(),r.get("nombre").toString(),r.get("correo").toString(),r.get("rol").toString(),r.get("csrf").toString(),r.get("tokenhash").toString());
 }
 void admin(User u){if(!u.rol().equals("Administrador"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta acción requiere un administrador");}
 String errorJson(String message){return data.json.writeValueAsString(Map.of("mensaje",Objects.toString(message,"Solicitud inválida")));}
 Map<String,Object> profile(User u){return Map.of("id",u.id(),"nombre",u.nombre(),"correo",u.correo(),"rol",u.rol(),"csrf",u.csrf(),"almacen",data.warehouseName(data.warehouse()));}
 boolean needsSetup(){return db.queryForObject("select count(*) from usuario where idsucursal=? and rol='Administrador' and activo=true",Integer.class,data.warehouse())==0;}
 @Transactional Map<String,Object> setup(String key,String name,String email,String password,HttpServletResponse response) {
  db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  if(!needsSetup())throw new ResponseStatusException(HttpStatus.CONFLICT,"El administrador ya está configurado");
  if(setupToken.isBlank()||!MessageDigest.isEqual(setupToken.getBytes(StandardCharsets.UTF_8),Objects.toString(key,"").getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La clave de activación no es válida");
  validateAccount(name,email,password);
  int id=data.insert("insert into usuario(idsucursal,nombre,correo,contrasena,rol,activo,fechacreacion) values(?,?,?,?,?,?,?)",data.warehouse(),name.strip(),email.strip().toLowerCase(Locale.ROOT),passwords.encode(password),"Administrador",true,LocalDateTime.now());
  return login(email,password,response);
 }
 void validateAccount(String name,String email,String password){
  if(name==null||name.isBlank()||name.length()>200||email==null||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||email.length()>100||password==null||password.length()<10||password.length()>72||password.getBytes(StandardCharsets.UTF_8).length>72)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa nombre, correo y una contraseña de 10 a 72 caracteres");
 }
 @Transactional Map<String,Object> login(String email,String password,HttpServletResponse response) {
  var r=db.queryForList("select * from usuario where lower(correo)=lower(?) and activo=true and idsucursal=?",Objects.toString(email,"").strip(),data.warehouse());
  if(r.isEmpty()||password==null||password.getBytes(StandardCharsets.UTF_8).length>72||!passwords.matches(password,r.getFirst().get("contrasena").toString()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos");
  int id=((Number)r.getFirst().get("idusuario")).intValue();String raw=token(),csrf=token(),hash=hash(raw);
  db.update("delete from mobile_sesion where expira<?",LocalDateTime.now());
  db.update("insert into mobile_sesion(tokenhash,idusuario,csrf,expira,creado) values(?,?,?,?,?)",hash,id,csrf,LocalDateTime.now().plusDays(30),LocalDateTime.now());
  db.update("update usuario set acceso=? where idusuario=?",LocalDateTime.now(),id);
  cookie(response,raw,30*86400);
  var u=r.getFirst();return profile(new User(id,u.get("nombre").toString(),u.get("correo").toString(),u.get("rol").toString(),csrf,hash));
 }
 void renew(HttpServletRequest req,HttpServletResponse response,User u){db.update("update mobile_sesion set expira=? where tokenhash=?",LocalDateTime.now().plusDays(30),u.hash());if(req.getCookies()!=null)for(var c:req.getCookies())if(COOKIE.equals(c.getName()))cookie(response,c.getValue(),30*86400);}
 void logout(User u,String device,HttpServletResponse response){db.update("delete from mobile_sesion where tokenhash=?",u.hash());if(device!=null&&!device.isBlank())db.update("update mobile_dispositivo set activo=false where idusuario=? and id=?",u.id(),device);cookie(response,"",0);}
 private void cookie(HttpServletResponse r,String value,long age){r.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,value).path("/").httpOnly(true).secure(secure).sameSite("Lax").maxAge(age).build().toString());}
}
