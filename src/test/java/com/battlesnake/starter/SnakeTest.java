package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.IO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.battlesnake.starter.Util.boardToArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnakeTest {

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

        handler.avoidMyNeck(testHead, testBody, possibleMoves);

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

        handler.avoidMyNeck(testHead, testBody, possibleMoves);

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

        handler.avoidMyNeck(testHead, testBody, possibleMoves);

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

        handler.avoidMyNeck(testHead, testBody, possibleMoves);

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

        handler.avoidMyNeck(testHead, testBody, possibleMoves);

        assertTrue(possibleMoves.size() == 3);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidSelfTest() throws IOException {
        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 4, \"y\": 1}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"x\": 3, \"y\": 0}, {\"x\": 4, \"y\": 0}, {\"x\": 5, \"y\": 0}, {\"x\": 5, \"y\": 1}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("up", "left"));


            Map<String, Point> nextPositions = handler.generateNextPositions(testHead);
            handler.avoidSelf(testHead,testBody,possibleMoves,nextPositions);
        assertTrue(possibleMoves.size() == 2);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void avoidOthersTest() throws IOException {
        JsonNode testHead = OBJECT_MAPPER.readTree("{\"x\": 5, \"y\": 5}");
        JsonNode testBody = OBJECT_MAPPER
                .readTree("[{\"id\": \"snake-one\", \"body\": [{\"x\": 4, \"y\": 5},{\"x\": 5, \"y\": 4}, {\"x\": 5, \"y\": 6}]}]");
        ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));
        ArrayList<String> expectedResult = new ArrayList<>(Arrays.asList("right"));


        Map<String, Point> nextPositions = handler.generateNextPositions(testHead);
        handler.avoidOthers(testBody,possibleMoves,nextPositions);
        assertTrue(possibleMoves.size() == 1);
        assertTrue(possibleMoves.equals(expectedResult));
    }

    @Test
    void buildArrayTest() throws IOException {
        JsonNode testMoveRequest = OBJECT_MAPPER.readTree("{ \"game\": { \"id\": \"game-00fe20da-94ad-11ea-bb37\", \"ruleset\": { \"name\": \"standard\", \"version\": \"v.1.2.3\" }, \"timeout\": 500 }, \"turn\": 14, \"board\": { \"height\": 11, \"width\": 11, \"food\": [{ \"x\": 5, \"y\": 5 }, { \"x\": 9, \"y\": 0 }, { \"x\": 2, \"y\": 6 }], \"hazards\": [{ \"x\": 3, \"y\": 2 }], \"snakes\": [{ \"id\": \"snake-508e96ac-94ad-11ea-bb37\", \"name\": \"My Snake\", \"health\": 54, \"body\": [{ \"x\": 0, \"y\": 0 }, { \"x\": 1, \"y\": 0 }, { \"x\": 2, \"y\": 0 }], \"latency\": \"111\", \"head\": { \"x\": 0, \"y\": 0 }, \"length\": 3, \"shout\": \"why are we shouting??\", \"squad\": \"\", \"customizations\": { \"color\": \"#FF0000\", \"head\": \"pixel\", \"tail\": \"pixel\" } }, { \"id\": \"snake-b67f4906-94ae-11ea-bb37\", \"name\": \"Another Snake\", \"health\": 16, \"body\": [{ \"x\": 5, \"y\": 4 }, { \"x\": 5, \"y\": 3 }, { \"x\": 6, \"y\": 3 }, { \"x\": 6, \"y\": 2 }], \"latency\": \"222\", \"head\": { \"x\": 5, \"y\": 4 }, \"length\": 4, \"shout\": \"I'm not really sure...\", \"squad\": \"\", \"customizations\": { \"color\": \"#26CF04\", \"head\": \"silly\", \"tail\": \"curled\" } }] }, \"you\": { \"id\": \"snake-508e96ac-94ad-11ea-bb37\", \"name\": \"My Snake\", \"health\": 54, \"body\": [{ \"x\": 0, \"y\": 0 }, { \"x\": 1, \"y\": 0 }, { \"x\": 2, \"y\": 0 }], \"latency\": \"111\", \"head\": { \"x\": 0, \"y\": 0 }, \"length\": 3, \"shout\": \"why are we shouting??\", \"squad\": \"\", \"customizations\": { \"color\": \"#FF0000\", \"head\": \"pixel\", \"tail\": \"pixel\" } } }");
        boardToArray(testMoveRequest);
    }

    @Test
    void testFood() throws IOException{
        JsonNode testMoveRequest = OBJECT_MAPPER.readTree("{\"game\":{\"id\":\"52a7c7c0-39b6-48de-83c9-c6beca213521\",\"ruleset\":{\"name\":\"standard\",\"version\":\"v1.0.25\",\"settings\":{\"foodSpawnChance\":15,\"minimumFood\":1,\"hazardDamagePerTurn\":14,\"royale\":{\"shrinkEveryNTurns\":0},\"squad\":{\"allowBodyCollisions\":false,\"sharedElimination\":false,\"sharedHealth\":false,\"sharedLength\":false}}},\"timeout\":500,\"source\":\"custom\"},\"turn\":0,\"board\":{\"height\":11,\"width\":11,\"snakes\":[{\"id\":\"gs_9kYjYgr3rBKFcB6w4yXFRXvb\",\"name\":\"sr2\",\"latency\":\"\",\"health\":100,\"body\":[{\"x\":5,\"y\":9},{\"x\":5,\"y\":9},{\"x\":5,\"y\":9}],\"head\":{\"x\":5,\"y\":9},\"length\":3,\"shout\":\"\",\"squad\":\"\",\"customizations\":{\"color\":\"#000000\",\"head\":\"default\",\"tail\":\"default\"}},{\"id\":\"gs_DRBhdRB7PTqqBGjpdjpRJjXT\",\"name\":\"sr2v2\",\"latency\":\"\",\"health\":100,\"body\":[{\"x\":9,\"y\":5},{\"x\":9,\"y\":5},{\"x\":9,\"y\":5}],\"head\":{\"x\":9,\"y\":5},\"length\":3,\"shout\":\"\",\"squad\":\"\",\"customizations\":{\"color\":\"#000000\",\"head\":\"default\",\"tail\":\"default\"}},{\"id\":\"gs_4YdkVXdyj7qtVHqG8XWqRpk4\",\"name\":\"ElenderDödel [@home]\",\"latency\":\"\",\"health\":100,\"body\":[{\"x\":1,\"y\":5},{\"x\":1,\"y\":5},{\"x\":1,\"y\":5}],\"head\":{\"x\":1,\"y\":5},\"length\":3,\"shout\":\"\",\"squad\":\"\",\"customizations\":{\"color\":\"#ff1111\",\"head\":\"sand-worm\",\"tail\":\"block-bum\"}},{\"id\":\"gs_ptkmghT3PCVGjWFmdJBVCTbX\",\"name\":\"tatosCooleSchlange\",\"latency\":\"\",\"health\":100,\"body\":[{\"x\":5,\"y\":1},{\"x\":5,\"y\":1},{\"x\":5,\"y\":1}],\"head\":{\"x\":5,\"y\":1},\"length\":3,\"shout\":\"\",\"squad\":\"\",\"customizations\":{\"color\":\"#cc33ff\",\"head\":\"sand-worm\",\"tail\":\"skinny\"}}],\"food\":[{\"x\":6,\"y\":10},{\"x\":10,\"y\":6},{\"x\":0,\"y\":4},{\"x\":6,\"y\":0},{\"x\":5,\"y\":5}],\"hazards\":[]},\"you\":{\"id\":\"gs_ptkmghT3PCVGjWFmdJBVCTbX\",\"name\":\"tatosCooleSchlange\",\"latency\":\"\",\"health\":100,\"body\":[{\"x\":5,\"y\":1},{\"x\":5,\"y\":1},{\"x\":5,\"y\":1}],\"head\":{\"x\":5,\"y\":1},\"length\":3,\"shout\":\"\",\"squad\":\"\",\"customizations\":{\"color\":\"#cc33ff\",\"head\":\"sand-worm\",\"tail\":\"skinny\"}}}");
        handler.move(testMoveRequest);
    }
}
