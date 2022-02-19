package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static class P {
        int x,y;

        public P(JsonNode p) {
            y = p.get("y").asInt();
            x = p.get("x").asInt();
        }

        public String toString(){
            return x+"|"+y;
        }
    }

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port == null) {
            LOG.info("Using default port: {}", port);
            port = "9191";
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
                //LOG.info("{} called with: {}", uri, req.body());
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
            response.put("author", "marq24");
            response.put("color", "#33FF33");
            response.put("head", "default"); // TODO: Personalize
            response.put("tail", "default"); // TODO: Personalize
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
            LOG.info("START");

            state = 0;
            tPhase = 0;
            X = -1;
            Y = -1;
            usedPlaces = null;
            patched = false;
            stateToRestore = -1;
            return EMPTY;
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


        private static final int UP = 0;
        private static final int RIGHT = 1;
        private static final int DOWN = 2;
        private static final int LEFT = 3;

        private static final String U = "up";
        private static final String D = "down";
        private static final String L = "left";
        private static final String R = "right";

        public int[][] usedPlaces;
        public ArrayList<P>foodPlaces;
        public int X,Y, Xmin, Ymin, Xmax, Ymax;
        int state = 0;
        boolean patched = false;
        int stateToRestore = -1;

        int tPhase = 0;

        public Map<String, String> move(JsonNode moveRequest) {
            JsonNode board = moveRequest.get("board");
            if(X == -1) {
                Y = board.get("height").asInt();
                X = board.get("width").asInt();
                Ymin = 0;
                Xmin = 0;
                Ymax = Y-1;
                Xmax = X-1;
            }

            // clearing the used fields...
            usedPlaces = new int[Y][X];
            foodPlaces = new ArrayList<>();

            // get the locations of all snakes...
            JsonNode snakes = board.get("snakes");
            int slen = snakes.size();
            for (int i = 0; i < slen; i++) {
                JsonNode body = snakes.get(i).get("body");
                int len = body.size();
                for (int j = 0; j < len; j++) {
                    P p = new P(body.get(j));
                    usedPlaces[p.y][p.x] = 1;
                }
            }

            JsonNode haz = board.get("hazards");
            if(haz != null){
                int hlen = haz.size();
                for (int i = 0; i < hlen; i++) {
                    P p = new P(haz.get(i));
                    usedPlaces[p.y][p.x] = 1;
                }
            }

            JsonNode food = board.get("food");
            if(food != null) {
                int flen = food.size();
                for (int i = 0; i < flen; i++) {
                    foodPlaces.add(new P(food.get(i)));
                }
            }

            String move = D;
            JsonNode you = moveRequest.get("you");
            int health = you.get("health").asInt();
            JsonNode body = you.get("body");
            int len = body.size();
            for (int i=1; i<len; i++){
                P p = new P(body.get(i));
                usedPlaces[p.y][p.x] = 1;
            }
            JsonNode head = you.get("head");
            P pos = new P(head);

            /*if(!patched) {
                if(Math.random() * 20 > 17) {
                    patched = true;
                    int addon1 = (int) (Math.random() * 3);
                    int addon2 = (int) (Math.random() * 3);
                    Ymin = addon1;
                    Xmin = addon2;
                    Ymax = Y - (1+addon1);
                    Xmax = X - (1+addon2);
                }
            }*/

//            boolean huntForFood = false;
//            if(health == 100){
//                // ok back on previous track!
//                Ymin = 0;
//                Xmin = 0;
//                Ymax = Y-1;
//                Xmax = X-1;
//
//                // after we got our food we need to find the closest wall (again?)
//                // 1 | 2
//                // -----
//                // 3 | 4
//                /*if( pos.x > Xmax/2 && pos.y > Ymax/2){
//                    // -> we are IN Q2
//                    if(pos.x > pos.y){
//                        // we should move RIGHT
//                        state = 1;
//                    }else{
//                        // we should move UP...
//                        state = 0;
//                    }
//                } else if( pos.x <= Xmax/2 && pos.y <= Ymax/2){
//                    // -> we are IN Q3
//                    if(pos.x > pos.y){
//                        // we should move LEFT
//                        state = 3;
//                    }else{
//                        // we should move DOWN...
//                        state = 2;
//                    }
//                } else if(pos.x <= Xmax/2){
//                    // we are in Q1
//                    state = 1;
//                }else{
//                    // we are in Q4
//                    state = 3;
//                }*/
//                if(pos.x <= Xmax/2){
//                    state = LEFT;
//                }else if( pos.y <= Ymax/2){
//                    state = DOWN;
//                }else if( pos.x > pos.y ) {
//                    state = RIGHT;
//                }else{
//                    state = UP;
//                }
//
//                //state = stateToRestore;
//
//            } else if(health < 50){
//                P closestFood = null;
//                int minDist = Integer.MAX_VALUE;
//                for(P f: foodPlaces){
//                    int dist = Math.abs( f.x - pos.x) + Math.abs( f.y - pos.y);
//                    minDist = Math.min(minDist, dist);
//                    if(minDist == dist){
//                        closestFood = f;
//                    }
//                }
//
//                if(closestFood != null){
//                    Ymin = closestFood.y;
//                    Xmin = closestFood.x;
//                    Ymax = closestFood.y;
//                    Xmax = closestFood.x;
//                    stateToRestore = state;
//                    huntForFood = true;
//                    LOG.info("GOTO: -> "+closestFood);
//                }
//            }


            boolean huntForFood = false;
            switch (state){
                case UP:
                    move = moveUp(pos, !huntForFood);
                    //move = moveUpOrRight(pos);
                    break;
                case RIGHT:
                    move = moveRight(pos, !huntForFood);
                    break;
                case DOWN:
                    move = moveDown(pos, !huntForFood);
                    //move = moveDownOrLeft(pos);
                    break;
                case LEFT:
                    move = moveLeft(pos, !huntForFood);
                    break;
            }

            /*int rand = (int) (Math.random() * 20);
            if(rand > 18){
                if(move.equals(U) && pos[0] > 2 && pos[0] < Y-3 ){
                    move = R;
                } else if(move.equals(L) && pos[1] > 2 && pos[1] < X-3 ){
                    move = U;
                } else if(move.equals(D) && pos[0] > 2 && pos[0] < Y-3 ){
                    move = L;
                } else if(move.equals(R) && pos[1] > 2 && pos[1] < X-3 ){
                    move = D;
                }
            }*/

            //JsonNode head = moveRequest.get("you").get("head");
            //JsonNode body = moveRequest.get("you").get("body");


            // Don't allow your Battlesnake to move back in on it's own neck
            //avoidMyNeck(head, body, possibleMoves);

            // TODO: Using information from 'moveRequest', find the edges of the board and
            // don't
            // let your Battlesnake move beyond them board_height = ? board_width = ?

            // TODO Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would hit its own body

            // TODO: Using information from 'moveRequest', don't let your Battlesnake pick a
            // move
            // that would collide with another Battlesnake

            // TODO: Using information from 'moveRequest', make your Battlesnake move
            // towards a
            // piece of food on the board

            // Choose a random direction to move in
            //final int choice = new Random().nextInt(possibleMoves.size());
            //final String move = possibleMoves.get(choice);

            //LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }

        private String moveUpOrRight(P pos) {
            if (pos.y < Ymax && usedPlaces[pos.y + 1][pos.x] == 0) {
                return U;
            } else if (pos.x < Xmax && usedPlaces[pos.y][pos.x + 1] == 0) {
                return R;
            } else {
                state = DOWN;
                patched = false;
                Ymax = Y - 1;
                Xmax = X - 1;
                return moveDownOrLeft(pos);
            }
        }

        private String moveDownOrLeft(P pos) {
            if (pos.y > Ymin && usedPlaces[pos.y - 1][pos.x] == 0) {
                return D;
            } else if (pos.x > Xmin && usedPlaces[pos.y][pos.x - 1] == 0) {
                return L;
            } else {
                state = UP;
                patched = false;
                Ymin = 0;
                Xmin = 0;
                return moveUpOrRight(pos);
            }
        }

        private String moveUp(P pos, boolean reset) {
            if (pos.y < Ymax && usedPlaces[pos.y + 1][pos.x] == 0) {
                return U;
            }else{
                state = RIGHT;
                if(reset) {
                    patched = false;
                    Ymax = Y - 1;
                    Xmax = X - 1;
                }
                return moveRight(pos, reset);
            }
        }

        private String moveRight(P pos, boolean reset) {
            if (pos.x < Xmax && usedPlaces[pos.y][pos.x + 1] == 0) {
                return R;
            } else {
                if(pos.x == Xmax && tPhase == 1){
                    state = LEFT;
                    // TODO check if we can MOVE UP?!
                    return U;
                } else {
                    state = DOWN;
                    if(reset) {
                        patched = false;
                        Ymax = Y - 1;
                        Xmax = X - 1;
                    }
                    return moveDown(pos, reset);
                }
            }
        }

        private String moveDown(P pos, boolean reset) {
            if (pos.y > Ymin && usedPlaces[pos.y - 1][pos.x] == 0) {
                if(tPhase == 1 && pos.y == 1){
                    state = RIGHT;
                    // TODO check if we can MOVE RIGHT?!
                    return R;
                }else {
                    return D;
                }
            } else {
                state = LEFT;
                if(reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveLeft(pos, reset);
            }
        }

        private String moveLeft(P pos, boolean reset) {
            if (pos.x > Xmin && (tPhase==1 || usedPlaces[pos.y][pos.x - 1] == 0)) {
                if(pos.x == 1){
                    if(pos.y == Ymax){
                        tPhase = 1;
                        state = DOWN;
                        // TODO check if we can MOVE LEFT
                        return L;
                    }else {
                        tPhase = 1;
                        state = RIGHT;
                        // TODO cehck if we can MOVE UP
                        return U;
                    }
                } else {
                    return L;
                }
            } else {
                state = UP;
                if(reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveUp(pos, reset);
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
            LOG.info("END");
            return EMPTY;
        }
    }

}
