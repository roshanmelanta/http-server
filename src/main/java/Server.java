import com.server.contract.HttpMethod;
import com.server.contract.RequestRunner;
import com.server.http.HttpHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Server: minimal multithreaded HTTP server.
 * - Binds to a TCP port, accepts client sockets, and dispatches each to a worker thread.
 * - Keeps a registry of routes keyed by "HTTP_METHOD + path" mapping to RequestRunner handlers.
 * - Delegates per-connection HTTP parsing, routing, and response writing to HttpHandler.
 * - Lifecycle: start() blocks on accept(), submits a task per connection, ensures socket cleanup.
 */
public class Server {
    private final Map<String, RequestRunner> routes;   // "METHOD + route" -> handler
    private final ServerSocket socket;                 // listening socket
    private final Executor threadPool;                 // worker pool for concurrency
    private HttpHandler handler;                       // shared handler using routes

    public Server(int port) throws IOException {
        routes = new HashMap<>();
        threadPool = Executors.newFixedThreadPool(100);
        socket = new ServerSocket(port);
    }

    // Register a route handler, e.g. addRoute(GET, "/users", runner)
    public void addRoute(HttpMethod opCode, String route, RequestRunner runner) {
        routes.put(opCode.name().concat(route), runner);
    }

    // Start accept loop: create handler, accept sockets, hand off to workers
    public void start() throws IOException{
        handler = new HttpHandler(routes);

        while(true) {
            Socket clientConnection = socket.accept();
            handleConnection(clientConnection);
        }
    }

    // Encapsulate per-connection work into a task and execute asynchronously
    private void handleConnection(Socket clientConnection) {
        Runnable httpRequestRunner = () -> {
            try {
                handler.handleConnection(clientConnection.getInputStream(), clientConnection.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try { clientConnection.close(); } catch (IOException e) { /* ignore */ }
            }
        };

        threadPool.execute(httpRequestRunner);
    }
}