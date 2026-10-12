package com.inventrack.inventrack.mobile;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import nl.martijndwars.webpush.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;

@Service @EnableScheduling
public class MobilePush {
 private static final Logger log=LoggerFactory.getLogger(MobilePush.class);
 private final MobileDatabase data;
 private final WarehouseService warehouse;
 private final String vapidPublic,vapidPrivate,vapidSubject,firebaseJson,apiKey,appId,projectId,senderId;
 private final boolean enabled;
 private FirebaseMessaging messaging;
 MobilePush(MobileDatabase data,WarehouseService warehouse,
 @Value("${MOBILE_VAPID_PUBLIC:}")String vapidPublic,@Value("${MOBILE_VAPID_PRIVATE:}")String vapidPrivate,
 @Value("${MOBILE_VAPID_SUBJECT:mailto:admin@inventrack.local}")String vapidSubject,
 @Value("${MOBILE_FIREBASE_SERVICE_ACCOUNT:}")String firebaseJson,
 @Value("${MOBILE_FIREBASE_API_KEY:}")String apiKey,@Value("${MOBILE_FIREBASE_APP_ID:}")String appId,
 @Value("${MOBILE_FIREBASE_PROJECT_ID:}")String projectId,@Value("${MOBILE_FIREBASE_SENDER_ID:}")String senderId,
 @Value("${mobile.enabled:true}")boolean enabled){this.data=data;this.warehouse=warehouse;this.vapidPublic=vapidPublic;this.vapidPrivate=vapidPrivate;this.vapidSubject=vapidSubject;this.firebaseJson=firebaseJson;this.apiKey=apiKey;this.appId=appId;this.projectId=projectId;this.senderId=senderId;this.enabled=enabled;}
 boolean webReady(){return !vapidPublic.isBlank()&&!vapidPrivate.isBlank();}
 boolean androidReady(){return !firebaseJson.isBlank()&&!apiKey.isBlank()&&!appId.isBlank()&&!senderId.isBlank()&&!projectId.isBlank();}
 Map<String,Object> publicConfig(){return Map.of("webPush",webReady(),"androidPush",androidReady(),"vapidPublic",vapidPublic,"firebase",Map.of("apiKey",apiKey,"appId",appId,"projectId",projectId,"senderId",senderId));}
 record Device(String id,String plataforma,String endpoint,String p256dh,String auth){}
 void register(MobileSession.User actor,Device req){
  if(req.id()==null||!req.id().matches("[a-zA-Z0-9_-]{16,64}")||!Set.of("web","android").contains(Objects.toString(req.plataforma(),""))||req.endpoint()==null||req.endpoint().isBlank()||req.endpoint().length()>2000)throw WarehouseService.bad("Dispositivo inválido");
  if(req.plataforma().equals("web")){
   if(!webReady())throw WarehouseService.conflict("Los avisos del almacén todavía no están activados");
   URI endpoint;try{endpoint=URI.create(req.endpoint());}catch(Exception e){throw WarehouseService.bad("Suscripción inválida");}
   String host=Objects.toString(endpoint.getHost(),"").toLowerCase(Locale.ROOT);
   if(!"https".equals(endpoint.getScheme())||endpoint.getPort()!=-1||!(host.equals("fcm.googleapis.com")||host.endsWith(".push.apple.com")||host.equals("web.push.apple.com")||host.equals("updates.push.services.mozilla.com"))||req.p256dh()==null||req.auth()==null||req.p256dh().length()>250||req.auth().length()>100)throw WarehouseService.bad("Servicio de notificaciones no admitido");
  }else if(!androidReady())throw WarehouseService.conflict("Los avisos de Android todavía no están activados");
  if(data.db.update("update mobile_dispositivo set notificados=case when idusuario=? and activo=true then notificados else '[]' end,ultimoenvio=case when idusuario=? and activo=true then ultimoenvio else null end,idusuario=?,plataforma=?,endpoint=?,p256dh=?,auth=?,activo=true where id=?",actor.id(),actor.id(),actor.id(),req.plataforma(),req.endpoint(),req.p256dh(),req.auth(),req.id())==0)
   data.db.update("insert into mobile_dispositivo(id,idusuario,plataforma,endpoint,p256dh,auth,notificados,activo) values(?,?,?,?,?,?,'[]',true)",req.id(),actor.id(),req.plataforma(),req.endpoint(),req.p256dh(),req.auth());
 }
 @Scheduled(fixedDelayString="${mobile.push-interval:30000}",initialDelay=30000)
 @SuppressWarnings("unchecked") public void deliver(){
  if(!enabled||(!webReady()&&!androidReady()))return;
  try{
   var lows=warehouse.allLowStock();List<String> ids=lows.stream().map(r->r.get("almacenid")+":"+r.get("id")).sorted().toList();
   var devices=data.db.queryForList("select d.* from mobile_dispositivo d join usuario u on u.idusuario=d.idusuario where d.activo=true and u.activo=true and u.idsucursal=?",data.warehouse());
   for(var device:devices){
    List<?> prior=data.json.readValue(device.get("notificados").toString(),List.class);
    List<String> previous=prior.stream().map(i->i instanceof Number?data.warehouse()+":"+i:i.toString()).toList();
    boolean newLow=ids.stream().anyMatch(i->!previous.contains(i));
    if(!newLow){data.db.update("update mobile_dispositivo set notificados=? where id=?",data.json.writeValueAsString(ids),device.get("id"));continue;}
    var last=device.get("ultimoenvio");if(last instanceof Timestamp t&&t.toLocalDateTime().isAfter(LocalDateTime.now().minusSeconds(60)))continue;
    long places=lows.stream().map(r->r.get("almacenid")).distinct().count();
    String title="Reposición pendiente",body="Tienes "+ids.size()+" productos con bajo stock en "+places+(places==1?" almacén.":" almacenes.")+" Toca para revisarlos.";
    try{
     if(!sendGrouped(device,title,body,ids.size()))continue;
     data.db.update("update mobile_dispositivo set notificados=?,ultimoenvio=? where id=?",data.json.writeValueAsString(ids),LocalDateTime.now(),device.get("id"));
    }catch(FirebaseMessagingException e){if(e.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED)data.db.update("update mobile_dispositivo set activo=false where id=?",device.get("id"));else log.warn("No se pudo entregar un aviso de stock; se reintentará");}
    catch(Exception e){log.warn("No se pudo entregar un aviso de stock; se reintentará");}
   }
  }catch(Exception e){log.warn("No se pudo comprobar el stock para los avisos");}
 }
 boolean sendGrouped(Map<String,Object> device,String title,String body,int count)throws Exception {
  if(device.get("plataforma").equals("android")){
   if(!androidReady())return false;
   sender().send(Message.builder().setToken(device.get("endpoint").toString()).putData("title",title).putData("body",body).putData("url","#alertas").putData("count",Integer.toString(count)).setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).setCollapseKey("stock-bajo").setTtl(3600000).build()).build());
  }else{
   if(!webReady())return false;
   if(Security.getProvider("BC")==null)Security.addProvider(new BouncyCastleProvider());
   String payload=data.json.writeValueAsString(Map.of("title",title,"body",body,"url","/#alertas","tag","stock-bajo","count",count));
   var notification=new nl.martijndwars.webpush.Notification(device.get("endpoint").toString(),device.get("p256dh").toString(),device.get("auth").toString(),payload);
   var response=new PushService(vapidPublic,vapidPrivate,vapidSubject).send(notification,Encoding.AES128GCM);
   int code=response.getStatusLine().getStatusCode();
   if(code==404||code==410){data.db.update("update mobile_dispositivo set activo=false where id=?",device.get("id"));return false;}
   if(code<200||code>=300)throw new IllegalStateException("No se pudo entregar el aviso");
  }
  return true;
 }
 private synchronized FirebaseMessaging sender()throws Exception{
  if(messaging==null){var credentials=GoogleCredentials.fromStream(new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8)));FirebaseApp app=FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build(),"inventrack-mobile");messaging=FirebaseMessaging.getInstance(app);}return messaging;
 }
}
