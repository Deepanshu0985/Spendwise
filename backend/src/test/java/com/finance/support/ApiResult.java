package com.finance.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpHeaders;

public record ApiResult(int status, HttpHeaders headers, JsonNode body) {

    public JsonNode data() {
        return body.get("data");
    }

    public JsonNode meta() {
        return body.get("meta");
    }

    public JsonNode error() {
        return body.get("error");
    }

    public String errorCode() {
        JsonNode error = error();
        return error == null ? null : error.get("code").asText();
    }
}
