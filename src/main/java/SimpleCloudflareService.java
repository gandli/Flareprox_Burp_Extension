import burp.api.montoya.MontoyaApi;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SimpleCloudflareService implements CloudflareService {
    private final String token;
    private final String accountId;
    private final MontoyaApi api;
    private String cachedSubdomain;

    public SimpleCloudflareService(String token, String accountId, MontoyaApi api) {
        this.token = token;
        this.accountId = accountId;
        this.api = api;
    }

    @Override
    public FlareProx.Endpoint createDeployment() {
        if (token == null || token.isBlank() || accountId == null || accountId.isBlank()) {
            throw new FlareProx.FlareProxException("Missing API token or account ID");
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "flareprox-" + id;

        // 1) 上传 Worker 脚本 (multipart PUT) + 模块化 -> Classic 兜底
        String putUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + name;
        String metadataJson = "{\"main_module\":\"worker.js\",\"compatibility_date\":\"2024-07-01\"}";
        String scriptModule = CloudflareWorkerUtils.getWorkerScript();
        String boundary = "----BurpBoundary" + UUID.randomUUID().toString().replace("-", "");
        String body = CloudflareWorkerUtils.buildMultipart(boundary, metadataJson, "worker.js", "application/javascript", scriptModule,
                "worker.js");
        // 预览 multipart 结构便于诊断
        api.logging().logToOutput("[CF] Multipart preview boundary=" + boundary
                + ", metadata_len=" + metadataJson.length()
                + ", script_len=" + scriptModule.length()
                + ", field=worker.js, content_type=application/javascript");
        String head = body.substring(0, Math.min(body.length(), 300));
        String tail = body.substring(Math.max(0, body.length() - 120));
        api.logging().logToOutput("[CF] Multipart head >>>\n" + head.replace("\r", "\\r"));
        api.logging().logToOutput("[CF] Multipart tail >>>\n" + tail.replace("\r", "\\r"));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
        HttpRequest putReq = HttpRequest.newBuilder(URI.create(putUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("User-Agent", "flareprox-burp/1.0")
                .timeout(java.time.Duration.ofSeconds(30))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> putResp = client.send(putReq, HttpResponse.BodyHandlers.ofString());
            api.logging().logToOutput("[CF] Deploy Worker status=" + putResp.statusCode());
            String cfRay = putResp.headers().firstValue("CF-RAY").orElse("");
            if (!cfRay.isEmpty())
                api.logging().logToOutput("[CF] CF-Ray=" + cfRay);
            boolean ok = putResp.statusCode() >= 200 && putResp.statusCode() < 300
                    && putResp.body().contains("\"success\":true");
            if (!ok) {
                int sc = putResp.statusCode();
                if (sc == 401 || sc == 403) {
                    api.logging().logToError(
                            "[CF] Auth error: verify API token scope 'Edit Cloudflare Workers' and Account ID.");
                    api.logging().raiseInfoEvent(
                            "[CF] Auth error: verify API token scope 'Edit Cloudflare Workers' and Account ID.");
                }
                if (sc == 429) {
                    api.logging().logToError("[CF] Rate limited (429). Retry later.");
                    api.logging().raiseInfoEvent("[CF] Rate limited (429). Retry later.");
                }
                if (sc >= 500) {
                    api.logging().logToError("[CF] Cloudflare service error " + sc + ".");
                    api.logging().raiseInfoEvent("[CF] Cloudflare service error " + sc + ".");
                }
                String respBody = putResp.body();
                // 提取错误细节便于定位问题
                java.util.regex.Matcher codeM = java.util.regex.Pattern
                        .compile("\"code\"\\s*:\\s*(\\d+)").matcher(respBody);
                java.util.regex.Matcher msgM = java.util.regex.Pattern
                        .compile("\"message\"\\s*:\\s*\"([^\"]+)\"").matcher(respBody);
                if (codeM.find() && msgM.find()) {
                    String code = codeM.group(1);
                    String msg = msgM.group(1);
                    api.logging()
                            .logToError("[CF] Error detail: code=" + code + ", message=" + msg);
                    api.logging()
                            .logToOutput("[CF] Error detail: code=" + code + ", message=" + msg + ", status=" + sc);
                    if (!cfRay.isEmpty()) {
                        api.logging().logToOutput("[CF] CF-Ray=" + cfRay);
                    }
                } else {
                    String brief = respBody.length() > 400 ? respBody.substring(0, 400) + "..." : respBody;
                    api.logging().logToOutput("[CF] Response body: " + brief);
                }
                boolean esmError = respBody.contains("Unexpected token 'export'")
                        || respBody.contains("\"code\":10021")
                        || putResp.statusCode() == 415
                        || (sc == 400 && respBody.contains("\"errors\""))
                        || respBody.toLowerCase().contains("unsupported media")
                        || respBody.contains("\"main_module\"");
                if (esmError) {
                    // Fallback to Classic script
                    String classicMetadata = "{\"body_part\":\"script\",\"compatibility_date\":\"2024-07-01\"}";
                    String classicScript = getClassicWorkerScript();
                    String boundary2 = "----BurpBoundary" + UUID.randomUUID().toString().replace("-", "");
                    String body2 = buildMultipart(boundary2, classicMetadata, "worker.js", "application/javascript",
                            classicScript, "script");
                    HttpRequest putReq2 = HttpRequest.newBuilder(URI.create(putUrl))
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "application/json")
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary2)
                            .header("User-Agent", "flareprox-burp/1.0")
                            .timeout(java.time.Duration.ofSeconds(30))
                            .PUT(HttpRequest.BodyPublishers.ofString(body2))
                            .build();
                    HttpResponse<String> putResp2 = client.send(putReq2, HttpResponse.BodyHandlers.ofString());
                    api.logging().logToOutput("[CF] Fallback Classic status=" + putResp2.statusCode());
                    String cfRay2 = putResp2.headers().firstValue("CF-RAY").orElse("");
                    if (!cfRay2.isEmpty())
                        api.logging().logToOutput("[CF] CF-Ray (fallback)=" + cfRay2);
                    ok = putResp2.statusCode() >= 200 && putResp2.statusCode() < 300
                            && (putResp2.body().contains("\"success\":true")
                                    || (!putResp2.body().contains("\"success\":false")
                                            && !putResp2.body().contains("\"errors\""))
                                    || putResp2.body().contains("\"result\""));
                    if (!ok) {
                        String resp2 = putResp2.body();
                        java.util.regex.Matcher codeM2 = java.util.regex.Pattern
                                .compile("\"code\"\\s*:\\s*(\\d+)").matcher(resp2);
                        java.util.regex.Matcher msgM2 = java.util.regex.Pattern
                                .compile("\"message\"\\s*:\\s*\"([^\"]+)\"").matcher(resp2);
                        if (codeM2.find() && msgM2.find()) {
                            String code = codeM2.group(1);
                            String msg = msgM2.group(1);
                            api.logging()
                                    .logToError("[CF] Fallback error detail: code=" + code + ", message=" + msg);
                            api.logging().raiseInfoEvent("[CF] Fallback error detail: code=" + code + ", message="
                                    + msg + ", status=" + putResp2.statusCode());
                            if (!cfRay2.isEmpty()) {
                                api.logging().raiseInfoEvent("[CF] CF-Ray (fallback)=" + cfRay2);
                            }
                        } else {
                            String brief2 = resp2.length() > 400 ? resp2.substring(0, 400) + "..." : resp2;
                            api.logging().raiseInfoEvent("[CF] Response body (fallback): " + brief2);
                        }
                        api.logging().logToError("[CF] Create worker failed body=" + putResp2.body());
                        api.logging().raiseInfoEvent("[CF] Create worker failed status=" + putResp2.statusCode());
                        throw new FlareProx.FlareProxException(
                                "Create worker failed: status=" + putResp2.statusCode());
                    }
                } else {
                    api.logging().logToError("[CF] Create worker failed body=" + respBody);
                    throw new FlareProx.FlareProxException("Create worker failed: status=" + putResp.statusCode());
                }
            }
        } catch (Exception ex) {
            throw new FlareProx.FlareProxException("Create worker error: " + ex.getMessage());
        }

        // 2) 尝试启用脚本的 subdomain (最佳努力)
        try {
            String subEnableUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/"
                    + name + "/subdomain";
            String json = "{\"enabled\": true}";
            HttpRequest subReq = HttpRequest.newBuilder(URI.create(subEnableUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> subResp = client.send(subReq, HttpResponse.BodyHandlers.ofString());
            api.logging().logToOutput("[CF] Enable script subdomain status=" + subResp.statusCode());
        } catch (Exception ignore) {
            api.logging().logToOutput("[CF] Subdomain enable skipped or failed.");
        }

        // 3) 生成 workers.dev URL（含 Python 版的 subdomain 兜底逻辑）
        String subdomain = fetchWorkersSubdomain();
        String url = "https://" + name + "." + subdomain + ".workers.dev";
        api.logging().logToOutput("  [CF] Deployment created: " + name + " -> " + url);
        return new FlareProx.Endpoint(name, url);
    }

    private String fetchWorkersSubdomain() {
        if (cachedSubdomain != null && !cachedSubdomain.isBlank()) {
            return cachedSubdomain;
        }
        HttpClient client = HttpClient.newHttpClient();
        String subUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/subdomain";
        HttpRequest req = HttpRequest.newBuilder(URI.create(subUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            api.logging().logToOutput("[CF] Get workers.dev subdomain status=" + resp.statusCode());
            String body = resp.body();
            if (resp.statusCode() == 200 && body != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\"subdomain\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(body);
                if (m.find()) {
                    cachedSubdomain = m.group(1);
                    api.logging().logToOutput("[CF] Parsed workers.dev subdomain=" + cachedSubdomain);
                    return cachedSubdomain;
                } else {
                    String preview = body.length() > 200 ? body.substring(0, 200) + "..." : body;
                    api.logging().logToOutput("[CF] Could not parse subdomain from body: " + preview);
                }
            } else {
                api.logging().logToOutput("[CF] Get subdomain returned non-200 or empty body");
            }
        } catch (Exception ex) {
            api.logging().logToError("[CF] Get subdomain error: " + ex.getMessage());
        }
        // Fail fast to avoid constructing incorrect workers.dev URLs
        throw new FlareProx.FlareProxException("Unable to resolve workers.dev subdomain");
    }


    private String getClassicWorkerScript() {
        return "addEventListener('fetch',e=>e.respondWith(handleRequest(e.request)));" +
                "function json(o,s){return new Response(JSON.stringify(o),{status:s,headers:{'Content-Type':'application/json'}})}"
                +
                "async function handleRequest(req){const u=new URL(req.url);let t=u.searchParams.get('url')||req.headers.get('X-Target-URL')||((u.pathname!=='/'&&u.pathname.slice(1).startsWith('http'))?u.pathname.slice(1):null);if(!t)return json({error:'No target URL'},400);try{new URL(t);}catch(e){return json({error:'Invalid target URL',provided:t},400);}const ps=new URLSearchParams();for(const[k,v]of u.searchParams){if(!['url','_cb','_t'].includes(k))ps.append(k,v);}const T=new URL(t);if(ps.toString())T.search=ps.toString();const h=new Headers();for(const[k,v]of req.headers){h.set(k,v);}h.set('Host',T.hostname);const prox=new Request(T.toString(),{method:req.method,headers:h,body:['GET','HEAD'].includes(req.method)?null:req.body});const r=await fetch(prox);const rh=new Headers();for(const[k,v]of r.headers){if(!['content-encoding','content-length','transfer-encoding'].includes(k.toLowerCase()))rh.set(k,v);}rh.set('Access-Control-Allow-Origin','*');rh.set('Access-Control-Allow-Methods','GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD');rh.set('Access-Control-Allow-Headers','*');if(req.method==='OPTIONS')return new Response(null,{status:204,headers:rh});return new Response(r.body,{status:r.status,statusText:r.statusText,headers:rh});}";
    }

    // 构造 Cloudflare Workers 上传所需的 multipart/form-data
    private String buildMultipart(String boundary, String metadataJson, String filename, String contentType,
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

    @Override
    public List<FlareProx.Endpoint> listEndpoints() {
        if (token == null || token.isBlank() || accountId == null || accountId.isBlank()) {
            throw new FlareProx.FlareProxException("Missing API token or account ID");
        }
        HttpClient client = HttpClient.newHttpClient();
        String listUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts";
        HttpRequest req = HttpRequest.newBuilder(URI.create(listUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            api.logging().logToOutput("[CF] List Workers status=" + resp.statusCode());
            if (resp.statusCode() != 200) {
                throw new FlareProx.FlareProxException("List Workers failed with status " + resp.statusCode());
            }
            String body = resp.body();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(flareprox-[^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(body);
            List<FlareProx.Endpoint> endpoints = new ArrayList<>();
            String subdomain = fetchWorkersSubdomain();
            while (m.find()) {
                String name = m.group(1);
                String url = "https://" + name + "." + subdomain + ".workers.dev";
                endpoints.add(new FlareProx.Endpoint(name, url));
            }
            return endpoints;
        } catch (Exception ex) {
            throw new FlareProx.FlareProxException("List endpoints error: " + ex.getMessage());
        }
    }

    @Override
    public void syncEndpoints() {
        api.logging().logToOutput("[CF] Syncing endpoints to Cloudflare...");
    }

    @Override
    public void deleteEndpoints(List<FlareProx.Endpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            api.logging().logToOutput("[CF] No endpoints to delete.");
            return;
        }
        HttpClient client = HttpClient.newHttpClient();
        for (FlareProx.Endpoint ep : endpoints) {
            String delUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/"
                    + ep.name;
            HttpRequest req = HttpRequest.newBuilder(URI.create(delUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .DELETE()
                    .build();
            try {
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300
                        && resp.body().contains("\"success\":true");
                api.logging().logToOutput("[CF] Delete " + ep.name + " status=" + resp.statusCode() + " success=" + ok);
            } catch (Exception ex) {
                api.logging().logToError("[CF] Delete failed for " + ep.name + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void cleanupAll() {
        if (token == null || token.isBlank() || accountId == null || accountId.isBlank()) {
            throw new FlareProx.FlareProxException("Missing API token or account ID");
        }
        HttpClient client = HttpClient.newHttpClient();
        String listUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts";
        HttpRequest listReq = HttpRequest.newBuilder(URI.create(listUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = client.send(listReq, HttpResponse.BodyHandlers.ofString());
            api.logging().logToOutput("[CF] List Workers status=" + resp.statusCode());
            if (resp.statusCode() != 200) {
                throw new FlareProx.FlareProxException("List Workers failed with status " + resp.statusCode());
            }
            String body = resp.body();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(flareprox-[^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(body);
            List<String> targets = new ArrayList<>();
            while (m.find()) {
                targets.add(m.group(1));
            }
            int total = targets.size();
            int deleted = 0;
            for (String name : targets) {
                String delUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/"
                        + name;
                HttpRequest delReq = HttpRequest.newBuilder(URI.create(delUrl))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .DELETE()
                        .build();
                HttpResponse<String> delResp = client.send(delReq, HttpResponse.BodyHandlers.ofString());
                boolean ok = delResp.statusCode() >= 200 && delResp.statusCode() < 300
                        && delResp.body().contains("\"success\":true");
                api.logging().logToOutput("[CF] Delete " + name + " status=" + delResp.statusCode() + " success=" + ok);
                if (ok) deleted++;
            }
            api.logging().raiseInfoEvent("Cloudflare Workers cleanup completed: " + deleted + "/" + total);
        } catch (Exception ex) {
            throw new FlareProx.FlareProxException("Cleanup failed: " + ex.getMessage());
        }
    }
}