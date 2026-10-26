import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { NO_MATCH, destinationIds, MAX_WAV_BYTES, isValidWav } from './matcher.mjs';

const send = (res, status, value) => {
  res.writeHead(status, {'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});
  res.end(JSON.stringify(value));
};

export function createServer({match, token='', configured=false, rateLimit=20}) {
  const buckets = new Map();
  const authorized = req => {
    if (!token) return true;
    const incoming = Buffer.from(req.headers.authorization || '');
    const expected = Buffer.from('Bearer '+token);
    return incoming.length === expected.length && timingSafeEqual(incoming, expected);
  };
  const server = http.createServer(async (req,res)=>{
    if (req.headers.origin) return send(res,403,{error:'FORBIDDEN'});
    if (req.url === '/health' && req.method === 'GET') return send(res,200,{status:configured?'ready':'configuration_required'});
    const audio = req.url === '/match-audio';
    if (req.url !== '/match' && !audio) return send(res,404,{error:'NOT_FOUND'});
    if (req.method !== 'POST') return send(res,405,{error:'METHOD_NOT_ALLOWED'});
    if (!authorized(req)) return send(res,401,{error:'UNAUTHORIZED'});
    if (req.headers['content-type']?.split(';')[0].trim().toLowerCase() !== (audio?'audio/wav':'application/json')) return send(res,415,{error:'INVALID_CONTENT_TYPE'});
    const now = Date.now();
    for (const [key,bucket] of buckets) if (bucket.until <= now) buckets.delete(key);
    const address = req.socket.remoteAddress;
    const bucket = buckets.get(address) || {count:0,until:now+60000};
    buckets.set(address,bucket);
    if (++bucket.count > rateLimit || buckets.size > 1024) return send(res,429,{error:'RATE_LIMITED'});
    let size = 0;
    const chunks = [];
    let oversized = false;
    try {
      for await (const chunk of req) {
        size += chunk.length;
        if (size > (audio?MAX_WAV_BYTES:2048)) { oversized = true; send(res,413,{error:'REQUEST_TOO_LARGE'}); req.resume(); break; }
        chunks.push(chunk);
      }
      if (oversized) return;
      const body = audio ? Buffer.concat(chunks) : JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if (audio ? !isValidWav(body) : (!body || typeof body !== 'object' || Array.isArray(body) || Object.keys(body).length !== 1 || typeof body.text !== 'string')) return send(res,400,{error:'INVALID_REQUEST'});
      const destination = await match(audio?body:body.text);
      return send(res,200,{destination:destinationIds.has(destination)?destination:NO_MATCH});
    } catch (error) {
      if (res.headersSent || res.destroyed) return;
      if (error instanceof SyntaxError) return send(res,400,{error:'INVALID_REQUEST'});
      send(res,error.message === 'GEMINI_NOT_CONFIGURED'?503:502,{error:error.message === 'GEMINI_NOT_CONFIGURED'?'GEMINI_NOT_CONFIGURED':'MATCH_UNAVAILABLE'});
    }
  });
  server.requestTimeout = 20000;
  server.headersTimeout = 10000;
  return server;
}
