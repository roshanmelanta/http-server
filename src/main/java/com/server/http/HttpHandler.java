package com.server.http;

import com.server.contract.RequestRunner;
import com.server.pojos.HttpRequest;
import com.server.pojos.HttpResponse;
import com.server.writers.ResponseWriter;

import java.io.*;
import java.util.Map;
import java.util.Optional;

/**
 * HttpHandler is responsible for managing a single client connection.
 * - Reads the incoming HTTP request from the InputStream.
 * - Decodes the request using HttpDecoder.
 * - Matches the request (method + URI) against registered routes.
 *     -> If a matching route exists, delegates it to the corresponding RequestRunner.
 *     -> If not, responds with 404 (Route Not Found).
 * - If the request is invalid/unparsable, responds with 400 (Bad Request).
 * - Finally writes the HttpResponse back to the client via OutputStream.
 *
 * In short: Acts as the main controller that routes requests to handlers and sends responses.
 */
public class HttpHandler {
    // Stores route mappings: (HTTP Method + Path) -> handler (RequestRunner)
    private final Map<String, RequestRunner> routes;

    // Constructor initializes route map
    public HttpHandler(Map<String, RequestRunner> routes) {
        this.routes = routes;
    }

    // Handles a single client connection (request/response cycle)
    public void handleConnection(final InputStream inputStream, final OutputStream outputStream) throws IOException {

        // Writer to send response back to client
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

        // Decode incoming request (returns Optional<HttpRequest>)
        Optional<HttpRequest> request = HttpDecoder.decode(inputStream);

        // Process request if valid, otherwise send error response
        request.ifPresentOrElse(
                (r) -> handleRequest(r, bufferedWriter),   // valid request
                () -> handleInvalidRequest(bufferedWriter) // invalid request
        );

        // Close resources (important for releasing socket I/O streams)
        bufferedWriter.close();
        inputStream.close();
    }

    // Handles case when request cannot be parsed (400 Bad Request)
    private void handleInvalidRequest(final BufferedWriter bufferedWriter) {
        HttpResponse notFoundResponse = new HttpResponse.Builder()
                .setStatusCode(400)
                .setEntity("Bad Request...") // Response body
                .build();

        ResponseWriter.writeResponse(bufferedWriter, notFoundResponse);
    }

    // Handles valid request by checking if route exists
    private void handleRequest(final HttpRequest request, final BufferedWriter bufferedWriter) {
        // Construct key: HTTP_METHOD + URI path (ex: "GET/api/user")
        final String routeKey = request.getHttpMethod().name().concat(request.getUri().getRawPath());

        if (routes.containsKey(routeKey)) {
            // Valid route → delegate request to corresponding handler
            ResponseWriter.writeResponse(bufferedWriter, routes.get(routeKey).run(request));
        } else {
            // Route not found → return 404
            ResponseWriter.writeResponse(bufferedWriter,
                    new HttpResponse.Builder()
                            .setStatusCode(404)
                            .setEntity("Route Not Found....")
                            .build());
        }
    }
}