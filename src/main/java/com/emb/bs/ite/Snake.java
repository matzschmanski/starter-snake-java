package com.emb.bs.ite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

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

    static final int UP = 0;
    static final int RIGHT = 1;
    static final int DOWN = 2;
    static final int LEFT = 3;

    static final String REPEATLAST = "repeat";

    static final String U = "up";
    static final String D = "down";
    static final String L = "left";
    static final String R = "right";

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
        private HashMap<String, Session> sessions = new HashMap();

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
                Map<String, String> snakeResponse;
                if (uri.equals("/")) {
                    snakeResponse = index();
                } else if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    //LOG.info("{} called with: {}", uri, req.body());
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                //LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
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
            //response.put("color", "#FF1111");
            // https://play.battlesnake.com/references/customizations/
            response.put("head", "sand-worm"); // TODO: Personalize
            response.put("tail", "block-bum"); // TODO: Personalize
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
            sessions.put(startRequest.get("game").get("id").asText(), new Session());
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

        public Map<String, String> move(JsonNode moveRequest) {
            JsonNode game = moveRequest.get("game");
            String sessId = game.get("id").asText();
            Session s = sessions.get(sessId);
            if(s != null) {
                String gameType = null;
                if(game.has("ruleset")){
                    gameType = game.get("ruleset").get("name").asText().toLowerCase();
                }
                readCurrentBoardStatusIntoSession(moveRequest, gameType, s);

                String move = calculateNextMove(s);
                if(move.equals(REPEATLAST)){
                    // OK we are DOOMED anyhow - so we can do what ever
                    // we want -> so we just repeat the last move...
                    move = s.LASTMOVE;
                    if(move == null){
                        // WTF?!
                        move = D;
                    }
                }else{
                    s.LASTMOVE = move;
                }

                LOG.info("=> RESULTING MOVE: "+move+" ["+s.state+"]");
                Map<String, String> response = new HashMap<>();
                response.put("move", move);
                return response;
            }else{
                // session is null ?!
                LOG.error("SESSION was not available?! -> "+sessId+" could not be found in: "+sessions);
                Map<String, String> response = new HashMap<>();
                response.put("move", R);
                return response;
            }
        }

        private void readCurrentBoardStatusIntoSession(JsonNode moveRequest, String rulesetName, Session s) {
            s.turn = moveRequest.get("turn").asInt();
            JsonNode board = moveRequest.get("board");

            // clearing the used session fields...
            s.initSessionForTurn(rulesetName, board.get("height").asInt(), board.get("width").asInt());

            // get OWN SnakeID
            JsonNode you = moveRequest.get("you");
            String myId = you.get("id").asText();

            s.myHealth = you.get("health").asInt();
            s.myLen = you.get("length").asInt();

            JsonNode head = you.get("head");
            s.myPos = new Point(head);
            // adding also myHead to the boddy array (to allow
            // simple NoGoZone-Detection
            s.myBody[s.myPos.y][s.myPos.x] = s.myLen;

            JsonNode myBody = you.get("body");
            int myBodyLen = myBody.size();
            for (int i = 1; i < myBodyLen; i++) {
                Point p = new Point(myBody.get(i));
                s.myBody[p.y][p.x] = 1;
            }

            // reading about available food...
            JsonNode food = board.get("food");
            if (food != null) {
                int fLen = food.size();
                for (int i = 0; i < fLen; i++) {
                    s.foodPlaces.add(new Point(food.get(i)));
                }
            }

            // get the locations of all snakes...
            JsonNode snakes = board.get("snakes");
            int sLen = snakes.size();
            for (int i = 0; i < sLen; i++) {
                JsonNode aSnake = snakes.get(i);
                if (!aSnake.get("id").asText().equals(myId)) {
                    String fof = aSnake.get("name").asText().toLowerCase();
                    if(!s.hungerMode && checkFoF(fof)){
                        s.hungerMode = true;
                    }
                    int len = aSnake.get("length").asInt();
                    s.maxOtherSnakeLen = Math.max(len, s.maxOtherSnakeLen);
                    Point h = new Point(aSnake.get("head"));
                    s.snakeBodies[h.y][h.x] = len;
                    s.snakeHeads.add(h);

                    //boolean isFoodReachableForSnake = false;
                    try {
                        if (s.snakeBodies[h.y - 1][h.x] == 0) {
                            s.snakeNextMovePossibleLocations[h.y - 1][h.x] = Math.max(len, s.snakeNextMovePossibleLocations[h.y - 1][h.x]);
                            /*if(s.foodPlaces.contains(new Point(h.y - 1, h.x))){
                                isFoodReachableForSnake = true;
                            }*/
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }
                    try {
                        if (s.snakeBodies[h.y + 1][h.x] == 0) {
                            // it might be that at the snakeNextMovePossibleLocations we have already a value of another
                            // snake - so we make sure that's the MAX value!
                            s.snakeNextMovePossibleLocations[h.y + 1][h.x] = Math.max(len, s.snakeNextMovePossibleLocations[h.y + 1][h.x]);
                            /*if(!isFoodReachableForSnake && s.foodPlaces.contains(new Point(h.y + 1, h.x))){
                                isFoodReachableForSnake = true;
                            }*/
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }
                    try {
                        if (s.snakeBodies[h.y][h.x - 1] == 0) {
                            s.snakeNextMovePossibleLocations[h.y][h.x - 1] = Math.max(len, s.snakeNextMovePossibleLocations[h.y][h.x - 1]);
                            /*if(!isFoodReachableForSnake && s.foodPlaces.contains(new Point(h.y, h.x - 1))){
                                isFoodReachableForSnake = true;
                            }*/
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }
                    try {
                        if (s.snakeBodies[h.y][h.x + 1] == 0) {
                            s.snakeNextMovePossibleLocations[h.y][h.x + 1] = Math.max(len, s.snakeNextMovePossibleLocations[h.y][h.x + 1]);
                            /*if(!isFoodReachableForSnake && s.foodPlaces.contains(new Point(h.y, h.x + 1))){
                                isFoodReachableForSnake = true;
                            }*/
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }

                    // dealing with the bodies of the other snakes...
                    JsonNode body = aSnake.get("body");
                    int bLen = body.size();

                    // IF THERE is NO FOOD directly ahead of the other Snake, we can ignore the last
                    // PART of the snake as well!!
                    /*if(!isFoodReachableForSnake) {
                        bLen--;
                    }*/

                    // a) we start from j=1 here - since we have handled the SneakHEAD's already
                    // b) we also do not have top care about the LAST entry in the body, since this
                    // we be always FREE after "this" turn (if the snake grows, that the last
                    // and the prev record of the body contain the same position!)
                    for (int j = 1; j < bLen-1; j++) {
                        Point p = new Point(body.get(j));
                        s.snakeBodies[p.y][p.x] = 1;
                    }
                }
            }

            JsonNode haz = board.get("hazards");
            if (haz != null) {
                int hLen = haz.size();
                for (int i = 0; i < hLen; i++) {
                    Point h = new Point(haz.get(i));
                    s.hazardZone[h.y][h.x] = 1;
                    /*s.hazardPlaces.add(h);
                    // one time around the pudding... (we want to avoid food that
                    // is nearby hazards!
                    s.hazardNearbyPlaces.add(new Point(h.y + 1, h.x + 0));
                    s.hazardNearbyPlaces.add(new Point(h.y + 1, h.x + 1));
                    s.hazardNearbyPlaces.add(new Point(h.y + 0, h.x + 1));
                    s.hazardNearbyPlaces.add(new Point(h.y - 1, h.x + 1));
                    s.hazardNearbyPlaces.add(new Point(h.y - 1, h.x + 0));
                    s.hazardNearbyPlaces.add(new Point(h.y - 1, h.x - 1));
                    s.hazardNearbyPlaces.add(new Point(h.y + 0, h.x - 1));
                    s.hazardNearbyPlaces.add(new Point(h.y + 1, h.x - 1));
                     */
                }
            }

            // after we have read all positions/Objects we might to additionally init the current
            // session status...
            s.initSessionAfterFullBoardRead();
            s.logState("MOVE CALLED");
            s.logBoard();
        }

        private boolean checkFoF(String fof) {
            return  fof.indexOf("tatos") > -1
                    || fof.indexOf("sr2") > -1
                    || fof.indexOf("paranoidba") > -1
                    ;
        }

        private String calculateNextMove(Session s) {
            String move = s.checkSpecialMoves();
            if (move == null) {
                switch (s.state) {
                    case UP:
                        move = s.moveUp();
                        break;
                    case RIGHT:
                        move = s.moveRight();
                        break;
                    case DOWN:
                        move = s.moveDown();
                        break;
                    case LEFT:
                        move = s.moveLeft();
                        break;
                    default:
                        move = Snake.D;
                }
            }
            return move;
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
            Session s = sessions.remove(endRequest.get("game").get("id").asText());

            // get OWN ID
            JsonNode you = endRequest.get("you");
            String myId = you.get("id").asText();

            JsonNode board = endRequest.get("board");
            // get the locations of all snakes...
            JsonNode snakes = board.get("snakes");
            int sLen = snakes.size();
            if(sLen > 0) {
                for (int i = 0; i < sLen; i++) {
                    JsonNode aSnake = snakes.get(i);
                    if (aSnake.get("id").asText().equals(myId)) {
                        LOG.info("****************");
                        LOG.info("WE ARE ALIVE!!!!");
                        LOG.info("****************");
                    } else {
                        LOG.info("that's not us... " + aSnake);
                    }
                }
            } else {
                LOG.info("that's not us... ");
            }
            return EMPTY;
        }
    }

    private void ranomizer(){
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
    }
}