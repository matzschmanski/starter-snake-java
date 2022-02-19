package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

public class Point {
    int x,y;

    public Point(JsonNode p) {
        y = p.get("y").asInt();
        x = p.get("x").asInt();
    }

    public String toString(){
        return x+"|"+y;
    }
}
