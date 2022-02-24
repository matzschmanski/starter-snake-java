package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.StreamSupport;

import static com.battlesnake.starter.SnakeAttribute.*;

public class Util {

    public static int[][] boardToArray(JsonNode moveRequest){
        JsonNode board = moveRequest.get("board");
        int height = board.get(HEIGHT).asInt();
        int width = board.get(WIDTH).asInt();
        int[][] boardArray = new int[height][width];
        //not reachable:
        //self, other snakes, walls
        StreamSupport.stream(board.get(SNAKES).spliterator(),false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(),false)).forEach(coordinate -> boardArray[coordinate.get(X).asInt()][coordinate.get(Y).asInt()] = 1024);

        // mark closest food as target
//        JsonNode food = board.get(FOOD);

//        StreamSupport.stream(board.get(FOOD).spliterator(),false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(),false)).forEach(coordinate -> boardArray[coordinate.get(X).asInt()][coordinate.get(Y).asInt()] = -1024);

        return boardArray;
    }
}
