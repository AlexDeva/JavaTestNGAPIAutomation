package com.example.apiautomation.model;

/** Immutable request model serialized by Jackson for resource API operations. */
public record ResourcePayload(String title, String body, int userId) {
}
