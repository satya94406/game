package com.skribbl.game;

import com.skribbl.dto.Avatar;

public class Player {

    private final String id;
    private String name;
    private Avatar avatar;

    private int score;
    private boolean host;
    private boolean drawing;
    private boolean connected = true;

    public Player(String id, String name, Avatar avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar != null ? avatar : Avatar.defaultAvatar();
    }

    public void addScore(int points) {
        this.score += points;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isDrawing() {
        return drawing;
    }

    public void setDrawing(boolean drawing) {
        this.drawing = drawing;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
