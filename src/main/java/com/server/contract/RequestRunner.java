package com.server.contract;


import com.server.pojos.HttpRequest;
import com.server.pojos.HttpResponse;

/**
 * RequestRunner: functional contract for handling an HTTP request.
 * - Accepts a parsed HttpRequest and returns a constructed HttpResponse.
 * - Enables pluggable route handlers that encapsulate business logic per endpoint.
 */
public interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
