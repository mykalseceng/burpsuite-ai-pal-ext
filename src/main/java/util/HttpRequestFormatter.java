package util;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;

public class HttpRequestFormatter {

    private static final int MAX_BODY_LENGTH = 10000;

    public static String format(HttpRequestResponse reqRes) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== HTTP Request ===\n");
        sb.append(formatRequest(reqRes.request()));

        if (reqRes.response() != null) {
            sb.append("\n\n=== HTTP Response ===\n");
            sb.append(formatResponse(reqRes.response()));
        }

        return sb.toString();
    }

    public static String formatRequest(HttpRequest request) {
        StringBuilder sb = new StringBuilder();

        // Request line
        sb.append(request.method()).append(" ");
        sb.append(request.path()).append(" ");
        sb.append("HTTP/").append(request.httpVersion()).append("\n");

        // Headers
        request.headers().forEach(header ->
                sb.append(header.name()).append(": ").append(header.value()).append("\n")
        );

        // Body
        String body = request.bodyToString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n");
            if (body.length() > MAX_BODY_LENGTH) {
                sb.append(body, 0, MAX_BODY_LENGTH);
                sb.append("\n... [truncated, ").append(body.length() - MAX_BODY_LENGTH).append(" more bytes]");
            } else {
                sb.append(body);
            }
        }

        return sb.toString();
    }

    public static String formatResponse(HttpResponse response) {
        StringBuilder sb = new StringBuilder();

        // Status line
        sb.append("HTTP/").append(response.httpVersion()).append(" ");
        sb.append(response.statusCode()).append(" ");
        sb.append(response.reasonPhrase()).append("\n");

        // Headers
        response.headers().forEach(header ->
                sb.append(header.name()).append(": ").append(header.value()).append("\n")
        );

        // Body
        String body = response.bodyToString();
        if (body != null && !body.isEmpty()) {
            sb.append("\n");
            if (body.length() > MAX_BODY_LENGTH) {
                sb.append(body, 0, MAX_BODY_LENGTH);
                sb.append("\n... [truncated, ").append(body.length() - MAX_BODY_LENGTH).append(" more bytes]");
            } else {
                sb.append(body);
            }
        }

        return sb.toString();
    }

    public static String formatCompact(HttpRequestResponse reqRes) {
        StringBuilder sb = new StringBuilder();

        HttpRequest request = reqRes.request();
        sb.append(request.method()).append(" ").append(request.path()).append("\n");
        sb.append("Host: ").append(request.headerValue("Host")).append("\n");

        String contentType = request.headerValue("Content-Type");
        if (contentType != null) {
            sb.append("Content-Type: ").append(contentType).append("\n");
        }

        String body = request.bodyToString();
        if (body != null && !body.isEmpty()) {
            sb.append("\nRequest Body:\n");
            if (body.length() > 500) {
                sb.append(body, 0, 500).append("...");
            } else {
                sb.append(body);
            }
        }

        if (reqRes.response() != null) {
            sb.append("\n\nResponse: ").append(reqRes.response().statusCode());
            sb.append(" ").append(reqRes.response().reasonPhrase());
        }

        return sb.toString();
    }

    public static String getRequestSummary(HttpRequestResponse reqRes) {
        HttpRequest request = reqRes.request();
        String host = request.headerValue("Host");
        if (host == null) {
            host = request.httpService().host();
        }
        return request.method() + " " + host + request.path();
    }

    // Overloaded methods for ProxyHttpRequestResponse
    public static String format(ProxyHttpRequestResponse reqRes) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== HTTP Request ===\n");
        sb.append(formatRequest(reqRes.request()));

        if (reqRes.response() != null) {
            sb.append("\n\n=== HTTP Response ===\n");
            sb.append(formatResponse(reqRes.response()));
        }

        return sb.toString();
    }

    public static String formatCompact(ProxyHttpRequestResponse reqRes) {
        StringBuilder sb = new StringBuilder();

        HttpRequest request = reqRes.request();
        sb.append(request.method()).append(" ").append(request.path()).append("\n");
        sb.append("Host: ").append(request.headerValue("Host")).append("\n");

        String contentType = request.headerValue("Content-Type");
        if (contentType != null) {
            sb.append("Content-Type: ").append(contentType).append("\n");
        }

        String body = request.bodyToString();
        if (body != null && !body.isEmpty()) {
            sb.append("\nRequest Body:\n");
            if (body.length() > 500) {
                sb.append(body, 0, 500).append("...");
            } else {
                sb.append(body);
            }
        }

        if (reqRes.response() != null) {
            sb.append("\n\nResponse: ").append(reqRes.response().statusCode());
            sb.append(" ").append(reqRes.response().reasonPhrase());
        }

        return sb.toString();
    }
}