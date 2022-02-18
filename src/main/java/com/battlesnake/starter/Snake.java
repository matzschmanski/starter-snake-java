package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * This is a simple Battlesnake server written in Java.
 *
 * For instructions see
 * https://github.com/BattlesnakeOfficial/starter-snake-java/README.md
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    private static final HashMap<String, char[][]> boards = new HashMap<>();



    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port == null) {
            LOG.info("Using default port: {}", port);
            port = "8080";
        } else {
            LOG.info("Found system provided port: {}", port);
        }
        port(Integer.parseInt(port));
        get("/", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the start/end request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();
        public static final String LEFT = "left";
        public static final String RIGHT = "right";
        public static final String DOWN = "down";
        public static final String UP = "up";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String BOARD = "board";
        public static final String ID = "id";
        public static final String HEIGHT = "height";
        public static final String WIDTH = "width";
        public static final String GAME = "game";
        public static final String YOU = "you";
        public static final String HEAD = "head";
        public static final String BODY = "body";
        public static final String SNAKES = "snakes";
        public static final String FOOD = "food";

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/")) {
                    snakeResponse = index();
                } else if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }

                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));

                return snakeResponse;
            } catch (JsonProcessingException e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         *
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @return a response back to the engine containing the Battlesnake setup
         *         values.
         */
        public Map<String, String> index() {
            Map<String, String> response = new HashMap<>();
            response.put("apiversion", "1");
            response.put("author", "geig006"); // TODO: Your Battlesnake Username
            response.put("color", "#cc33ff"); // TODO: Personalize
            response.put(HEAD, "sand-worm"); // TODO: Personalize
            response.put("tail", "skinny"); // TODO: Personalize
            return response;
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         *
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @param startRequest a JSON data map containing the information about the game
         *                     that is about to be played.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> start(JsonNode startRequest) {




            String id = startRequest.get(GAME).get(ID).asText();
            int height = startRequest.get(BOARD).get(HEIGHT).asInt();
            int width = startRequest.get(BOARD).get(WIDTH).asInt();
            LOG.info("Creating board '{}' with height: {} and width: {}",id, height,width);
            boards.put(id, createBoard(height, width));
            LOG.info("START");
            return EMPTY;
        }

        private char[][] createBoard(int height, int width){
            char[][] board = new char[height][width];

            return board;
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         *
         * Use the information in 'moveRequest' to decide your next move. The
         * 'moveRequest' variable can be interacted with as
         * com.fasterxml.jackson.databind.JsonNode, and contains all of the information
         * about the Battlesnake board for each move of the game.
         *
         * For a full example of 'json', see
         * https://docs.battlesnake.com/references/api/sample-move-request
         *
         * @param moveRequest JsonNode of all Game Board data as received from the
         *                    Battlesnake Engine.
         * @return a Map<String,String> response back to the engine the single move to
         *         make. One of "up", "down", "left" or "right".
         */
        static int mode = 0;
        public Map<String, String> move(JsonNode moveRequest) {

            try {
                LOG.debug("Data: {}", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(moveRequest));
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing payload", e);
            }

            /*
             * Example how to retrieve data from the request payload:
             *
             * String gameId = moveRequest.get("game").get("id").asText();
             *
             * int height = moveRequest.get(BOARD).get("height").asInt();
             *
             */

            JsonNode head = moveRequest.get(YOU).get(HEAD);
            JsonNode body = moveRequest.get(YOU).get(BODY);

            String gameId = moveRequest.get(GAME).get(ID).asText();


            JsonNode board = moveRequest.get(BOARD);
            int height = board.get(HEIGHT).asInt();
            int width = board.get(WIDTH).asInt();
            JsonNode snakes = board.get(SNAKES);
//            boardToArray(moveRequest);
            ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList(UP, DOWN, LEFT, RIGHT));
            Map<String, Point> nextPositions = generateNextPositions(head);
            // Don't allow your Battlesnake to move back in on it's own neck
            avoidMyNeck(head, body, possibleMoves);

            // TODO: Using information from 'moveRequest', find the edges of the board and
            // don't
            // let your Battlesnake move beyond them board_height = ? board_width = ?
            avoidWalls(head,height,width,possibleMoves);

            // TODO Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would hit its own body
            avoidSelf(head, body, possibleMoves, nextPositions);

            // TODO: Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would collide with another Battlesnake
            avoidOthers(snakes, possibleMoves, nextPositions);

            // TODO: Using information from 'moveRequest', make your Battlesnake move
            // towards a
            // piece of food on the board

            // Choose a random direction to move in
//            final int choice = new Random().nextInt(possibleMoves.size());
//            final String move = possibleMoves.get(choice);
            //aways go right, then always down

            //calculate mode
            if((head.get(Y).asInt() !=0 || head.get(X).asInt() != 0) && mode ==0){
                mode =0;
            }else{
                mode=1;
            }
            if(head.get(Y).asInt() ==0 && head.get(X).asInt()==0){
                mode=2;
            }

            String move = RIGHT;
            if(mode==0) {
                if (!possibleMoves.contains(RIGHT)) {
                    move = DOWN;
                    if (!possibleMoves.contains(DOWN)) {
                        //we are bottom right,
                        //start le snaking!
                        mode=1;
                    }
                }
            }


            if(mode == 1){
                //go left until x =0;
                move = LEFT;

            }
            if(mode ==2){
                //go up
                move= UP;
            }
            if( mode==3){
                //go right until length -2
                move = RIGHT;
            }
            if(mode==4){
                //go right once more
                move = RIGHT;
            }





            LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }
        private void moveToBottomRight(ArrayList<String> possibleMoves){
            if(possibleMoves.size()>1){
                //always go right, then go down
            }
        }

        /**
         * Remove the 'neck' direction from the list of possible moves
         *
         * @param head          JsonNode of the head position e.g. {X: 0, Y: 0}
         * @param body          JsonNode of x/y coordinates for every segment of a
         *                      Battlesnake. e.g. [ {X: 0, Y: 0}, {X: 1, Y: 0},
         *                      {X: 2, Y: 0} ]
         * @param possibleMoves ArrayList of String. Moves to pick from.
         */
        public void avoidMyNeck(JsonNode head, JsonNode body, ArrayList<String> possibleMoves) {
            JsonNode neck = body.get(1);

            if (neck.get(X).asInt() < head.get(X).asInt()) {
                possibleMoves.remove(LEFT);
            } else if (neck.get(X).asInt() > head.get(X).asInt()) {
                possibleMoves.remove(RIGHT);
            } else if (neck.get(Y).asInt() < head.get(Y).asInt()) {
                possibleMoves.remove(DOWN);
            } else if (neck.get(Y).asInt() > head.get(Y).asInt()) {
                possibleMoves.remove(UP);
            }
        }

        public void avoidWalls(JsonNode head, int boardHeight, int boardWidth, ArrayList<String> possibleMoves){
            if(head.get(X).asInt()+1 == boardHeight){
                possibleMoves.remove(RIGHT);
            }
            if(head.get(X).asInt()-1 <0){
                possibleMoves.remove(LEFT);
            }
            if(head.get(Y).asInt()+1 == boardWidth){
                possibleMoves.remove(UP);
            }
            if(head.get(Y).asInt()-1 <0){
                possibleMoves.remove(DOWN);
            }
        }
        public Map<String, Point> generateNextPositions(JsonNode head){
            int currentX = head.get(X).asInt();
            int currentY = head.get(Y).asInt();
            Point right = new Point(currentX+1,currentY);
            Point left = new Point(currentX-1,currentY);
            Point up = new Point(currentX,currentY+1);
            Point down = new Point(currentX,currentY-1);
            Map<String,Point> moves = Map.of(RIGHT,right,LEFT,left,DOWN,down,UP,up);
            return moves;
        }

        public void avoidOthers(JsonNode snakes ,ArrayList<String> possibleMoves, Map<String, Point> nextPositions){
            List<Point> noGoAreas = StreamSupport.stream(snakes.spliterator(),false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(),false)).map(coordinate -> new Point(coordinate.get(X).asInt(), coordinate.get(Y).asInt())).collect(Collectors.toList());
            for (Map.Entry<String, Point> pair : nextPositions.entrySet()) {
                if(noGoAreas.contains(pair.getValue())){
                    possibleMoves.remove(pair.getKey());
                }

            }

        }


        public void avoidSelf(JsonNode head, JsonNode body,ArrayList<String> possibleMoves, Map<String, Point> nextPositions){

            List<Point> noGoAreas = new ArrayList<>();
            noGoAreas.add(new Point(head.get(X).asInt(),head.get(Y).asInt()));
            for (final JsonNode objNode : body) {
                noGoAreas.add(new Point(objNode.get(X).asInt(),objNode.get(Y).asInt()));
            }

            for (Map.Entry<String, Point> pair : nextPositions.entrySet()) {
                if(noGoAreas.contains(pair.getValue())){
                    possibleMoves.remove(pair.getKey());
                }

            }

        }

        /**
         * This method is called when a game your Battlesnake was in ends.
         *
         * It is purely for informational purposes, you don't have to make any decisions
         * here.
         *
         * @param endRequest a map containing the JSON sent to this snake. Use this data
         *                   to know which game has ended
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            String gameId = endRequest.get("game").get("id").asText();
            boards.remove(gameId);
            LOG.info("ENDED ({})",gameId);
            return EMPTY;
        }




    }


}
