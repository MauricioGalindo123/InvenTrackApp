package com.inventrack.inventrack.mobile;

import java.io.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelService {
 private final MobileDatabase data;
 private final MobileSession sessions;
 ExcelService(MobileDatabase data,MobileSession sessions){this.data=data;this.sessions=sessions;}
 record Item(int fila,String codigo,String nombre,BigDecimal precio){}
 record Parsed(List<Item> items,List<Map<String,Object>> errors,int header){}
 String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-zA-Z0-9]","").toLowerCase(Locale.ROOT);}
 Parsed parse(byte[] bytes) {
  if(bytes.length>20*1024*1024)throw WarehouseService.bad("El archivo supera los 20 MB");
  List<Item> items=new ArrayList<>();List<Map<String,Object>> errors=new ArrayList<>();Set<String> codes=new HashSet<>();
  try(var book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
   var formatter=new DataFormatter(Locale.ROOT);Sheet selected=null;int head=-1,codeCol=-1,nameCol=-1,priceCol=-1;
   outer:for(var sheet:book){for(int r=sheet.getFirstRowNum();r<=Math.min(sheet.getLastRowNum(),20);r++){Row row=sheet.getRow(r);if(row==null)continue;int c=-1,n=-1,p=-1;
    for(var cell:row){String key=normalize(formatter.formatCellValue(cell));if(Set.of("codigo","sku","codigoproducto","codigodeproducto").contains(key))c=cell.getColumnIndex();if(Set.of("producto","nombre","descripcion","nombreproducto","nombredeproducto").contains(key))n=cell.getColumnIndex();if(Set.of("preciounitario","precio","precioventa","preciodeventa").contains(key))p=cell.getColumnIndex();}
    if(c>=0&&n>=0){selected=sheet;head=r;codeCol=c;nameCol=n;priceCol=p;break outer;}
   }}
   if(selected==null)throw WarehouseService.bad("No se encontraron las columnas CÓDIGO y PRODUCTO");
   if(selected.getLastRowNum()-head>20000)throw WarehouseService.bad("El archivo supera los 20,000 productos");
   for(int n=head+1;n<=selected.getLastRowNum();n++){
    Row row=selected.getRow(n);if(row==null)continue;
    String code=formatter.formatCellValue(row.getCell(codeCol)).strip(),name=formatter.formatCellValue(row.getCell(nameCol)).strip();
    if(code.isBlank()&&name.isBlank())continue;
    try{
     if(code.isBlank()||code.length()>50||name.isBlank()||name.length()>300)throw new IllegalArgumentException("Código o nombre incompleto");
     if(!codes.add(code))throw new IllegalArgumentException("Código repetido en el archivo: "+code);
     BigDecimal price=BigDecimal.ZERO;
     if(priceCol>=0){var cell=row.getCell(priceCol);if(cell!=null&&cell.getCellType()==CellType.NUMERIC)price=BigDecimal.valueOf(cell.getNumericCellValue());else {String value=formatter.formatCellValue(cell).replace("S/","").replace(" ","").strip();if(!value.isBlank()){if(value.contains(",")&&value.contains("."))value=value.replace(",","");else value=value.replace(",",".");price=new BigDecimal(value);}}}
     if(price.signum()<0||price.compareTo(new BigDecimal("99999999.99"))>0)throw new IllegalArgumentException("Precio fuera de rango");
     items.add(new Item(n+1,code,name,price.setScale(2,java.math.RoundingMode.HALF_UP)));
    }catch(Exception e){if(errors.size()<100)errors.add(Map.of("fila",n+1,"codigo",code,"mensaje",Objects.toString(e.getMessage(),"Datos inválidos")));}
   }
   if(items.isEmpty()&&errors.isEmpty())throw WarehouseService.bad("El archivo no contiene productos");return new Parsed(items,errors,head+1);
  }catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(Exception e){throw WarehouseService.bad("No se pudo leer el Excel. Usa un archivo .xlsx o .xls válido");}
 }
 Map<String,Object> preview(MultipartFile file)throws IOException{
  var parsed=parse(file.getBytes());Set<String> existing=new HashSet<>(data.db.queryForList("select p.codigo from producto p join inventario i on i.idproducto=p.idproducto where i.idsucursal=?",String.class,data.warehouse()));
  long repeated=parsed.items().stream().filter(i->existing.contains(i.codigo())).count();
  return Map.of("total",parsed.items().size(),"nuevos",parsed.items().size()-repeated,"existentes",repeated,"errores",parsed.errors(),"muestra",parsed.items().stream().limit(8).toList(),"filaCabecera",parsed.header(),"cantidadesIniciales",0,"aviso","Las cantidades de Kayfácil se ignoran. Los productos nuevos comienzan en 0 y los existentes conservan su stock.");
 }
 @Transactional Map<String,Object> importCatalog(MobileSession.User actor,MultipartFile file,String mode)throws IOException{
  sessions.admin(actor);if(!Set.of("nuevos","actualizar").contains(mode))throw WarehouseService.bad("Selecciona cómo importar");
  byte[] bytes=file.getBytes();var parsed=parse(bytes);if(!parsed.errors().isEmpty())throw WarehouseService.bad("Corrige las filas con errores antes de importar");
  String fingerprint=MobileSession.hash(bytes),id="excel-"+fingerprint+"-"+mode;
  data.db.queryForList("select idsucursal from sucursales where idsucursal=? for update",data.warehouse());
  var replay=data.db.queryForList("select resultado from mobile_operacion where id=?",id);
  if(!replay.isEmpty()){var prior=data.json.readValue(replay.getFirst().get("resultado").toString(),Map.class);var res=new LinkedHashMap<String,Object>(prior);res.put("repetido",true);return res;}
  Map<String,Integer> known=new HashMap<>();data.db.query("select codigo,idproducto from producto",rs->{known.put(rs.getString(1),rs.getInt(2));});
  int created=0,updated=0,skipped=0,category=data.category("Sin clasificar");
  for(var item:parsed.items()){
   Integer productId=known.get(item.codigo());boolean inWarehouse=productId!=null&&data.db.queryForObject("select count(*) from inventario where idsucursal=? and idproducto=?",Integer.class,data.warehouse(),productId)>0;
   if(inWarehouse){if(mode.equals("actualizar")){data.db.update("update producto set nombre=?,precioventa=? where idproducto=?",item.nombre(),item.precio(),productId);updated++;}else skipped++;}
   else {if(productId==null){productId=data.insert("insert into producto(idcategorias,codigo,nombre,precioventa,stockminimo,activo,fechacreacion) values(?,?,?,?,?,true,?)",category,item.codigo(),item.nombre(),item.precio(),data.defaultMinimum,LocalDateTime.now());known.put(item.codigo(),productId);}
    data.db.update("insert into inventario(idsucursal,idproducto,cantidaddisponible,conteoreal,actualizacion) values(?,?,0,false,?)",data.warehouse(),productId,LocalDateTime.now());created++;}
  }
  var result=Map.<String,Object>of("nuevos",created,"actualizados",updated,"omitidos",skipped,"repetido",false,"mensaje","Catálogo importado; las cantidades del almacén se conservaron");
  data.db.update("insert into mobile_operacion(id,idusuario,idsucursal,tipo,fecha,huella,resultado) values(?,?,?,?,?,?,?)",id,actor.id(),data.warehouse(),"Importacion",LocalDateTime.now(),fingerprint,data.json.writeValueAsString(result));return result;
 }
 byte[] export(String type)throws IOException{
  try(var book=new XSSFWorkbook();var output=new ByteArrayOutputStream()){
   Sheet sheet=book.createSheet(type.equals("movimientos")?"Movimientos":"Productos");
   List<String> headers;List<Map<String,Object>> rows;
   if(type.equals("movimientos")){headers=List.of("fecha","codigo","producto","tipo","cantidad","destino","usuario","motivo");rows=data.db.queryForList("select m.fecha,p.codigo,p.nombre as producto,m.tipomovimiento as tipo,m.cantidad,coalesce(o.destino,'') as destino,u.nombre as usuario,m.motivo from movimiento_inventario m join producto p on p.idproducto=m.idproducto join usuario u on u.idusuario=m.idusuario left join mobile_operacion o on o.id=m.idoperacion where m.idsucursal=? order by m.fecha desc",data.warehouse());}
   else {headers=List.of("codigo","nombre","precio","cantidad","minimo","categoria","marca","ubicacion","contado");rows=data.db.queryForList(WarehouseService.PRODUCT_SELECT+" where i.idsucursal=? and p.activo=true order by p.nombre",data.warehouse());}
   CellStyle title=book.createCellStyle();Font font=book.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());title.setFont(font);title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
   Row head=sheet.createRow(0);for(int c=0;c<headers.size();c++){Cell cell=head.createCell(c);String key=headers.get(c);cell.setCellValue(switch(key){case "nombre"->"PRODUCTO";case "precio"->"PRECIO UNITARIO";default->key.toUpperCase(Locale.ROOT);});cell.setCellStyle(title);}
   int n=1;for(var record:rows){Row row=sheet.createRow(n++);for(int c=0;c<headers.size();c++){Object value=record.get(headers.get(c));Cell cell=row.createCell(c);if(value instanceof Number num)cell.setCellValue(num.doubleValue());else if(value instanceof Boolean b)cell.setCellValue(b?"Sí":"Pendiente");else cell.setCellValue(Objects.toString(value,""));}}
   for(int c=0;c<headers.size();c++)sheet.setColumnWidth(c,headers.get(c).equals("nombre")||headers.get(c).equals("producto")?58*256:22*256);
   sheet.createFreezePane(0,1);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,n-1),0,headers.size()-1));book.write(output);return output.toByteArray();
  }
 }
}
