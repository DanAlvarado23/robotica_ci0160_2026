import { createMatcher } from './matcher.mjs';
import { createServer } from './http-server.mjs';

const host = process.env.HOST || '127.0.0.1';
const token = process.env.DESTINATION_API_TOKEN || '';
if (!['127.0.0.1','::1','localhost'].includes(host) && token.length < 32) throw new Error('Para escuchar en red configure un token de al menos 32 caracteres y un proxy HTTPS.');
const port = Number(process.env.PORT || 8787);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Puerto inválido');
const apiKey = process.env.GEMINI_API_KEY || '';
const server = createServer({match:createMatcher({apiKey,model:process.env.GEMINI_MODEL || 'gemini-3.5-flash-lite'}),token,configured:Boolean(apiKey.trim())});
server.listen(port,host,()=>console.log('Leitroboter: http://'+host+':'+port+'; Gemini '+(apiKey.trim()?'configurado':'pendiente de GEMINI_API_KEY')));
for (const signal of ['SIGTERM','SIGINT']) process.on(signal,()=>server.close(()=>process.exit(0)));
