package com.battlesnake.starter;

public class GameDetails {

    private GameMode mode;

    public GameDetails(GameMode mode) {
        this.mode = mode;
    }

    public GameMode getMode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
    }
}
