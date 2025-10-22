import java.util.List;

public interface CloudflareService {
    FlareProx.Endpoint createDeployment() throws FlareProx.FlareProxException;

    void syncEndpoints();

    void deleteEndpoints(List<FlareProx.Endpoint> endpoints);

    List<FlareProx.Endpoint> listEndpoints();

    void cleanupAll();
}