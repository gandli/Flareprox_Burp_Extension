import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MultipartBuilderTest {
    private static Object newService() throws Exception {
        Class<?> svc = Class.forName("Extension$SimpleCloudflareService");
        Class<?> montoya = Class.forName("burp.api.montoya.MontoyaApi");
        Constructor<?> ctor = svc.getDeclaredConstructor(String.class, String.class, montoya);
        return ctor.newInstance("tok", "acc", null);
    }

    private static String invokeBuild(Object service, String boundary, String metadata, String filename,
                                      String contentType, String script, String fieldName) throws Exception {
        Method m = service.getClass().getDeclaredMethod(
                "buildMultipart",
                String.class, String.class, String.class, String.class, String.class, String.class
        );
        m.setAccessible(true);
        return (String) m.invoke(service, boundary, metadata, filename, contentType, script, fieldName);
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    private static void testModuleMultipartStructure() throws Exception {
        Object service = newService();
        String boundary = "----TestBoundaryABC";
        String metadata = "{\"main_module\":\"worker.js\",\"compatibility_date\":\"2024-07-01\"}";
        String script = "export default {}";
        String body = invokeBuild(service, boundary, metadata, "worker.js", "application/javascript", script, "worker.js");

        assertTrue(body.startsWith("--" + boundary + "\r\n"), "Boundary start missing");
        assertTrue(body.contains("Content-Disposition: form-data; name=\"metadata\"\r\n"), "Metadata disposition missing");
        assertTrue(body.contains("Content-Type: application/json\r\n\r\n"), "Metadata content type missing");
        assertTrue(body.contains(metadata + "\r\n"), "Metadata payload missing");
        assertTrue(body.contains("--" + boundary + "\r\n"), "Second boundary segment missing");
        assertTrue(body.contains("Content-Disposition: form-data; name=\"worker.js\"; filename=\"worker.js\"\r\n"), "Script disposition incorrect");
        assertTrue(body.contains("Content-Type: application/javascript\r\n\r\n"), "Script content type incorrect");
        assertTrue(body.contains(script + "\r\n"), "Script payload missing");
        assertTrue(body.endsWith("--" + boundary + "--\r\n"), "Boundary end missing");
    }

    private static void testClassicMultipartStructure() throws Exception {
        Object service = newService();
        String boundary = "----TestBoundaryXYZ";
        String metadata = "{\"body_part\":\"script\",\"compatibility_date\":\"2024-07-01\"}";
        String script = "addEventListener('fetch',()=>{});";
        String body = invokeBuild(service, boundary, metadata, "worker.js", "application/javascript", script, "script");

        assertTrue(body.startsWith("--" + boundary + "\r\n"), "Boundary start missing");
        assertTrue(body.contains("Content-Disposition: form-data; name=\"metadata\"\r\n"), "Metadata disposition missing");
        assertTrue(body.contains("Content-Type: application/json\r\n\r\n"), "Metadata content type missing");
        assertTrue(body.contains(metadata + "\r\n"), "Metadata payload missing");
        assertTrue(body.contains("--" + boundary + "\r\n"), "Second boundary segment missing");
        assertTrue(body.contains("Content-Disposition: form-data; name=\"script\"; filename=\"worker.js\"\r\n"), "Script disposition incorrect");
        assertTrue(body.contains("Content-Type: application/javascript\r\n\r\n"), "Script content type incorrect");
        assertTrue(body.contains(script + "\r\n"), "Script payload missing");
        assertTrue(body.endsWith("--" + boundary + "--\r\n"), "Boundary end missing");
    }

    public static void main(String[] args) throws Exception {
        testModuleMultipartStructure();
        testClassicMultipartStructure();
        System.out.println("MultipartBuilderTest: basic validations passed.");
    }
}