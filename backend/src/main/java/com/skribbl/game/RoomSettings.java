package com.skribbl.game;

import java.util.ArrayList;
import java.util.List;

public class RoomSettings {

    private int maxPlayers = 8;     
    private int rounds = 3;        
    private int drawTime = 80;      
    private int wordCount = 3;     
    private int hints = 2;         
    private boolean publicRoom = false;
    private String language = "en";
    private List<String> categories = new ArrayList<>(); 

    public void clamp() {
        maxPlayers = clamp(maxPlayers, 2, 20);
        rounds = clamp(rounds, 2, 10);
        drawTime = clamp(drawTime, 15, 240);
        wordCount = clamp(wordCount, 1, 5);
        hints = clamp(hints, 0, 5);
        if (language == null || language.isBlank()) {
            language = "en";
        }
        if (categories == null) {
            categories = new ArrayList<>();
        }
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    public void copyFrom(RoomSettings other) {
        this.maxPlayers = other.maxPlayers;
        this.rounds = other.rounds;
        this.drawTime = other.drawTime;
        this.wordCount = other.wordCount;
        this.hints = other.hints;
        this.publicRoom = other.publicRoom;
        this.language = other.language;
        this.categories = other.categories;
        clamp();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getDrawTime() {
        return drawTime;
    }

    public void setDrawTime(int drawTime) {
        this.drawTime = drawTime;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public int getHints() {
        return hints;
    }

    public void setHints(int hints) {
        this.hints = hints;
    }

    public boolean isPublicRoom() {
        return publicRoom;
    }

    public void setPublicRoom(boolean publicRoom) {
        this.publicRoom = publicRoom;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
