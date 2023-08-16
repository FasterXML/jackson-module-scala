package com.fasterxml.jackson.module.scala.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record MRecord(String name, String domain) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MRecord fromString(String uri) {
        String[] parts = uri.split("@");
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException(String.format("Invalid SIP URI: %s", uri));
        }

        String[] usernameParts = parts[0].split("\\.");
            return new MRecord(usernameParts[usernameParts.length - 1], parts.length == 2 ? parts[1] : null);
        }

    @Override
    @JsonValue
    public String toString() {
        return String.format("%s@%s", name, domain);
    }
}
