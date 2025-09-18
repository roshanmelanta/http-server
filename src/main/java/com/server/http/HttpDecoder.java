package com.server.http;

import com.server.contract.HttpMethod;
import com.server.pojos.HttpRequest;
import com.server.pojos.HttpRequest.Builder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * HttpDecoder: parses an HTTP/1.1 request from a socket InputStream into HttpRequest.
 * - Expects start-line format: "<METHOD> <request-target> HTTP/1.1" per spec. [RFC 7230/9112]
 * - Splits message into lines, validates version, and extracts method + URI.
 * - Parses headers into a multi-valued map; body parsing is not implemented.
 * - Returns Optional.empty() for malformed or unsupported requests.
 */
public class HttpDecoder {

    /**
     * Decode a request from the input stream to a typed HttpRequest, if possible.
     */
    public static Optional<HttpRequest> decode(final InputStream inputStream) {
        return readMessage(inputStream).flatMap(HttpDecoder::buildRequest);
    }

    /**
     * Read raw request bytes as lines.
     * - Reads available bytes, splits on newline into a list of lines.
     * - Returns empty if no bytes are available or an error occurs.
     */
    private static Optional<List<String>> readMessage(final InputStream inputStream) {
        try {
            if (!(inputStream.available() > 0)) {
                return Optional.empty();
            }

            final char[] inBuffer = new char[inputStream.available()];
            final InputStreamReader inReader = new InputStreamReader(inputStream);
            final int read = inReader.read(inBuffer);

            List<String> message = new ArrayList<>();

            try (Scanner sc = new Scanner(new String(inBuffer))) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    message.add(line);
                }
            }
            System.out.println("-----------Character Array Buffer-----------");
            for (int i = 0; i < inBuffer.length; i++) {
                System.out.print(inBuffer[i]);
            }
            System.out.println("\n");
            System.out.println("-----------Message-----------");
            System.out.println(message);
            System.out.println("\n");
            return Optional.of(message);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Build a typed HttpRequest from message lines.
     * - Validates request-line: method, target, and "HTTP/1.1".
     * - Populates method, URI, and headers; body not parsed.
     */
    private static Optional<HttpRequest> buildRequest(List<String> message) {
        if (message.isEmpty()) {
            return Optional.empty();
        }

        String firstLine = message.get(0);
        String[] httpInfo = firstLine.split(" ");

        if (httpInfo.length != 3) {
            return Optional.empty();
        }

        String protocolVersion = httpInfo[2];
        if (!protocolVersion.equals("HTTP/1.1")) {
            return Optional.empty();
        }

        try {
            Builder requestBuilder = new Builder();
            requestBuilder.setHttpMethod(HttpMethod.valueOf(httpInfo[0]));
            requestBuilder.setUri(new URI(httpInfo[1]));
            return Optional.of(addRequestHeader(message, requestBuilder));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Parse header lines into a multi-valued map and finalize the request.
     * - Stops parsing when a non-header line is encountered.
     */
    private static HttpRequest addRequestHeader(final List<String> message, Builder builder) {
        final Map<String, List<String>> requestHeaders = new HashMap<>();

        if (message.size() > 1) {
            for (int i = 1; i < message.size(); i++) {
                String header = message.get(i);
                System.out.println("-----------Header-----------");
                System.out.println(header);
                System.out.println("\n");
                int colonIndex = header.indexOf(":");

                if (!(colonIndex > 0 && header.length() > colonIndex + 1)) {
                    break;
                }

                String headerName = header.substring(0, colonIndex);
                String headerValue = header.substring(colonIndex + 1);

                requestHeaders.compute(headerName, (key, values) -> {
                    if (values != null) {
                        values.add(headerValue);
                    } else {
                        values = new ArrayList<>();
                    }
                    return values;
                });
            }
        }
        System.out.println("-----------Request Headers-----------");
        System.out.println(requestHeaders);
        System.out.println("\n");
        builder.setRequestHeaders(requestHeaders);
        return builder.build();
    }
}