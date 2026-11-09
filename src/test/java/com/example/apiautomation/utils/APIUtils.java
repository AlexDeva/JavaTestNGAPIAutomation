package com.example.apiautomation.utils;

import com.example.apiautomation.config.ConfigManager;
import com.example.apiautomation.reporting.ExtentReportManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.qameta.allure.Allure;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Provides generic HTTP operations with request and response reporting for API tests. */
public final class APIUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i).*(authorization|cookie|api[-_]?key|token|password|secret).*");

    private APIUtils() {
    }

    /** Sends a GET request with optional headers and query parameters and returns the raw response. */
    public static Response get(String endpoint, Map<String, ?> headers, Map<String, ?> queryParameters) {
        return execute("GET", endpoint, headers, queryParameters, null);
    }

    /** Sends a POST request with optional headers, query parameters, and a JSON payload. */
    public static Response post(String endpoint, Map<String, ?> headers, Map<String, ?> queryParameters, Object payload) {
        return execute("POST", endpoint, headers, queryParameters, payload);
    }

    /** Sends a PUT request with optional headers, query parameters, and a JSON payload. */
    public static Response put(String endpoint, Map<String, ?> headers, Map<String, ?> queryParameters, Object payload) {
        return execute("PUT", endpoint, headers, queryParameters, payload);
    }

    /** Sends a PATCH request with optional headers, query parameters, and a JSON payload. */
    public static Response patch(String endpoint, Map<String, ?> headers, Map<String, ?> queryParameters, Object payload) {
        return execute("PATCH", endpoint, headers, queryParameters, payload);
    }

    /** Sends a DELETE request with optional headers and query parameters and returns the raw response. */
    public static Response delete(String endpoint, Map<String, ?> headers, Map<String, ?> queryParameters) {
        return execute("DELETE", endpoint, headers, queryParameters, null);
    }

    /**
     * Executes an HTTP method with a JSON payload when present, attaches sanitized request/response details,
     * and returns the server response without imposing status-code assumptions.
     */
    public static Response execute(String method, String endpoint, Map<String, ?> headers,
                                   Map<String, ?> queryParameters, Object payload) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be blank");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Endpoint must not be blank");
        }

        String requestBody = serializePayload(payload);
        String stepName = method.toUpperCase() + " " + endpoint;
        return Allure.step(stepName, () -> {
            String requestSummary = buildRequestSummary(method, endpoint, headers, queryParameters, requestBody);
            Allure.addAttachment("Request " + UUID.randomUUID(), "text/plain", requestSummary);
            ExtentReportManager.logInfo(requestSummary);

            RequestSpecification request = RestAssured.given()
                    .baseUri(ConfigManager.getBaseUri())
                    .accept(ContentType.JSON);
            if (headers != null && !headers.isEmpty()) {
                request.headers(headers);
            }
            if (queryParameters != null && !queryParameters.isEmpty()) {
                request.queryParams(queryParameters);
            }
            if (payload != null) {
                request.contentType(ContentType.JSON).body(payload);
            }

            Response response = request.when().request(method.toUpperCase(), endpoint);
            String responseSummary = buildResponseSummary(response);
            Allure.addAttachment("Response " + UUID.randomUUID(), "text/plain", responseSummary);
            ExtentReportManager.logInfo(responseSummary);
            return response;
        });
    }

    /** Serializes a payload as JSON for safe request reporting and propagates serialization failures. */
    private static String serializePayload(Object payload) {
        if (payload == null) {
            return "";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize API request payload", exception);
        }
    }

    /** Builds a request summary while masking sensitive headers, query values, and JSON fields. */
    private static String buildRequestSummary(String method, String endpoint, Map<String, ?> headers,
                                             Map<String, ?> queryParameters, String body) {
        Map<String, Object> safeHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) ->
                    safeHeaders.put(name, SENSITIVE_VALUE.matcher(name).matches() ? "[REDACTED]" : value));
        }
        return "Method: " + method.toUpperCase()
                + "\nBase URI: " + ConfigManager.getBaseUri()
                + "\nEndpoint: " + endpoint
                + "\nHeaders: " + safeHeaders
                + "\nQuery parameters: " + sanitizeMap(queryParameters)
                + "\nPayload: " + sanitizeJson(body);
    }

    /** Builds a response summary with sensitive headers and JSON fields masked before reporting. */
    private static String buildResponseSummary(Response response) {
        Map<String, Object> safeHeaders = new LinkedHashMap<>();
        response.getHeaders().forEach(header -> safeHeaders.put(
                header.getName(),
                SENSITIVE_VALUE.matcher(header.getName()).matches() ? "[REDACTED]" : header.getValue()));
        return "Status: " + response.statusCode()
                + "\nHeaders: " + safeHeaders
                + "\nBody: " + sanitizeJson(response.asString());
    }

    /** Returns a copy of query parameters with values for sensitive keys replaced by a redaction marker. */
    private static Map<String, Object> sanitizeMap(Map<String, ?> values) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((name, value) -> sanitized.put(
                    name, SENSITIVE_VALUE.matcher(name).matches() ? "[REDACTED]" : value));
        }
        return sanitized;
    }

    /** Redacts sensitive JSON properties; non-JSON response bodies are retained as text. */
    private static String sanitizeJson(String json) {
        if (json == null || json.isBlank()) {
            return json == null ? "" : json;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            redactSensitiveFields(root);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return json;
        }
    }

    /** Recursively masks sensitive properties in JSON objects and arrays. */
    private static void redactSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_VALUE.matcher(field.getKey()).matches()) {
                    objectNode.put(field.getKey(), "[REDACTED]");
                } else {
                    redactSensitiveFields(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(APIUtils::redactSensitiveFields);
        }
    }
}
