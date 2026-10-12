import {randomBytes} from 'node:crypto';
// Copia el resultado únicamente a MOBILE_SETUP_TOKEN en las variables privadas de Render.
console.log(randomBytes(24).toString('base64url'));
