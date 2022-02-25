package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class GameLibrary {
    private static final GameLibrary INSTANCE = new GameLibrary();
    Cache<String, JsonNode> cache;


    private GameLibrary() {
        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .build();
    }

    public static GameLibrary getInstance() {
        return INSTANCE;
    }

    public JsonNode getGame(String gameId) {
        return cache.getIfPresent(gameId);
    }

    public void updateGame(String gameId, JsonNode moveRequest){
        cache.put(gameId,moveRequest);
    }

}
