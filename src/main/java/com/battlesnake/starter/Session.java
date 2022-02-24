package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;

public class Session {

    //private static final Logger LOG = LoggerFactory.getLogger(Session.class);
    private static final SessionLogger LOG = new SessionLogger();

    public void writeLogDataToFS() {
        LOG.write();
    }

    public void logReq(JsonNode json) {
        LOG.logReq(json);
    }

    public void logMove(String move) {
        LOG.add("=> RESULTING MOVE: " + move + " [" + state + "]");
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
    ArrayList<Point> snakeHeads = null;
    int[][] snakeBodies = null;
    int[][] snakeNextMovePossibleLocations = null;
    int maxOtherSnakeLen = 0;
    int[][] myBody = null;
    int[][] noGoArea = null;

    private boolean goForFood = false;
    ArrayList<Point> foodPlaces = null;

    private boolean enterBorderZone = false;
    private boolean enterDangerZone = false;
    private boolean enterNoGoZone = false;

    boolean escapeFromBorder = false;
    private int xMin, yMin, xMax, yMax;

    String LASTMOVE = null;

    private void setFullBoardBounds() {
        yMin = 0;
        xMin = 0;
        yMax = Y - 1;
        xMax = X - 1;
    }

    private void initSaveBoardBounds() {
        yMin = 1;
        xMin = 1;
        yMax = Y - 2;
        xMax = X - 2;
        enterDangerZone = false;
        enterNoGoZone = false;
        enterBorderZone = false;
    }

    public void initSessionForTurn(int height, int width) {
        Y = height;
        X = width;
        initSaveBoardBounds();
        doomed = false;
        cmdChain = new ArrayList<>();

        snakeHeads = new ArrayList<>();
        snakeBodies = new int[Y][X];
        snakeNextMovePossibleLocations = new int[Y][X];
        maxOtherSnakeLen = 0;

        myBody = new int[Y][X];
        noGoArea = new int[Y][X];

        goForFood = false;
        foodPlaces = new ArrayList<>();

        escapeFromBorder = false;
    }

    public void initSessionAfterFullBoardRead() {
        // before we check any special moves, we check, if we are already on the borderline, and if this is the
        // case we can/will disable 'avoid borders' flag...

        if (!enterBorderZone) {
            if (pos.y == 0 ||
                    pos.y == Y - 1 ||
                    pos.x == 0 ||
                    pos.x == X - 1
            ) {
                escapeFromBorder = true;
            }
        }

        // mark NO-GO areas
        // 1) corners are NO GO's if the x+1, y+1 fields are taken!

        Point[] possibleNoGoPoints = new Point[]{
                //new Point(  0,    1),
                //new Point(  1,    0),
                new Point(0, 0),

                //new Point(  Y-2,  X-1),
                //new Point(  Y-1,  X-2),
                new Point(Y - 1, X - 1),

                //new Point(  0,    X-2),
                //new Point(  1,    X-1),
                new Point(0, X - 1),

                //new Point(  Y-1,  1),
                //new Point(  Y-2,  0),
                new Point(Y - 1, 0)
        };

        for (Point p : possibleNoGoPoints) {
            if (isNoGo(p.y, p.x)) {
                noGoArea[p.y][p.x] = 1;
            }
        }
    }

    private boolean isNoGo2(int y, int x) {
        boolean b01 = y == Y - 1 ||
                (y < Y - 1 && (noGoArea[y + 1][x] > 0 || myBody[y + 1][x] > 0 || snakeBodies[y + 1][x] > 0));
        boolean b02 = x == X - 1 ||
                (x < X - 1 && (noGoArea[y][x + 1] > 0 || myBody[y][x + 1] > 0 || snakeBodies[y][x + 1] > 0));
        boolean b03 = (y == Y - 1 && x == X - 1) ||
                (y < Y - 1 && x < X - 1 && (noGoArea[y + 1][x + 1] > 0 || snakeBodies[y + 1][x + 1] > 0));

        boolean b11 = y == 0 ||
                (y > 0 && (noGoArea[y - 1][x] > 0 || myBody[y - 1][x] > 0 || snakeBodies[y - 1][x] > 0));
        boolean b12 = x == 0 ||
                (x > 0 && (noGoArea[y][x - 1] > 0 || myBody[y][x - 1] > 0 || snakeBodies[y][x - 1] > 0));
        boolean b13 = (y == 0 && x == 0) ||
                (y > 0 && x > 0 && (noGoArea[y - 1][x - 1] > 0 || snakeBodies[y - 1][x - 1] > 0));

        return b01 && b02 && b03 && b11 && b12 && b13;
    }

    // THIS IS JUST A MESS!!!
    private boolean isNoGo(int y, int x) {
        if (y == 0 && x == 0) {
            // 0/0
            boolean b01 = y < Y && (noGoArea[y + 1][x] > 0 || myBody[y + 1][x] > 0 || snakeBodies[y + 1][x] > 0);
            boolean b02 = x < X && (noGoArea[y][x + 1] > 0 || myBody[y][x + 1] > 0 || snakeBodies[y][x + 1] > 0);
            boolean b03 = y < Y && x < X && (noGoArea[y + 1][x + 1] > 0 || myBody[y + 1][x + 1] > 0 || snakeBodies[y + 1][x + 1] > 0);
            return b01 && b02 && b03;
        } else if (y == 0) {
            // 0/X
            boolean b21 = y < Y && (noGoArea[y + 1][x] > 0 || myBody[y + 1][x] > 0 || snakeBodies[y + 1][x] > 0);
            boolean b22 = x > 0 && (noGoArea[y][x - 1] > 0 || myBody[y][x - 1] > 0 || snakeBodies[y][x - 1] > 0);
            boolean b23 = y < Y && x > 0 && (noGoArea[y + 1][x - 1] > 0 || myBody[y + 1][x - 1] > 0 || snakeBodies[y + 1][x - 1] > 0);
            return b21 && b22 && b23;
        } else if (x == 0) {
            // Y/0
            boolean b31 = y > 0 && (noGoArea[y - 1][x] > 0 || myBody[y - 1][x] > 0 || snakeBodies[y - 1][x] > 0);
            boolean b32 = x < X && (noGoArea[y][x + 1] > 0 || myBody[y][x + 1] > 0 || snakeBodies[y][x + 1] > 0);
            boolean b33 = y > 0 && x < X && (noGoArea[y - 1][x + 1] > 0 || myBody[y - 1][x + 1] > 0 || snakeBodies[y - 1][x + 1] > 0);
            return b31 && b32 && b33;
        } else {
            // Y/X
            boolean b11 = y > 0 && (noGoArea[y - 1][x] > 0 || myBody[y - 1][x] > 0 || snakeBodies[y - 1][x] > 0);
            boolean b12 = x > 0 && (noGoArea[y][x - 1] > 0 || myBody[y][x - 1] > 0 || snakeBodies[y][x - 1] > 0);
            boolean b13 = y > 0 && x > 0 && (noGoArea[y - 1][x - 1] > 0 || myBody[y - 1][x - 1] > 0 || snakeBodies[y - 1][x - 1] > 0);
            return b11 && b12 && b13;
        }
    }

    private boolean checkDoomed(int cmdToAdd) {
        cmdChain.add(cmdToAdd);
        if (cmdChain.size() > 4) {
            if (escapeFromBorder) {
                LOG.info("activate STAY-ON-BORDER");
                escapeFromBorder = false;
                cmdChain = new ArrayList<>();
                cmdChain.addAll(movesToIgnore);
                cmdChain.add(cmdToAdd);
            } else if (!enterBorderZone) {
                LOG.info("activate now GO-TO-BORDERS");
                enterBorderZone = true;
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
            } else if (!enterNoGoZone) {
                LOG.info("activate now GO-TO-NO-GO-ZONE");
                enterNoGoZone = true;
                cmdChain = new ArrayList<>();
                cmdChain.addAll(movesToIgnore);
                cmdChain.add(cmdToAdd);
            } else {
                doomed = true;
                logState("DOOMED!", true);
                return true;
            }
        }
        return false;
    }

    public String checkSpecialMoves(boolean ignoreFood) {
        if (!ignoreFood && (health < 31 || (len <= maxOtherSnakeLen))) {
            LOG.info("Check for FOOD! health:" + health + " len:" + len + "<=" + maxOtherSnakeLen);

            // ok we need to start to fetch FOOD!
            // we should move into the direction of the next FOOD!
            String possibleFoodMove = checkFoodMove();
            LOG.info("POSSIBLE FOOD MOVE: " + possibleFoodMove);
            if (possibleFoodMove != null) {
                return possibleFoodMove;
            } else {
                // checkFoodMove() might set the MIN/MAX to the total bounds...
                // this needs to be reset...
                initSaveBoardBounds();
            }
        }

        // REMOVED cause after REFACTOR moveLeft not needed ?!
        // if we are in the UPPER-ROW and the x=0 is free, let's move to the LEFT!
        /*if (tPhase > 0 && pos.y == yMax && pos.x < xMax / 3) {
            if (pos.x > xMax) {
                LOG.info("SPECIAL MOVE -> LEFT CALLED");
                return moveLeft();
            } else {
                LOG.info("SPECIAL MOVE -> DOWN CALLED");
                return moveDown();
            }
        }*/
        return null;
    }

    private String checkFoodMove() {
        Point closestFood = null;
        int minDist = Integer.MAX_VALUE;

        // we remove all food's that are in direct area of other snakes heads
        // I don't want to battle for food with others (now)
        ArrayList<Point> availableFoods = new ArrayList<>(foodPlaces.size());
        availableFoods.addAll(foodPlaces);
        if (health > 15) {
            for (int i = 1; i <= 2; i++) {
                for (Point h : snakeHeads) {
                    availableFoods.remove(new Point(h.y - i, h.x - i));
                    availableFoods.remove(new Point(h.y - i, h.x));
                    availableFoods.remove(new Point(h.y - i, h.x + i));

                    availableFoods.remove(new Point(h.y + i, h.x - i));
                    availableFoods.remove(new Point(h.y + i, h.x));
                    availableFoods.remove(new Point(h.y + i, h.x + i));

                    availableFoods.remove(new Point(h.y, h.x - i));
                    availableFoods.remove(new Point(h.y, h.x + i));
                }
            }
        }

        for (Point f : availableFoods) {
            int dist = Math.abs(f.x - pos.x) + Math.abs(f.y - pos.y);
            minDist = Math.min(minDist, dist);
            if (minDist == dist) {
                closestFood = f;
            }
        }

        if (closestFood != null && minDist <= X / 3 + Y / 3) {
            goForFood = true;
            if (!enterBorderZone) {
                if (closestFood.y == 0 ||
                        closestFood.y == Y - 1 ||
                        closestFood.x == 0 ||
                        closestFood.x == X - 1
                ) {
                    enterBorderZone = true;
                    escapeFromBorder = false;
                    setFullBoardBounds();
                }
            }

            LOG.info("TRY TO GET FOOD: at: " + closestFood);
            // TODO:
            // here we have to find a smarter way to decide, in which direction we should
            // go to approach the food -> since currently this causing quite often "self-loops"
            if (pos.x > closestFood.x) {
                return moveLeft();
            } else if (pos.x < closestFood.x) {
                return moveRight();
            } else if (pos.y > closestFood.y) {
                return moveDown();
            } else {
                return moveUp();
            }
        } else {
            LOG.info("NO NEARBY FOOD FOUND minDist:" + minDist + " x:" + (X / 3) + "+y:" + (Y / 3) + "=" + ((X / 3) + (Y / 3)));
        }
        return null;
    }

    private boolean willCreateLoop(int move) {
        // OK we have to check, if with the "planed" next move we will create a closed loop structure (either
        // with ourselves, with the border or with any enemy...
        try {
            /*

            Point resPos = pos.clone();
            switch (move){
                case Snake.UP:
                    resPos.y++;
                    break;
                case Snake.RIGHT:
                    resPos.x++;
                    break;
                case Snake.DOWN:
                    resPos.y--;
                    break;
                case Snake.LEFT:
                    resPos.x--;
                    break;
            }
            int[][] finalMap = new int[Y][X];
            finalMap[resPos.y][resPos.x] = 1;
            finalMap[pos.y][pos.x] = 1;
            for (int y = 0; y < X; y++) {
                for (int x = 0; x < X; x++) {
                    if (myBody[y][x] > 0) {
                        finalMap[y][x] = 1;
                    } else if (snakeBodies[y][x] > 0) {
                        finalMap[y][x] = 1;
                    } else if (snakeNextMovePossibleLocations[y][x] > 0) {
                        finalMap[y][x] = 1;
                    }
                }
            }
            // so in the finalMap we have the picture of the MOVE RESULT



            if(X == 11){
                LOG.info("_____________");
            }else{
                LOG.info("_____________________");
            }
            for (int y = Y - 1; y >= 0; y--) {
                StringBuffer b = new StringBuffer();
                b.append('|');
                for (int x = 0; x < X; x++) {
                    if(finalMap[y][x]>0){
                        b.append('X');
                    }else{
                        b.append(' ');
                    }
                }
                b.append('|');
                LOG.info(b.toString());
            }
            if(X == 11){
                LOG.info("-------------");
            }else {
                LOG.info("---------------------");
            }

            */

        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ willCreateLoop " + getMoveIntAsString(move) + " check...", e);
            return false;
        }
        return false;
    }

    private boolean canMoveUp() {
        try {
            if (escapeFromBorder && (pos.x == 0 || pos.x == X - 1)) {
                return false;
            } else {
                return pos.y < yMax
                        && myBody[pos.y + 1][pos.x] == 0
                        && snakeBodies[pos.y + 1][pos.x] == 0
                        && (enterDangerZone || snakeNextMovePossibleLocations[pos.y + 1][pos.x] < len)
                        && (enterNoGoZone || noGoArea[pos.y + 1][pos.x] == 0)
                        && !willCreateLoop(Snake.UP);
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveUp check...", e);
            return false;
        }
    }

    public String moveUp() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.UP)) {
            // here we can generate randomness!
            return moveRight();
        } else {
            if (checkDoomed(Snake.UP)) {
                return Snake.REPEATLAST;
            }
            logState("UP");
            if (canMoveUp()) {
                LOG.debug("UP: YES");
                return Snake.U;
            } else {
                LOG.debug("UP: NO");
                // can't move...
                if (pos.x < xMax / 2 || cmdChain.contains(Snake.LEFT)) {
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

    private boolean canMoveRight() {
        try {
            if (escapeFromBorder && (pos.y == 0 || pos.y == Y - 1)) {
                return false;
            } else {
                return pos.x < xMax
                        && myBody[pos.y][pos.x + 1] == 0
                        && snakeBodies[pos.y][pos.x + 1] == 0
                        && (enterDangerZone || snakeNextMovePossibleLocations[pos.y][pos.x + 1] < len)
                        && (enterNoGoZone || noGoArea[pos.y][pos.x + 1] == 0)
                        && !willCreateLoop(Snake.RIGHT);
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveRight check...", e);
            return false;
        }
    }

    public String moveRight() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.RIGHT)) {
            return moveDown();
        } else {
            if (checkDoomed(Snake.RIGHT)) {
                return Snake.REPEATLAST;
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

    private boolean canMoveDown() {
        try {
            if (escapeFromBorder && (pos.x == 0 || pos.x == X - 1)) {
                return false;
            } else {
                return pos.y > yMin
                        && myBody[pos.y - 1][pos.x] == 0
                        && snakeBodies[pos.y - 1][pos.x] == 0
                        && (enterDangerZone || snakeNextMovePossibleLocations[pos.y - 1][pos.x] < len)
                        && (enterNoGoZone || noGoArea[pos.y - 1][pos.x] == 0)
                        && !willCreateLoop(Snake.DOWN);
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveDown check...", e);
            return false;
        }
    }

    public String moveDown() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.DOWN)) {
            return moveLeft();
        } else {
            if (checkDoomed(Snake.DOWN)) {
                return Snake.REPEATLAST;
            }
            logState("DO");
            if (canMoveDown()) {
                LOG.debug("DOWN: YES");
                if (goForFood) {
                    return Snake.D;
                } else {
                    if (tPhase == 2 && pos.y == yMin + 1) {
                        tPhase = 1;
                        state = Snake.RIGHT;
                        return moveRight();
                    } else {
                        return Snake.D;
                    }
                }
            } else {
                LOG.debug("DOWN: NO");
                // can't move...
                if (tPhase > 0) {
                    state = Snake.RIGHT;
                    return moveRight();
                } else {
                    if (pos.x < xMax / 2 || cmdChain.contains(Snake.LEFT)) {
                        state = Snake.RIGHT;
                        return moveRight();
                    } else {
                        state = Snake.LEFT;
                        return moveLeft();
                    }
                }
            }
        }
    }

    private boolean canMoveLeft() {
        try {
            if (escapeFromBorder && (pos.y == 0 || pos.y == Y - 1)) {
                return false;
            } else {
                return pos.x > xMin
                        && myBody[pos.y][pos.x - 1] == 0
                        && snakeBodies[pos.y][pos.x - 1] == 0
                        && (enterDangerZone || snakeNextMovePossibleLocations[pos.y][pos.x - 1] < len)
                        && (enterNoGoZone || noGoArea[pos.y][pos.x - 1] == 0)
                        && !willCreateLoop(Snake.LEFT);
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveLeft check...", e);
            return false;
        }
    }

    public String moveLeft() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.LEFT)) {
            return moveUp();
        } else {
            if (checkDoomed(Snake.LEFT)) {
                return Snake.REPEATLAST;
            }
            logState("LE");
            if (canMoveLeft()) {
                LOG.debug("LEFT: YES");
                if (goForFood) {
                    return Snake.L;
                } else {
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
                            if (canMoveUp()) {
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
                        if (canMoveUp()) {
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
            }
        }
    }

    private String decideForUpOrDownUsedFromMoveLeftOrRight(int cmd) {
        // if we are in the pending mode, we prefer to go ALWAYS-UP
        if (tPhase > 0 && !cmdChain.contains(Snake.UP)) {
            state = Snake.UP;
            return moveUp();
        } else {
            if (pos.y < yMax / 2 || cmdChain.contains(Snake.DOWN)) {
                state = Snake.UP;
                return moveUp();
            } else {
                state = Snake.DOWN;
                return moveDown();
            }
        }
    }

    void logBoard() {
        if (X == 11) {
            LOG.info("_____________");
        } else {
            LOG.info("_____________________");
        }
        for (int y = Y - 1; y >= 0; y--) {
            StringBuffer b = new StringBuffer();
            b.append('|');
            for (int x = 0; x < X; x++) {
                if (pos.x == x && pos.y == y) {
                    b.append("X");
                } else if (myBody[y][x] == 1) {
                    b.append('c');
                } else if (snakeBodies[y][x] > 0) {
                    if (snakeBodies[y][x] == 1) {
                        b.append('-');
                    } else {
                        b.append('+');
                    }
                } else if (snakeNextMovePossibleLocations[y][x] > 0) {
                    b.append('o');
                } else if (foodPlaces.contains(new Point(y, x))) {
                    b.append('.');
                } else if (noGoArea[y][x] > 0) {
                    b.append('N');
                } else {
                    b.append(' ');
                }
            }
            b.append('|');
            LOG.info(b.toString());
        }
        if (X == 11) {
            LOG.info("-------------");
        } else {
            LOG.info("---------------------");
        }
    }

    void logState(final String method) {
        logState(method, false);
    }

    private void logState(String msg, boolean isDoomed) {
        msg = msg
                + " st:" + getMoveIntAsString(state).substring(0, 2).toUpperCase() + "[" + state + "]"
                + " ph:" + tPhase
                + (escapeFromBorder ? " GAWYBRD" : "")
                + " goBorder? " + enterBorderZone
                + " goDanger? " + enterDangerZone
                + " goNoGo? " + enterNoGoZone
                + " {" + cmdChain.toString() + "}";
        if (isDoomed) {
            LOG.error("===================================");
            LOG.error(msg);
            LOG.error("===================================");
        } else {
            LOG.info(msg);
        }
    }

    private String getMoveIntAsString(int move) {
        String txt = null;
        switch (move) {
            case Snake.UP:
                txt = Snake.U;
                break;
            case Snake.RIGHT:
                txt = Snake.R;
                break;
            case Snake.DOWN:
                txt = Snake.D;
                break;
            case Snake.LEFT:
                txt = Snake.L;
                break;
        }
        return txt;
    }

}