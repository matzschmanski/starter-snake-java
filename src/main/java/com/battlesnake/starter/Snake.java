package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.battlesnake.starter.SnakeAttribute.*;
import static com.battlesnake.starter.Util.boardToArray;
import static spark.Spark.*;

/**
 * This is a simple Battlesnake server written in Java.
 * <p>
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

        private static QItem minDistance(char[][] grid, QItem source,QItem destination)
        {
            char oldChar =grid[destination.x][destination.y];
            grid[destination.x][destination.y] = Util.DESTINATION;

            // To keep track of visited QItems. Marking
            // blocked cells as visited.
            firstLoop:
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++)
                {

                    // Finding source
                    if (grid[i][j] == Util.SOURCE) {
                        source.x = i;
                        source.y = j;
                        break firstLoop;
                    }
                }
            }

            // applying BFS on matrix cells starting from source
            Queue<QItem> queue = new LinkedList<>();
            queue.add(new QItem(source.x, source.y, 0));

            boolean[][] visited
                    = new boolean[grid.length][grid[0].length];
            visited[source.x][source.y] = true;

            while (!queue.isEmpty()) {
                QItem p = queue.remove();

                // Destination found;
                if (grid[p.x][p.y] == Util.DESTINATION) {
                    grid[destination.x][destination.y] = oldChar;
                    p.setX(grid.length-p.getX()-1);
                    return p;
                }

                // moving up
                if (isValid(p.x - 1, p.y, grid, visited)) {
                    queue.add(new QItem(p.x - 1, p.y,
                            p.dist + 1));
                    visited[p.x - 1][p.y] = true;
                }

                // moving down
                if (isValid(p.x + 1, p.y, grid, visited)) {
                    queue.add(new QItem(p.x + 1, p.y,
                            p.dist + 1));
                    visited[p.x + 1][p.y] = true;
                }

                // moving left
                if (isValid(p.x, p.y - 1, grid, visited)) {
                    queue.add(new QItem(p.x, p.y - 1,
                            p.dist + 1));
                    visited[p.x][p.y - 1] = true;
                }

                // moving right
                if (isValid(p.x, p.y + 1, grid,
                        visited)) {
                    queue.add(new QItem(p.x, p.y + 1,
                            p.dist + 1));
                    visited[p.x][p.y + 1] = true;
                }
            }
            grid[destination.x][destination.y] = oldChar;
            return null;
        }

        // checking where it's valid or not
        private static boolean isValid(int x, int y,
                                       char[][] grid,
                                       boolean[][] visited)
        {
            if (x >= 0 && y >= 0 && x < grid.length
                    && y < grid[0].length && grid[x][y] != Util.CAN_NOT_TRAVEL
                    && !visited[x][y]) {
                return true;
            }
            return false;
        }


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
         * <p>
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @return a response back to the engine containing the Battlesnake setup
         * values.
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
         * <p>
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
            LOG.info("Creating board '{}' with height: {} and width: {}", id, height, width);
            boards.put(id, createBoard(height, width));
            LOG.info("START");
            return EMPTY;
        }

        private char[][] createBoard(int height, int width) {
            char[][] board = new char[height][width];

            return board;
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         * <p>
         * Use the information in 'moveRequest' to decide your next move. The
         * 'moveRequest' variable can be interacted with as
         * com.fasterxml.jackson.databind.JsonNode, and contains all of the information
         * about the Battlesnake board for each move of the game.
         * <p>
         * For a full example of 'json', see
         * https://docs.battlesnake.com/references/api/sample-move-request
         *
         * @param moveRequest JsonNode of all Game Board data as received from the
         *                    Battlesnake Engine.
         * @return a Map<String,String> response back to the engine the single move to
         * make. One of "up", "down", "left" or "right".
         */
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


            String gameId = moveRequest.get(GAME).get(ID).asText();
            GameLibrary.getInstance().getGames().getOrDefault(gameId, new GameDetails(GameMode.GO_LEFT_BOTTOM));


            char[][] board = boardToArray(moveRequest);
            //get food
            // calculate closest food
            JsonNode food = moveRequest.get(BOARD).get(FOOD);
            JsonNode head = moveRequest.get(YOU).get(HEAD);
            QItem source = new QItem(Util.snakeToBoard(board,head.get(Y).asInt()),Util.snakeToBoard(board,head.get(X).asInt()) , 0);

            System.out.println();

            List<QItem> foundFood = StreamSupport.stream(food.spliterator(), true)
                    .map(coordinate -> new QItem(Util.snakeToBoard(board,coordinate.get(X).asInt()), coordinate.get(Y).asInt(), 0))
                    .map(destination -> minDistance(board, source, destination))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            foundFood = foundFood.stream()
                    .sorted(Comparator.comparing(QItem::getDist))
                    .collect(Collectors.toList());

            ArrayList<String> possibleMoves = new ArrayList<>();
            if (foundFood.size() > 0) {
                QItem targetFood = foundFood.get(0);
                LOG.info("found food at {} with distance of {}", targetFood, targetFood.getDist());

                if (targetFood.getX() < head.get(X).asInt()) {
                    possibleMoves.add(LEFT);
                } else if (targetFood.getX() > head.get(X).asInt()) {
                    possibleMoves.add(RIGHT);
                }
                if (targetFood.getY() < head.get(Y).asInt()) {
                    possibleMoves.add(DOWN);
                } else if (targetFood.getY() > head.get(Y).asInt()) {
                    possibleMoves.add(UP);
                }
            } else {
                possibleMoves = new ArrayList<>(Arrays.asList(UP, DOWN, LEFT, RIGHT));
            }


            possibleMoves = survive(moveRequest, possibleMoves);

            if(possibleMoves.size()==0){
                //SNAP
                possibleMoves = new ArrayList<>(Arrays.asList(UP, DOWN, LEFT, RIGHT));
                possibleMoves = survive(moveRequest, possibleMoves);
                LOG.warn("Avoided sending same command because out of moves");
            }


            LOG.info("possible moves:");
            for (String move: possibleMoves){
                System.out.println(move);
            }
            final int choice = new Random().nextInt(possibleMoves.size());
            final String move = possibleMoves.get(choice);
            LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }

        private ArrayList<String> survive(JsonNode moveRequest, ArrayList<String> possibleMoves) {
            JsonNode board = moveRequest.get(BOARD);

            int height = board.get(HEIGHT).asInt();
            int width = board.get(WIDTH).asInt();
            JsonNode snakes = board.get(SNAKES);
            JsonNode head = moveRequest.get(YOU).get(HEAD);
            JsonNode body = moveRequest.get(YOU).get(BODY);
            int myLength = moveRequest.get("you").get("length").asInt();
            Map<String, Point> nextPositions = generateNextPositions(head);

            possibleMoves = avoidMyNeck(head, body, possibleMoves);
            possibleMoves = avoidWalls(head, height, width, possibleMoves);
            possibleMoves = avoidSelf(head, body, possibleMoves, nextPositions);
            possibleMoves = avoidOthers(snakes, possibleMoves, nextPositions);
            possibleMoves = avoidBiggerHeads(head,myLength,snakes,possibleMoves, nextPositions);
            return possibleMoves;
        }

        public ArrayList<String> avoidBiggerHeads(JsonNode head,int myLength, JsonNode snakes, ArrayList<String> possibleMoves, Map<String, Point> nextPositions){
            //TODO for survive calculate possible next head moves for other snakes, remove those fields if snake has more points
            for(JsonNode snake : snakes){
                JsonNode potentialEnemyHead = snake.get("head");
                if(!potentialEnemyHead.equals(head)){
                    if(snake.get("length").asInt()>=myLength){
                        //avoid!
                        Map<String, Point> nextPositionsForEnemy = generateNextPositions(potentialEnemyHead);
                        for (Map.Entry<String, Point> entry : nextPositionsForEnemy.entrySet()) {
                            if(entry.getValue().equals(nextPositions.get(entry.getKey()))){
                                possibleMoves.remove(entry.getKey());
                            }
                        }
                    }
                }
            }
            return possibleMoves;
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
        public ArrayList<String> avoidMyNeck(JsonNode head, JsonNode body, ArrayList<String> possibleMoves) {
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
            return possibleMoves;
        }

        public ArrayList<String> avoidWalls(JsonNode head, int boardHeight, int boardWidth, ArrayList<String> possibleMoves) {
            if (head.get(X).asInt() + 1 == boardHeight) {
                possibleMoves.remove(RIGHT);
            }
            if (head.get(X).asInt() - 1 < 0) {
                possibleMoves.remove(LEFT);
            }
            if (head.get(Y).asInt() + 1 == boardWidth) {
                possibleMoves.remove(UP);
            }
            if (head.get(Y).asInt() - 1 < 0) {
                possibleMoves.remove(DOWN);
            }
            return possibleMoves;
        }

        public Map<String, Point> generateNextPositions(JsonNode head) {
            int currentX = head.get(X).asInt();
            int currentY = head.get(Y).asInt();
            Point right = new Point(currentX + 1, currentY);
            Point left = new Point(currentX - 1, currentY);
            Point up = new Point(currentX, currentY + 1);
            Point down = new Point(currentX, currentY - 1);
            Map<String, Point> moves = Map.of(RIGHT, right, LEFT, left, DOWN, down, UP, up);
            return moves;
        }

        public ArrayList<String> avoidOthers(JsonNode snakes, ArrayList<String> possibleMoves, Map<String, Point> nextPositions) {
            List<Point> noGoAreas = StreamSupport.stream(snakes.spliterator(), false).flatMap(jsonNode -> StreamSupport.stream(jsonNode.get(BODY).spliterator(), false)).map(coordinate -> new Point(coordinate.get(X).asInt(), coordinate.get(Y).asInt())).collect(Collectors.toList());
            for (Map.Entry<String, Point> pair : nextPositions.entrySet()) {
                if (noGoAreas.contains(pair.getValue())) {
                    possibleMoves.remove(pair.getKey());
                }

            }
            return possibleMoves;

        }


        public ArrayList<String> avoidSelf(JsonNode head, JsonNode body, ArrayList<String> possibleMoves, Map<String, Point> nextPositions) {

            List<Point> noGoAreas = new ArrayList<>();
            noGoAreas.add(new Point(head.get(X).asInt(), head.get(Y).asInt()));
            //ignore tail
            for (int i = 0; i < body.size()-1; i++) {
                final JsonNode objNode = body.get(i);
                noGoAreas.add(new Point(objNode.get(X).asInt(), objNode.get(Y).asInt()));
            }
//            for (final JsonNode objNode : body) {
//                noGoAreas.add(new Point(objNode.get(X).asInt(), objNode.get(Y).asInt()));
//            }

            for (Map.Entry<String, Point> pair : nextPositions.entrySet()) {
                if (noGoAreas.contains(pair.getValue())) {
                    possibleMoves.remove(pair.getKey());
                }

            }
            return possibleMoves;
        }

        /**
         * This method is called when a game your Battlesnake was in ends.
         * <p>
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
            LOG.info("ENDED ({})", gameId);
            return EMPTY;
        }


    }


}
