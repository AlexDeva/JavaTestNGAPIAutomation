package com.example.apiautomation.support;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/** Provides deterministic API responses so the example suite runs without external services. */
public final class TestApiServer {
    private static WireMockServer server;

    private TestApiServer() {
    }

    /** Starts WireMock on an available port and registers the sample resource API routes. */
    public static synchronized void start() {
        if (server != null && server.isRunning()) {
            return;
        }
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        registerRoutes();
    }

    /** Returns the active server's base URI or fails if the server has not been started. */
    public static synchronized String getBaseUri() {
        if (server == null || !server.isRunning()) {
            throw new IllegalStateException("The local test API server is not running");
        }
        return server.baseUrl();
    }

    /** Stops the local server and releases its listening port. */
    public static synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private static void registerRoutes() {
        server.stubFor(post(urlEqualTo("/posts"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":101,\"title\":\"Created resource\",\"body\":\"Created body\",\"userId\":1}")));

        server.stubFor(get(urlEqualTo("/posts/1"))
                .willReturn(jsonResponse("{\"id\":1,\"title\":\"Existing resource\",\"body\":\"Original body\",\"userId\":1}", 200)));
        server.stubFor(get(urlEqualTo("/posts/2"))
                .willReturn(jsonResponse("{\"id\":2,\"title\":\"Second resource\",\"body\":\"Second body\",\"userId\":2}", 200)));

        server.stubFor(put(urlEqualTo("/posts/1"))
                .willReturn(jsonResponse("{\"id\":1,\"title\":\"Updated resource\",\"body\":\"Updated body\",\"userId\":1}", 200)));
        server.stubFor(patch(urlEqualTo("/posts/1"))
                .willReturn(jsonResponse("{\"id\":1,\"title\":\"Patched resource\",\"body\":\"Original body\",\"userId\":1}", 200)));

        server.stubFor(get(urlEqualTo("/posts/99"))
                .inScenario("delete-resource-99")
                .whenScenarioStateIs(STARTED)
                .willReturn(jsonResponse("{\"id\":99,\"title\":\"Delete me\",\"body\":\"Temporary resource\",\"userId\":1}", 200)));
        server.stubFor(delete(urlEqualTo("/posts/99"))
                .inScenario("delete-resource-99")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("DELETED")
                .willReturn(aResponse().withStatus(204)));
        server.stubFor(get(urlEqualTo("/posts/99"))
                .inScenario("delete-resource-99")
                .whenScenarioStateIs("DELETED")
                .willReturn(jsonResponse("{\"message\":\"Resource not found\"}", 404)));

        server.stubFor(get(urlEqualTo("/posts/invalid"))
                .willReturn(jsonResponse("{\"error\":\"Invalid resource identifier\"}", 400)));
        server.stubFor(get(urlEqualTo("/protected"))
                .willReturn(jsonResponse("{\"error\":\"Unauthorized\"}", 401)));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonResponse(
            String body, int status) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
    }
}
