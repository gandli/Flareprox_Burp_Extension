public final class CloudflareWorkerUtils {
    private CloudflareWorkerUtils() {}

    public static String getWorkerScript() {
        return "const ALLOWED=['accept','accept-language','accept-encoding','authorization','cache-control','content-type','origin','referer','user-agent'];"
                + "addEventListener('fetch',e=>e.respondWith(handle(e.request)));" +
                "async function handle(req){" +
                "  const u=new URL(req.url);" +
                "  let t=u.searchParams.get('url')||req.headers.get('X-Target-URL')||((u.pathname!=='/'&&u.pathname.slice(1).startsWith('http'))?u.pathname.slice(1):null);"
                + "  if(!t) return json({error:'No target URL', usage:{query:'?url=https://example.com', header:'X-Target-URL', path:'/https://example.com'}},400);"
                + "  try{ new URL(t);}catch(e){ return json({error:'Invalid target URL', provided:t},400);}" +
                "  const ps=new URLSearchParams(); for(const [k,v] of u.searchParams){ if(!['url','_cb','_t'].includes(k)) ps.append(k,v); }"
                + "  const T=new URL(t); if(ps.toString()) T.search=ps.toString();" +
                "  const h=new Headers(); for(const [k,v] of req.headers){ if(ALLOWED.includes(k.toLowerCase())) h.set(k,v);} h.set('Host',T.hostname);"
                + "  ['x-forwarded-for','x-real-ip','true-client-ip','cf-connecting-ip','forwarded','x-client-ip','x-cluster-client-ip','x-original-forwarded-for'].forEach(hn=>h.delete(hn));"
                + "  h.set('X-Forwarded-For',[1,2,3,4].map(()=>Math.floor(Math.random()*255)+1).join('.')); const proto=T.protocol.replace(':',''); h.set('Forwarded','for=\"0.0.0.0\";proto='+proto);"
                + "  const prox=new Request(T.toString(),{method:req.method, headers:h, body:['GET','HEAD'].includes(req.method)?null:req.body});"
                + "  const r=await fetch(prox); const rh=new Headers(); for(const [k,v] of r.headers){ if(!['content-encoding','content-length','transfer-encoding'].includes(k.toLowerCase())) rh.set(k,v); }"
                + "  rh.set('Access-Control-Allow-Origin','*'); rh.set('Access-Control-Allow-Methods','GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD'); rh.set('Access-Control-Allow-Headers','*');"
                + "  if(req.method==='OPTIONS') return new Response(null,{status:204,headers:rh});" +
                "  return new Response(r.body,{status:r.status,statusText:r.statusText,headers:rh});" +
                "}" +
                "function json(obj,status){ return new Response(JSON.stringify(obj),{status,headers:{'Content-Type':'application/json'}}); }";
    }

    public static String getClassicWorkerScript() {
        return "addEventListener('fetch',e=>e.respondWith(handleRequest(e.request)));" +
                "function json(o,s){return new Response(JSON.stringify(o),{status:s,headers:{'Content-Type':'application/json'}})}"
                + "async function handleRequest(req){const u=new URL(req.url);let t=u.searchParams.get('url')||req.headers.get('X-Target-URL')||((u.pathname!=='/'&&u.pathname.slice(1).startsWith('http'))?u.pathname.slice(1):null);if(!t)return json({error:'No target URL'},400);try{new URL(t);}catch(e){return json({error:'Invalid target URL',provided:t},400);}const ps=new URLSearchParams();for(const[k,v]of u.searchParams){if(!['url','_cb','_t'].includes(k))ps.append(k,v);}const T=new URL(t);if(ps.toString())T.search=ps.toString();const h=new Headers();for(const[k,v]of req.headers){h.set(k,v);}h.set('Host',T.hostname);const prox=new Request(T.toString(),{method:req.method,headers:h,body:['GET','HEAD'].includes(req.method)?null:req.body});const r=await fetch(prox);const rh=new Headers();for(const[k,v]of r.headers){if(!['content-encoding','content-length','transfer-encoding'].includes(k.toLowerCase()))rh.set(k,v);}rh.set('Access-Control-Allow-Origin','*');rh.set('Access-Control-Allow-Methods','GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD');rh.set('Access-Control-Allow-Headers','*');if(req.method==='OPTIONS')return new Response(null,{status:204,headers:rh});return new Response(r.body,{status:r.status,statusText:r.statusText,headers:rh});}";
    }

    public static String buildMultipart(String boundary, String metadataJson, String filename, String contentType,
                                       String script, String fieldName) {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"metadata\"\r\n");
        sb.append("Content-Type: application/json\r\n\r\n");
        sb.append(metadataJson).append("\r\n");
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"")
                .append(fieldName).append("\"; filename=\"")
                .append(filename).append("\"\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n\r\n");
        sb.append(script).append("\r\n");
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }
}