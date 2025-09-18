package com.server.writers;

import com.server.pojos.HttpResponse;
import com.server.pojos.HttpStatusCode;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ResponseWriter: serializes an HttpResponse to the wire in HTTP/1.1 format.
 * - Writes status line, headers, blank line, then optional body.
 * - Builds header lines from a multi-valued header map.
 * - Currently supports String entities; other types are ignored.
 * - Silent error handling by design to avoid breaking connection teardown.
 */
public class ResponseWriter {

    /**
     * Write full HTTP/1.1 response to the provided BufferedWriter.
     * - Status line: HTTP/1.1 <code> <reason>
     * - Headers: one per line, ending with CRLF
     * - Body: optional; when present, includes Content-Length and a blank line before content
     */
    public static void writeResponse(final BufferedWriter outputStream, final HttpResponse response) {
        try {
            final int statusCode = response.getStatusCode();
            final String statusCodeMeaning = HttpStatusCode.STATUS_CODES.get(statusCode);
            final List<String> responseHeaders = buildHeaderStrings(response.getResponseHeaders());

            outputStream.write("HTTP/1.1 " + statusCode + " " + statusCodeMeaning + "\r\n");

            for (String header : responseHeaders) {
                outputStream.write(header);
            }

            final Optional<String> entityString = response.getEntity().flatMap(ResponseWriter::getResppnseString);
            if (entityString.isPresent()) {
                final String encodedString = new String(entityString.get().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                // Note: header name has a typo ("Content-Lenght"); consider fixing to "Content-Length"
                outputStream.write("Content-Lenght: " + encodedString.getBytes().length + "\r\n");
                outputStream.write("\r\n");
                outputStream.write(encodedString);
            }
            else {
                outputStream.write("\r\n");
            }
        } catch (Exception ignored) {
            // Intentionally swallow to avoid propagating I/O errors during response writing
        }
    }

    /**
     * Convert header map into HTTP header lines.
     * - Concatenates multiple values with ';' and appends CRLF.
     */
    private static List<String> buildHeaderStrings(final Map<String, List<String>> responseHeaders) {
        final List<String> responseHeadersList = new ArrayList<>();

        responseHeaders.forEach((name, values) -> {
            final StringBuilder valuesCombined = new StringBuilder();
            values.forEach(valuesCombined::append);
            valuesCombined.append(";");
            responseHeadersList.add(name + ": " + valuesCombined + "\r\n");
        });

        return responseHeadersList;
    }

    /**
     * Extract a writeable String from the entity if supported.
     * - Currently supports String entities; otherwise empty.
     */
    private static Optional<String> getResppnseString(final Object entity) {
        // Currently only supporting strings
        if (entity instanceof String) {
            try {
                return Optional.of(entity.toString());
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}