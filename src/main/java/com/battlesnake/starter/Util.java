package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.stream.StreamSupport;

import static com.battlesnake.starter.SnakeAttribute.*;

public class Util {

    public static final char CAN_NOT_TRAVEL ='0';
    public static final char CAN_TRAVEL ='*';
    public static final char SOURCE ='s';
    public static final char DESTINATION ='d';
    public static char[][] boardToArray(JsonNode moveRequest){
        JsonNode board = moveRequest.get("board");
        int height = board.get(HEIGHT).asInt();
        int width = board.get(WIDTH).asInt();
        char[][] boardArray = new char[height][width];

        for (char[] chars : boardArray) {
            Arrays.fill(chars, CAN_TRAVEL);
        }
        JsonNode head = moveRequest.get(YOU).get(HEAD);
        //not reachable:
        //self, other snakes, walls
        StreamSupport.stream(board.get(SNAKES).spliterator(),false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(),false)).forEach(coordinate -> boardArray[snakeToBoard(boardArray,coordinate.get(Y).asInt())][snakeToBoard(boardArray,coordinate.get(X).asInt())] = CAN_NOT_TRAVEL);

        boardArray[boardArray.length-1-head.get(X).asInt()][head.get(Y).asInt()] = SOURCE;

        // mark closest food as target
//        JsonNode food = board.get(FOOD);

//        StreamSupport.stream(board.get(FOOD).spliterator(),false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(),false)).forEach(coordinate -> boardArray[coordinate.get(X).asInt()][coordinate.get(Y).asInt()] = -1024);

        return boardArray;
    }

    public static int snakeToBoard(char[][] board, int value){
        return board.length-1-value;
    }
}
