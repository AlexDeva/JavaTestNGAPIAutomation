package com.example.apiautomation.tests;

import com.example.apiautomation.base.BaseTest;
import com.example.apiautomation.model.ResourcePayload;
import com.example.apiautomation.utils.APIUtils;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;

@Epic("API Automation")
@Feature("Resource API")
public class ResourceApiTests extends BaseTest {

    /** Validates successful resource creation, response schema, and returned fields. */
    @Test(description = "Create a resource and validate its response contract")
    @Story("Create resource")
    @Description("Posts a resource, verifies HTTP 201, validates the JSON schema, and checks returned data.")
    public void createResourceReturnsCreatedResource() {
        ResourcePayload payload = new ResourcePayload("Created resource", "Created body", 1);
        Response response = APIUtils.post("/posts", Map.of(), Map.of(), payload);

        assertEquals(response.statusCode(), 201);
        response.then().body(matchesJsonSchemaInClasspath("schemas/resource.schema.json"));
        response.then().body("id", equalTo(101))
                .body("title", equalTo(payload.title()))
                .body("body", equalTo(payload.body()));
    }

    /** Supplies independent resource IDs to the concurrent GET contract test. */
    @DataProvider(name = "existingPosts", parallel = true)
    public Object[][] existingPosts() {
        return new Object[][]{
                {1, "Existing resource"},
                {2, "Second resource"}
        };
    }

    /** Validates successful retrieval, JSON response headers, and path-extracted resource fields. */
    @Test(dataProvider = "existingPosts", description = "Retrieve existing resources concurrently")
    @Story("Read resource")
    @Description("Gets existing resources and verifies their status, content type, ID, and title.")
    public void getResourceReturnsExpectedData(int id, String expectedTitle) {
        Response response = APIUtils.get("/posts/" + id, Map.of(), Map.of());

        assertEquals(response.statusCode(), 200);
        assertThat(response.getHeader("Content-Type"), containsString("application/json"));
        assertEquals(response.jsonPath().getInt("id"), id);
        assertEquals(response.jsonPath().getString("title"), expectedTitle);
    }

    /** Validates full resource replacement with PUT. */
    @Test(description = "Replace a resource with PUT")
    @Story("Update resource")
    @Description("Sends a complete replacement payload and verifies the updated response fields.")
    public void putResourceUpdatesResource() {
        ResourcePayload payload = new ResourcePayload("Updated resource", "Updated body", 1);
        Response response = APIUtils.put("/posts/1", Map.of(), Map.of(), payload);

        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("title"), payload.title());
        assertEquals(response.jsonPath().getString("body"), payload.body());
    }

    /** Validates partial resource modification with PATCH. */
    @Test(description = "Partially update a resource with PATCH")
    @Story("Update resource")
    @Description("Sends a partial update and verifies the modified title.")
    public void patchResourceUpdatesSelectedFields() {
        Response response = APIUtils.patch("/posts/1", Map.of(), Map.of(), Map.of("title", "Patched resource"));

        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("title"), "Patched resource");
        assertEquals(response.jsonPath().getString("body"), "Original body");
    }

    /** Validates resource deletion and confirms the deleted resource is no longer available. */
    @Test(description = "Delete a resource and verify it no longer exists")
    @Story("Delete resource")
    @Description("Deletes resource 99, checks the no-content response, then verifies a 404 lookup.")
    public void deleteResourceReturnsNoContentAndThenNotFound() {
        Response deleteResponse = APIUtils.delete("/posts/99", Map.of(), Map.of());
        assertEquals(deleteResponse.statusCode(), 204);

        Response lookupResponse = APIUtils.get("/posts/99", Map.of(), Map.of());
        assertEquals(lookupResponse.statusCode(), 404);
    }

    /** Validates a malformed resource lookup is rejected with HTTP 400. */
    @Test(description = "Reject an invalid resource request")
    @Story("Negative scenarios")
    @Description("Requests an invalid resource identifier and verifies HTTP 400.")
    public void invalidResourceReturnsBadRequest() {
        Response response = APIUtils.get("/posts/invalid", Map.of(), Map.of());
        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("error"), "Invalid resource identifier");
    }

    /** Validates an unauthenticated protected-resource request is rejected with HTTP 401. */
    @Test(description = "Reject an unauthenticated request")
    @Story("Negative scenarios")
    @Description("Requests a protected resource without credentials and verifies HTTP 401.")
    public void protectedResourceReturnsUnauthorized() {
        Response response = APIUtils.get("/protected", Map.of(), Map.of());
        assertEquals(response.statusCode(), 401);
        assertEquals(response.jsonPath().getString("error"), "Unauthorized");
    }
}
