package com.inventrack.inventrack.mobile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

// Antes de la cadena JWT: también bloquea las rutas académicas cuando se usa el almacén.
@Component @Order(-200)
public class MobileAccessFilter extends OncePerRequestFilter {
 private final MobileSession sessions;
 private final boolean enabled;
 private final Map<String,List<Long>> attempts=new ConcurrentHashMap<>();
 public MobileAccessFilter(MobileSession sessions,@Value("${mobile.enabled:true}")boolean enabled){this.sessions=sessions;this.enabled=enabled;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
  if(!enabled){chain.doFilter(req,res);return;}
  String path=req.getRequestURI();
  res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("Referrer-Policy","same-origin");res.setHeader("X-Frame-Options","DENY");
  res.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'; frame-src 'none'; object-src 'none'");
  if(path.contains(";")||path.contains("%")||path.contains("..")||path.contains("//")){error(res,404,"Ruta no disponible");return;}
  if(!path.startsWith("/api/mobile/")){
   if(!Set.of("/","/index.html","/app.js","/app.css","/sw.js","/manifest.webmanifest","/icon.svg","/icon-192.png","/icon-512.png","/favicon.ico","/error").contains(path)||!Set.of("GET","HEAD").contains(req.getMethod())){error(res,404,"Ruta no disponible en la aplicación del almacén");return;}
   chain.doFilter(req,res);return;
  }
  res.setHeader("Cache-Control","no-store");
  boolean publicPath=Set.of("/api/mobile/public-config","/api/mobile/login","/api/mobile/setup").contains(path);
  try {
   if(!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod())) {
    String origin=req.getHeader("Origin");String expected=req.getScheme()+"://"+req.getHeader("Host");
    if(origin!=null&&!origin.equals(expected))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Origen no autorizado");
   }
   if(publicPath&&!req.getMethod().equals("GET")) {
    String ip=req.getRemoteAddr();long now=Instant.now().getEpochSecond();
    var list=attempts.computeIfAbsent(ip,x->Collections.synchronizedList(new ArrayList<>()));
    synchronized(list){list.removeIf(t->now-t>600);if(list.size()>=20)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espera unos minutos antes de volver a intentar");list.add(now);}
    if(attempts.size()>10000)attempts.entrySet().removeIf(e->e.getValue().isEmpty()||e.getValue().getLast()<now-600);
   }
   if(!publicPath) {
    var user=sessions.require(req);req.setAttribute("mobileUser",user);
    if(!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod())&&!user.csrf().equals(req.getHeader("X-CSRF-Token")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Actualiza la aplicación para continuar");
   }
   chain.doFilter(req,res);
  } catch(ResponseStatusException e){error(res,e.getStatusCode().value(),e.getReason());}
 }
 private void error(HttpServletResponse res,int code,String message)throws IOException {res.setStatus(code);res.setContentType("application/json;charset=UTF-8");res.getWriter().write(sessions.errorJson(message));}
}
