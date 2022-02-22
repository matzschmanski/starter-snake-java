package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class Session {

    //private static final Logger LOG = LoggerFactory.getLogger(Session.class);
    private static final SLogger LOG = new SLogger();

    private static class SLogger extends ArrayList<String> {
        private boolean doIt = true;
        private ArrayList<JsonNode> req = new ArrayList<>(5000);

        private static final Logger iLOG = LoggerFactory.getLogger(Session.class);

        public void debug(String s) {
            if(doIt) {
                if (Snake.loggingFailed) {
                    add(s);
                }
                iLOG.debug(s);
            }
        }

        public void info(String s) {
            if(doIt) {
                if (Snake.loggingFailed) {
                    add(s);
                }
                iLOG.info(s);
            }
        }

        public void info(String s, Throwable t) {
            if(doIt) {
                if (Snake.loggingFailed) {
                    add(s);
                }
                iLOG.info(s, t);
            }
        }

        public void error(String s) {
            if(doIt) {
                if (Snake.loggingFailed) {
                    add(s);
                }
                iLOG.error(s);
            }
        }

        private void write() {
            if(Snake.loggingFailed) {
                Long ts = System.currentTimeMillis();
                writeLOG(ts);
                writeREQ(ts);
            }
        }

        private void writeLOG(long ts) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File("log_"+ts+".txt"));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                for (String line : this) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.close();
            } catch (IOException e) {
                iLOG.error("", e);
            } finally {
                if(fos != null){
                    try {
                        fos.close();
                    }catch(Exception e){}
                }
            }
        }

        private void writeREQ(long ts) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File("req_"+ts+".txt"));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                for (JsonNode json : req) {
                    bw.write(json.toString());
                    bw.newLine();
                }
                bw.close();
            } catch (IOException e) {
                iLOG.error("", e);
            } finally {
                if(fos != null){
                    try {
                        fos.close();
                    }catch(Exception e){}
                }
            }
        }

        private void logReq(JsonNode json) {
            req.add(json);
        }
    }

    Point pos;
    int len;
    int health;
    int state = 0;
    int tPhase = 0;
    ArrayList<Integer> cmdChain = null;
    HashSet<Integer> movesToIgnore = new HashSet<>();

    boolean doomed = false;
    int X = -1;
    int Y = -1;
    int[][] enemyBodies = null;
    int[][] enemyNextMovePossibleLocations = null;
    int[][] myBody = null;
    ArrayList<Point> foodPlaces = null;
    boolean patched = false;
    int stateToRestore = -1;
    private boolean enterDangerZone = false;
    private boolean avoidBorder = true;
    private int xMin, yMin, xMax, yMax;

    public void writeLogDataToFS(){
        LOG.write();
    }

    public void logReq(JsonNode json) {
        LOG.logReq(json);
    }

    public void logMove(String move) {
        LOG.add("=> RESULTING MOVE: "+move);
    }

    public void log(boolean b) {
        LOG.doIt = b;
    }

    public void initSaveBoardBounds(){
        yMin = 1;
        xMin = 1;
        yMax = Y - 2;
        xMax = X - 2;
        enterDangerZone = false;
        avoidBorder = true;
    }

    private void setFullBoardBounds(){
        yMin = 0;
        xMin = 0;
        yMax = Y - 1;
        xMax = X - 1;
    }

    private boolean checkDoomed(int cmdToAdd) {
        cmdChain.add(cmdToAdd);
        if(cmdChain.size() > 4){
            if(avoidBorder){
                LOG.info("activate now GO-TO-BORDERS");
                avoidBorder = false;
                setFullBoardBounds();
                cmdChain = new ArrayList<>();
                cmdChain.addAll(movesToIgnore);
                cmdChain.add(cmdToAdd);
            } else if (!enterDangerZone) {
                LOG.info("activate now GO-TO-DANGER-ZONE");
                enterDangerZone = true;
                cmdChain = new ArrayList<>();
                cmdChain.addAll(movesToIgnore);
                cmdChain.add(cmdToAdd);
            } else {
                doomed = true;
                LOG.error("DOOMED! "+tPhase + " avoidBorder? "+ avoidBorder +" goDangerZone? "+ enterDangerZone +" {" + cmdChain.toString() + "}");
                return true;
            }
        }
        return false;
    }

    public String checkSpecialMoves(){
        // if we are in the UPPERROW and the x=0 is free, let's move to the LEFT!
        if(tPhase > 0 && pos.y == yMax && pos.x < xMax /3){
            if(pos.x > 0) {
                LOG.info("SPECIAL MOVE -> LEFT CALLED");
                return moveLeft();
            }else{
                LOG.info("SPECIAL MOVE -> DOWN CALLED");
                return moveDown();
            }
        }
        return null;
    }

    private boolean canMoveUp(){
        try {
            return  pos.y < yMax
                    && myBody[pos.y + 1][pos.x] == 0
                    && enemyBodies[pos.y + 1][pos.x] == 0
                    && (enterDangerZone || enemyNextMovePossibleLocations[pos.y + 1][pos.x] < len);
        }catch(IndexOutOfBoundsException e){
            LOG.info("IoB @ canMoveUp check...", e);
            return false;
        }
    }

    public String moveUp() {
        if(cmdChain.size() < 4 && cmdChain.contains(Snake.UP)){
            // here we can generate randomness!
            return moveRight();
        } else {
            if (checkDoomed(Snake.UP)) {
                //LOG.debug("UP: DOOMED -> "+Snake.D);
                return Snake.D;
            }
            logState("UP");
            if (canMoveUp()) {
LOG.debug("UP: YES");
                return Snake.U;
            } else {
LOG.debug("UP: NO");
                // can't move...
                if(pos.x < xMax/2 || cmdChain.contains(Snake.LEFT)) {
                    state = Snake.RIGHT;
                    //LOG.debug("UP: NO - check RIGHT x:" + pos.x + " < Xmax/2:"+ xMax/2);
                    return moveRight();
                } else {
                    state = Snake.LEFT;
                    //LOG.debug("UP: NO - check LEFT");
                    return moveLeft();
                }
            }
        }
    }

    private boolean canMoveRight(){
        try {
            return  pos.x < xMax
                    && myBody[pos.y][pos.x + 1] == 0
                    && enemyBodies[pos.y][pos.x + 1] == 0
                    && (enterDangerZone || enemyNextMovePossibleLocations[pos.y][pos.x + 1] < len);
                    //&& (enemyHeads[pos.y].length < pos.x + 3 || enemyHeads[pos.y][pos.x + 2] < len)
        }catch(IndexOutOfBoundsException e){
            LOG.info("IoB @ canMoveRight check...", e);
            return false;
        }
    }

    public String moveRight() {
        if(cmdChain.size() < 4 && cmdChain.contains(Snake.RIGHT)){
            return moveDown();
        }else {
            if (checkDoomed(Snake.RIGHT)) {
                //LOG.debug("RIGHT: DOOMED -> "+Snake.L);
                return Snake.L;
            }
            logState("RIGHT");
            if (canMoveRight()) {
LOG.debug("RIGHT: YES");
                return Snake.R;
            } else {
LOG.debug("RIGHT: NO");
                // can't move...
                if (pos.x == xMax && tPhase > 0) {
                    if (pos.y == yMax) {
                        // we should NEVER BE HERE!!
                        // we are in the UPPER/RIGHT Corner while in TraverseMode! (something failed before)
                        LOG.info("WE SHOULD NEVER BE HERE in T-PHASE >0");
                        tPhase = 0;
                        state = Snake.DOWN;
                        return moveDown();
                    } else {
                        state = Snake.LEFT;
                        return moveUp();
                    }
                } else {
                    return decideForUpOrDownUsedFromMoveLeftOrRight(Snake.RIGHT);
                }
            }
        }
    }

    private boolean canMoveDown(){
        try {
            return  pos.y > yMin
                    && myBody[pos.y - 1][pos.x] == 0
                    && enemyBodies[pos.y - 1][pos.x] == 0
                    && (enterDangerZone || enemyNextMovePossibleLocations[pos.y - 1][pos.x] < len);
            //&& (pos.y < 2 || enemyHeads[pos.y - 2][pos.x] < len)
        }catch(IndexOutOfBoundsException e){
            LOG.info("IoB @ canMoveDown check...", e);
            return false;
        }
    }

    public String moveDown() {
        if(cmdChain.size() < 4 && cmdChain.contains(Snake.DOWN)){
            return moveLeft();
        }
        if(checkDoomed(Snake.DOWN)){
            //LOG.debug("DOWN: DOOMED -> "+Snake.U);
            return Snake.U;
        }
        logState("DOWN");

        if (canMoveDown()) {
LOG.debug("DOWN: YES");
            if (tPhase == 2 && pos.y == yMin + 1) {
                tPhase = 1;
                state = Snake.RIGHT;
                return moveRight();
            } else {
                return Snake.D;
            }
        } else {
LOG.debug("DOWN: NO");
            // can't move...
            if (tPhase > 0) {
                state = Snake.RIGHT;
                return moveRight();
            } else {
                if(pos.x < xMax/2 || cmdChain.contains(Snake.LEFT)) {
                    state = Snake.RIGHT;
                    return moveRight();
                } else {
                    state = Snake.LEFT;
                    return moveLeft();
                }
            }
        }
    }

    private boolean canMoveLeft(){
        try {
            return  pos.x > xMin
                    && myBody[pos.y][pos.x - 1] == 0
                    && enemyBodies[pos.y][pos.x - 1] == 0
                    && (enterDangerZone || enemyNextMovePossibleLocations[pos.y][pos.x - 1] < len);
            //&& (pos.y < 2 || enemyHeads[pos.y - 2][pos.x] < len)
        }catch(IndexOutOfBoundsException e){
            LOG.info("IoB @ canMoveLeft check...", e);
            return false;
        }
    }

    public String moveLeft() {
        if(cmdChain.size() < 4 && cmdChain.contains(Snake.LEFT)){
            return moveUp();
        }else {
            if (checkDoomed(Snake.LEFT)) {
                //LOG.debug("LEFT: DOOMED -> "+Snake.R);
                return Snake.R;
            }
            logState("LEFT");
            if (canMoveLeft()) {
LOG.debug("LEFT: YES");
                // even if we "could" move to left - let's check, if we should/will follow our program...
                if (pos.x == xMin + 1) {
                    // We are at the left-hand "border" side of the board
                    if (tPhase != 2) {
                        tPhase = 1;
                    }
                    if (pos.y == yMax) {
                        state = Snake.DOWN;
                        return Snake.L;
                    } else {
                        if(canMoveUp()) {
                            state = Snake.RIGHT;
                            return moveUp();// U;
                        } else {
                            return Snake.L;
                        }
                    }
                } else {
                    if ((yMax - pos.y) % 2 == 1) {
                        // before we instantly decide to go up - we need to check, IF we can GO UP (and if not,
                        // we simply really move to the LEFT (since we can!))
                        if (canMoveUp()) {
                            tPhase = 2;
                            return moveUp();
                        } else {
                            return Snake.L;
                        }
                    } else {
                        return Snake.L;
                    }
                }
            } else {
                // can't move...
LOG.debug("LEFT: NO");
                // if we are in the pending mode, we prefer to go ALWAYS UP
                return decideForUpOrDownUsedFromMoveLeftOrRight(Snake.LEFT);
            }


            /*boolean canMoveLeft = pos.x > xMin;
            boolean isSpaceToLeft = false;
            if(pos.x > 0) {
                isSpaceToLeft = myBody[pos.y][pos.x - 1] == 0
                                && enemyBodies[pos.y][pos.x - 1] == 0
                                && (enterDangerZone || enemyNextMovePossibleLocations[pos.y][pos.x - 1] < len)
                //&& (pos.x < 2 || enemyHeads[pos.y][pos.x - 2] < len)
                ;
            }
            if (canMoveLeft && (isSpaceToLeft || tPhase > 0)) {
                if (pos.x == xMin + 1) {
                    if (pos.y == yMax) {
                        if (tPhase != 2) {
                            tPhase = 1;
                        }
                        state = Snake.DOWN;
                        // TODO check if we can MOVE LEFT
                        return Snake.L;
                    } else {
                        if (tPhase != 2) {
                            tPhase = 1;
                        }
                        state = Snake.RIGHT;
                        // check if we can MOVE UP
                        return moveUp();// U;
                    }
                } else {
                    if (isSpaceToLeft) {
                        if ( tPhase > 0 && (yMax - pos.y) % 2 == 1 ) {
                            // before we instantly decide to go up - we need to check, IF we can GO UP (and if not,
                            // we simply really move to the LEFT (since we can!))
                            if(canMoveUp()) {
                                tPhase = 2;
                                return moveUp();
                            }else{
                                return Snake.L;
                            }
                        } else {
                            return Snake.L;
                        }
                    } else {
                        return moveUp();
                    }
                }
            } else {
                // can't move...
                if(pos.y < yMax/2 || cmdChain.contains(Snake.DOWN)) {
                    state = Snake.UP;
                    return moveUp();
                } else {
                    state = Snake.DOWN;
                    return moveDown();
                }
            }*/
        }
    }

    private String decideForUpOrDownUsedFromMoveLeftOrRight(int cmd) {
        // if we are in the pending mode, we prefer to go ALWAYS-UP
        if (tPhase > 0 && !cmdChain.contains(Snake.UP)) {
            state = Snake.UP;
            return moveUp();
        }else {
            if (pos.y < yMax / 2 || cmdChain.contains(Snake.DOWN)) {
                state = Snake.UP;
                return moveUp();
            } else {
                state = Snake.DOWN;
                return moveDown();
            }
        }
    }

    private void logState(final String method) {
        //new Thread(() -> {
            LOG.info(method + " " + tPhase + " avoidBorder? "+ avoidBorder +" goDangerZone? "+ enterDangerZone +" {" + cmdChain.toString() + "}");
            LOG.info("____________________");
            for (int y = Y-1; y >= 0; y--) {
                StringBuffer b = new StringBuffer();
                b.append('|');
                for (int x = 0; x < X; x++) {
                    if (pos.x == x && pos.y == y) {
                        b.append("X");
                    } else {
                        if (myBody[y][x] == 1) {
                            b.append('x');
                        } else {
                            if (enemyBodies[y][x] > 0) {
                                if (enemyBodies[y][x] == 1) {
                                    b.append('-');
                                }else{
                                    b.append('+');
                                }
                            } else {
                                if(enemyNextMovePossibleLocations[y][x] > 0){
                                    b.append('o');
                                }else{
                                    b.append(' ');
                                }
                            }
                        }
                    }
                }
                b.append('|');
                LOG.info(b.toString());
            }
            LOG.info("--------------------");
        //}).start();
    }
}