import { createECDH } from 'node:crypto';
const keys = createECDH('prime256v1');
keys.generateKeys();
console.log('MOBILE_VAPID_PUBLIC=' + keys.getPublicKey().toString('base64url'));
console.log('MOBILE_VAPID_PRIVATE=' + keys.getPrivateKey().toString('base64url'));
console.log('Guarda la clave privada solo en las variables de tu servidor.');
