package com.battlesnake.starter;

import java.util.HashMap;

public class GameLibrary {
    private static final GameLibrary INSTANCE = new GameLibrary();
    private final HashMap<String, GameDetails> games;



    private GameLibrary() {
        games = new HashMap<>();
    }

    public static GameLibrary getInstance() {
        return INSTANCE;
    }

    public HashMap<String, GameDetails> getGames() {
        return games;
    }

    public static GameMode getMode(String gameId){
        return getInstance().getGames().getOrDefault(gameId, new GameDetails(GameMode.GO_DOWN)).getMode();
    }
}
