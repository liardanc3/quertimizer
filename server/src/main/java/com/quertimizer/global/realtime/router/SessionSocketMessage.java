package com.quertimizer.global.realtime.router;

import com.fasterxml.jackson.databind.JsonNode;

public record SessionSocketMessage(String type, JsonNode payload) {
}
