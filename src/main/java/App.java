import com.server.pojos.HttpResponse;

import java.io.IOException;

import static com.server.contract.HttpMethod.GET;

public class App {
    public static void main(String[] args) throws IOException {
        Server myServer = new Server(8080);
        myServer.addRoute(GET, "/testOne",
                (req) -> new HttpResponse.Builder()
                        .setStatusCode(200)
                        .addHeader("Content-Type", "text/html")
                        .setEntity("<HTML> <P> Hello There... </P> </HTML>")
                        .build());
        myServer.addRoute(GET, "/testTwo",
                (req) -> new HttpResponse.Builder()
                        .setStatusCode(200)
                        .addHeader("Content-Type", "text/plain")
                        .setEntity("Response for /testTwo")
                        .build());
        myServer.start();
    }
}
