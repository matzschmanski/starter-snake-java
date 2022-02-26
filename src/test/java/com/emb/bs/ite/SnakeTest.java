package com.emb.bs.ite;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnakeTest {

    private static final Logger LOG = LoggerFactory.getLogger(SnakeTest.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    private Snake.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new Snake.Handler();
    }

    @Test
    void replay() throws Exception{
        String[] currentDir = new File(new File("").getAbsolutePath()).list();
        if(currentDir != null && currentDir.length >0) {
            Arrays.sort(currentDir, Collections.reverseOrder());
            for (String fName : currentDir) {
                if (fName.startsWith("req_")) {
                    List<String> simJson = readFileLineByLineAsList((new File(fName).toPath()));
                    LOG.info("REPLAY: "+fName);
                    handler.start(OBJECT_MAPPER.readTree(simJson.remove(0)));
                    for (String line : simJson) {
                        handler.move(OBJECT_MAPPER.readTree(line));
                    }
                    break;
                }
            }
        }
    }

    @Test
    void replayJSON() throws Exception{
        String[] currentDir = new File(new File("").getAbsolutePath()).list();
        if(currentDir != null && currentDir.length >0) {
            Arrays.sort(currentDir, Collections.reverseOrder());
            for (String fName : currentDir) {
                if (fName.endsWith(".json")) {
                    String simJsonString = readFileAsString((new File(fName).toPath()));
                    LOG.info("REPLAY: "+fName);
                    JsonNode list = OBJECT_MAPPER.readTree(simJsonString);
                    JsonNode first = list.get(0);
                    handler.start(convertToGame(first));
                    for(int i=1; i < list.size(); i++){
                        handler.move(convertToGame(list.get(i)));
                    }
                    break;
                }
            }
        }
    }

    private static final String gameID = "myId";
    private JsonNode convertToGame(JsonNode gamePlay) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode game = OBJECT_MAPPER.createObjectNode();
        root.put("game", game);
        game.put("id", gameID);

        root.put("turn", gamePlay.get("turn").asInt());

        ObjectNode board = OBJECT_MAPPER.createObjectNode();
        root.put("board", board);
        board.put("height", 11);
        board.put("width", 11);

        board.put("food", gamePlay.get("food"));

        ArrayNode targetSnakes = OBJECT_MAPPER.createArrayNode();
        board.put("snakes", targetSnakes);

        ObjectNode you = OBJECT_MAPPER.createObjectNode();
        root.put("you", you);

        JsonNode srcSnakes = gamePlay.get("snakes");
        for(int i=0; i<srcSnakes.size(); i++){
            ObjectNode dest = OBJECT_MAPPER.createObjectNode();
            JsonNode srcS = srcSnakes.get(i);
            if(srcS.get("name").asText().indexOf("lend")>-1){
                dest = you;
            }else{
                targetSnakes.add(dest);
            }
            dest.put("id", srcS.get("_id").asText());
            dest.put("name", srcS.get("name").asText());
            dest.put("body", srcS.get("body"));
            dest.put("head", srcS.get("body").get(0));
            dest.put("length", srcS.get("body").size());
            dest.put("health", srcS.get("health").intValue());
        }

        return root;
    }


    /*
    @Test
    void indexTest() throws IOException {
        Map<String, String> response = handler.index();
        assertEquals("#888888", response.get("color"));
        assertEquals("default", response.get("head"));
        assertEquals("default", response.get("tail"));
    }

    @Test
    void startTest() throws IOException {
        JsonNode startRequest = OBJECT_MAPPER.readTree("{}");
        Map<String, String> response = handler.end(startRequest);
        assertEquals(0, response.size());

    }

    @Test
    void moveTest() throws IOException {
        JsonNode moveRequest = OBJECT_MAPPER.readTree(
                "{\"game\":{\"id\":\"game-00fe20da-94ad-11ea-bb37\",\"ruleset\":{\"name\":\"standard\",\"version\":\"v.1.2.3\"},\"timeout\":500},\"turn\":14,\"board\":{\"height\":11,\"width\":11,\"food\":[{\"x\":5,\"y\":5},{\"x\":9,\"y\":0},{\"x\":2,\"y\":6}],\"hazards\":[{\"x\":3,\"y\":2}],\"snakes\":[{\"id\":\"snake-508e96ac-94ad-11ea-bb37\",\"name\":\"My Snake\",\"health\":54,\"body\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":0},{\"x\":2,\"y\":0}],\"latency\":\"111\",\"head\":{\"x\":0,\"y\":0},\"length\":3,\"shout\":\"why are we shouting??\",\"squad\":\"\"},{\"id\":\"snake-b67f4906-94ae-11ea-bb37\",\"name\":\"Another Snake\",\"health\":16,\"body\":[{\"x\":5,\"y\":4},{\"x\":5,\"y\":3},{\"x\":6,\"y\":3},{\"x\":6,\"y\":2}],\"latency\":\"222\",\"head\":{\"x\":5,\"y\":4},\"length\":4,\"shout\":\"I'm not really sure...\",\"squad\":\"\"}]},\"you\":{\"id\":\"snake-508e96ac-94ad-11ea-bb37\",\"name\":\"My Snake\",\"health\":54,\"body\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":0},{\"x\":2,\"y\":0}],\"latency\":\"111\",\"head\":{\"x\":0,\"y\":0},\"length\":3,\"shout\":\"why are we shouting??\",\"squad\":\"\"}}");
        Map<String, String> response = handler.move(moveRequest);

        List<String> options = new ArrayList<String>();
        options.add("up");
        options.add("down");
        options.add("left");
        options.add("right");

        assertTrue(options.contains(response.get("move")));
    }

    @Test
    void endTest() throws IOException {
        JsonNode endRequest = OBJECT_MAPPER.readTree("{}");
        Map<String, String> response = handler.end(endRequest);
        assertEquals(0, response.size());
    }

    @Test
    void avoidNeckAllTest() throws IOException {

        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 5, \"y\": 5}, {\"x\": 5, \"y\": 5}, {\"x\": 5, \"y\": 5}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));

        //handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 4);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidNeckLeftTest() throws IOException {

        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 5, \"y\": 5}, {\"x\": 4, \"y\": 5}, {\"x\": 3, \"y\": 5}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("up", "down", "right"));

        //handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 3);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidNeckRightTest() throws IOException {

        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 5, \"y\": 5}, {\"x\": 6, \"y\": 5}, {\"x\": 7, \"y\": 5}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("up", "down", "left"));

        //handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 3);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidNeckUpTest() throws IOException {

        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 5, \"y\": 5}, {\"x\": 5, \"y\": 6}, {\"x\": 5, \"y\": 7}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("down", "left", "right"));

        //handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 3);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidNeckDownTest() throws IOException {

        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 5, \"y\": 5}, {\"x\": 5, \"y\": 4}, {\"x\": 5, \"y\": 3}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("up", "left", "right"));

        //handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 3);
        assertTrue(possibleMoves.equals(expectedResult));
    }
    */

    private static String[] readFileLineByLine(Path inputFilePath) {
        Charset charset = Charset.defaultCharset();
        List<String> stringList = null;
        try {
            stringList = Files.readAllLines(inputFilePath, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringList.toArray(new String[]{});
    }

    private static List<String> readFileLineByLineAsList(Path inputFilePath) {
        Charset charset = Charset.defaultCharset();
        List<String> stringList = null;
        try {
            stringList = Files.readAllLines(inputFilePath, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringList;
    }
    private static String readFileAsString(Path inputFilePath) {
        Charset charset = Charset.defaultCharset();
        String data = null;
        try {
            byte[] bytes = Files.readAllBytes(inputFilePath);
            data = new String(bytes, charset);
            //data = Files.readString(inputFilePath, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
