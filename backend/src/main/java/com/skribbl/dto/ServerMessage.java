package com.skribbl.dto;

public record ServerMessage(String type, Object data, long ts) {

    public static ServerMessage of(String type, Object data) {
        return new ServerMessage(type, data, System.currentTimeMillis());
    }
}
