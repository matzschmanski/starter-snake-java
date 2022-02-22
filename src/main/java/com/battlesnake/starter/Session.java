package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;

public class Session {

    //private static final Logger LOG = LoggerFactory.getLogger(Session.class);
    private static final SessionLogger LOG = new SessionLogger();

    public void writeLogDataToFS(){
        LOG.write();
    }
    public void logReq(JsonNode json) {
        LOG.logReq(json);
    }
    public void logMove(String move) {
        LOG.add("=> RESULTING MOVE: "+move+" ["+state+"]");
    }
    public void doLog(boolean b) {
        LOG.doIt = b;
    }

    int state = 0;
    int tPhase = 0;

    Point pos;
    int len;
    int health;
    ArrayList<Integer> cmdChain = null;
    HashSet<Integer> movesToIgnore = new HashSet<>();
    int X = -1;
    int Y = -1;
    boolean doomed = false;
    int[][] enemyBodies = null;
    int[][] enemyNextMovePossibleLocations = null;
    int[][] myBody = null;
    ArrayList<Point> foodPlaces = null;
    boolean patched = false;
    int stateToRestore = -1;
    private boolean enterDangerZone = false;
    private boolean avoidBorder = true;
    private int xMin, yMin, xMax, yMax;
    public boolean boardLogged;

    public void initSessionForTurn(int height, int width) {
        Y = height;
        X = width;
        initSaveBoardBounds();
        boardLogged = false;
        doomed = false;
        cmdChain = new ArrayList<>();
        enemyBodies = new int[Y][X];
        enemyNextMovePossibleLocations = new int[Y][X];
        myBody = new int[Y][X];
        foodPlaces = new ArrayList<>();
    }

    private void initSaveBoardBounds(){
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
        if(health < 31){
            LOG.info("NEED FOOD!");
            // ok we need to start to fetch FOOD!
            // we should move into the direction of the next FOOD!
            String possibleFoodMove = checkFoodMove();
            if(possibleFoodMove !=null){
                return possibleFoodMove;
            }
        }

        // if we are in the UPPERROW and the x=0 is free, let's move to the LEFT!
        if (tPhase > 0 && pos.y == yMax && pos.x < xMax / 3) {
            if (pos.x > 0) {
                LOG.info("SPECIAL MOVE -> LEFT CALLED");
                return moveLeft();
            } else {
                LOG.info("SPECIAL MOVE -> DOWN CALLED");
                return moveDown();
            }
        }
        return null;
    }

    private String checkFoodMove() {
        Point closestFood = null;
        int minDist = Integer.MAX_VALUE;
        for(Point f: foodPlaces){
            int dist = Math.abs( f.x - pos.x) + Math.abs( f.y - pos.y);
            minDist = Math.min(minDist, dist);
            if(minDist == dist){
                closestFood = f;
            }
        }

        if(closestFood != null && minDist <= X/3 + Y/3){
            LOG.info("TRY TO GET FOOD: at: "+closestFood);
            if(pos.x > closestFood.x){
                return moveLeft();
            }else if(pos.x < closestFood.x){
                return moveRight();
            }else if(pos.y > closestFood.y){
                return moveDown();
            }else{
                return  moveUp();
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
            logState("RI");
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
        logState("DO");

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
            logState("LE");
            if (canMoveLeft()) {
LOG.debug("LEFT: YES");
                // even if we "could" move to left - let's check, if we should/will follow our program...
                if (pos.x == xMin + 1) {
                    // We are at the left-hand "border" side of the board
                    if (tPhase != 2) {
                        tPhase = 1;
                    }
                    if (pos.y == yMax) {
//LOG.debug("LEFT: STATE down -> RETURN: LEFT");
                        state = Snake.DOWN;
                        return Snake.L;
                    } else {
                        if(canMoveUp()) {
//LOG.debug("LEFT: STATE right -> RETURN: UP");
                            state = Snake.RIGHT;
                            return moveUp();
                        } else {
//LOG.debug("LEFT: RETURN: LEFT");
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
                // IF we can't go LEFT, then we should check, if we are at our special position
                // SEE also 'YES' part (only difference is, that we do not MOVE to LEFT here!)
                if (pos.x == xMin + 1) {
                    // We are at the left-hand "border" side of the board
                    if (tPhase != 2) {
                        tPhase = 1;
                    }
                    if (pos.y == yMax) {
                        state = Snake.DOWN;
                        //return Snake.L;
                        return moveDown();

                    } else {
                        if(canMoveUp()) {
                            state = Snake.RIGHT;
                            return moveUp();
                        } else {
                            //return Snake.L;
                            return moveDown();
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
                            //return Snake.L;
                            return moveDown();
                        }
                    } else {
                        //return Snake.L;
                        // if we are in the pending mode, we prefer to go ALWAYS UP
                        return decideForUpOrDownUsedFromMoveLeftOrRight(Snake.LEFT);
                    }
                }


                // if we are in the pending mode, we prefer to go ALWAYS UP
                //return decideForUpOrDownUsedFromMoveLeftOrRight(Snake.LEFT);
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
        if(!boardLogged) {
            boardLogged = true;
            LOG.info("____________________");
            for (int y = Y - 1; y >= 0; y--) {
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
                                } else {
                                    b.append('+');
                                }
                            } else {
                                if (enemyNextMovePossibleLocations[y][x] > 0) {
                                    b.append('o');
                                } else {
                                    if(foodPlaces.contains(new Point(y,x))){
                                        b.append('.');
                                    }else {
                                        b.append(' ');
                                    }
                                }
                            }
                        }
                    }
                }
                b.append('|');
                LOG.info(b.toString());
            }
            LOG.info("--------------------");
        }

        String stateAsString = null;
        switch (state) {
            case Snake.UP:
                stateAsString = Snake.U;
                break;
            case Snake.RIGHT:
                stateAsString = Snake.R;
                break;
            case Snake.DOWN:
                stateAsString = Snake.D;
                break;
            case Snake.LEFT:
                stateAsString = Snake.L;
                break;
        }
        LOG.info(method + " moveState:" +stateAsString.substring(0,2).toUpperCase()+"["+state+"] phase:"+ tPhase + " avoidBorder? " + avoidBorder + " goDangerZone? " + enterDangerZone + " {" + cmdChain.toString() + "}");
    }
}