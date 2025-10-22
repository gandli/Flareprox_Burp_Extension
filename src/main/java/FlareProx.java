import burp.api.montoya.MontoyaApi;
import java.util.ArrayList;
import java.util.List;

public class FlareProx {
    public static class Endpoint {
        public final String name;
        public final String url;

        public Endpoint(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static class Result {
        public final List<Endpoint> created = new ArrayList<>();
        public int failed = 0;
    }

    public static class FlareProxException extends RuntimeException {
        public FlareProxException(String msg) {
            super(msg);
        }
    }

    private final CloudflareService cloudflare;

    public FlareProx(CloudflareService cloudflare) {
        this.cloudflare = cloudflare;
    }

    public Result createProxies(int count, MontoyaApi api) {
        if (cloudflare == null) {
            throw new FlareProxException("FlareProx not configured");
        }

        api.logging().logToOutput("\nCreating " + count + " FlareProx endpoint" + (count != 1 ? "s" : "") + "...");

        Result results = new Result();
        for (int i = 0; i < count; i++) {
            try {
                Endpoint endpoint = cloudflare.createDeployment();
                results.created.add(endpoint);
                api.logging().logToOutput("  [" + (i + 1) + "/" + count + "] " + endpoint.name + " -> " + endpoint.url);
            } catch (FlareProxException e) {
                api.logging().logToOutput("  Failed to create endpoint " + (i + 1) + ": " + e.getMessage());
                results.failed += 1;
            }
        }

        cloudflare.syncEndpoints();

        int totalCreated = results.created.size();
        api.logging().logToOutput("\nCreated: " + totalCreated + ", Failed: " + results.failed);
        return results;
    }
}