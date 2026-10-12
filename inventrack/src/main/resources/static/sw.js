const CACHE='inventrack-shell-v4-warehouses';
const ASSETS=['/','/app.css','/app.js','/icon.svg','/icon-192.png','/icon-512.png','/manifest.webmanifest'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS.map(url=>new Request(url,{cache:'reload'})))));self.skipWaiting();});
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
 const url=new URL(event.request.url);
 if(event.request.method!=='GET'||url.origin!==self.location.origin||url.pathname.startsWith('/api/'))return;
 if(!ASSETS.includes(url.pathname))return;
 event.respondWith(fetch(event.request,{cache:'no-cache'}).then(response=>{if(response.ok){const copy=response.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy));}return response;}).catch(()=>caches.match(event.request)));
});
self.addEventListener('push',event=>{
 let data;try{data=event.data.json();}catch{return;}
 event.waitUntil(self.registration.showNotification(data.title||'Reposición pendiente',{body:data.body,tag:'stock-bajo',renotify:false,icon:'/icon-192.png',badge:'/icon-192.png',data:{url:'/#alertas'}}));
});
self.addEventListener('notificationclick',event=>{
 event.notification.close();const url=new URL('/#alertas',self.location.origin).href;
 event.waitUntil(clients.matchAll({type:'window',includeUncontrolled:true}).then(async windows=>{
  for(const client of windows)if(new URL(client.url).origin===self.location.origin){await client.navigate(url);return client.focus();}
  return clients.openWindow(url);
 }));
});
