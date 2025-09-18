# Minimal Java HTTP Server

A lightweight, educational HTTP/1.1 server built from scratch in Java. It accepts TCP connections, decodes HTTP requests, routes them by method + path, and writes HTTP responses. Great for revising core networking, HTTP parsing, and server concurrency patterns.

## Features

- HTTP/1.1 request parsing (method, request-target, headers)
- Simple routing keyed by METHOD + path
- Thread-pooled, blocking I/O per connection
- Plain-text and HTML response examples
