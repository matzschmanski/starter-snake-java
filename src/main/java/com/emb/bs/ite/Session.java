package com.emb.bs.ite;

import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Session {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    int tPhase = 0;
    int state = 0;
    int lastFoodState = -1;

    int MAXDEEP = 0;

    int turn;

    Point myPos;
    int myLen;
    int myHealth;

    ArrayList<Integer> cmdChain = null;
    int firstMoveToTry = -1;

    int X = -1;
    int Y = -1;
    boolean doomed = false;
    ArrayList<Point> snakeHeads = null;
    int[][] snakeBodies = null;
    int[][] snakeNextMovePossibleLocations = null;
    int maxOtherSnakeLen = 0;
    int[][] myBody = null;

    private boolean goForFood = false;
    ArrayList<Point> foodPlaces = null;


    int[][] hazardZone = null;
    //ArrayList<Point> hazardNearbyPlaces = null;

    private boolean enterHazardZone = false;
    private boolean enterBorderZone = false;
    boolean enterDangerZone = false;
    boolean enterNoGoZone = false;

    boolean escapeFromBorder = false;
    boolean escapeFromHazard = false;

    private int xMin, yMin, xMax, yMax;

    boolean hungerMode = true;
    Point activeFood = null;

    boolean wrappedMode = false;

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
        enterHazardZone = false;
    }

    public void initSessionForTurn(String gameType, int height, int width) {
        Y = height;
        X = width;
        initSaveBoardBounds();
        doomed = false;
        firstMoveToTry = -1;
        cmdChain = new ArrayList<>();

        snakeHeads = new ArrayList<>();
        snakeBodies = new int[Y][X];
        snakeNextMovePossibleLocations = new int[Y][X];
        maxOtherSnakeLen = 0;

        myBody = new int[Y][X];
        MAXDEEP = myLen;//Math.min(len, 20);

        goForFood = false;
        foodPlaces = new ArrayList<>();

        hazardZone = new int[Y][X];
        //hazardNearbyPlaces = new ArrayList<>();

        escapeFromBorder = false;
        escapeFromHazard = false;

        if(gameType != null) {
            switch (gameType) {
                case "standard":
                case "royale":
                case "squad":
                    break;

                case "wrapped":
                    wrappedMode = true;
                    enterBorderZone = true;
                    setFullBoardBounds();
                    break;

                case "constrictor":
                    // NOT sure yet, if moving totally
                    // to the border is smart...
                    enterBorderZone = true;
                    hungerMode = false;
                    setFullBoardBounds();
                    break;
            }
        }else{
            // no game mode provided? [do we read from a REPLAY?!]
        }
    }

    public void initSessionAfterFullBoardRead(boolean hazardDataIsPresent) {
        // before we check any special moves, we check, if we are already on the borderline, and if this is the
        // case we can/will disable 'avoid borders' flag...

        if (!enterBorderZone) {
            if (    myPos.y == 0 ||
                    myPos.y == Y - 1 ||
                    myPos.x == 0 ||
                    myPos.x == X - 1
            ) {
                escapeFromBorder = true;
            }
        }

        if(hazardDataIsPresent) {
            if (!enterHazardZone) {
                if (hazardZone[myPos.y][myPos.x] > 0 && myHealth < 50) {
                    escapeFromHazard = true;
                }
            }
        }else{
            // there is no hazard  so we can skip the check in the array...
            enterHazardZone = true;
        }
    }

    private class DoomedCheckReply{
        boolean doomed = false;
        boolean retry = false;
    }

    private String checkFirstOnRetry(int move){
        cmdChain.clear();
        switch (firstMoveToTry){
            case Snake.UP:
                return moveUp();
            case Snake.RIGHT:
                return moveRight();
            case Snake.DOWN:
                return moveDown();
            case Snake.LEFT:
                return moveLeft();
            default:
                switch (move) {
                    case Snake.UP:
                        return moveUp();
                    case Snake.RIGHT:
                        return moveRight();
                    case Snake.DOWN:
                        return moveDown();
                    default:
                    case Snake.LEFT:
                        return moveLeft();
                }
        }
    }

    private DoomedCheckReply checkDoomed(int cmdToAdd) {
        DoomedCheckReply reply = new DoomedCheckReply();
        cmdChain.add(cmdToAdd);
        if (cmdChain.size() > 4) {
            reply.retry = true;
            cmdChain = new ArrayList<>();
            cmdChain.add(cmdToAdd);
            if (escapeFromBorder) {
                LOG.info("deactivate ESCAPE-FROM-BORDER");
                escapeFromBorder = false;
            } else if(escapeFromHazard){
                LOG.info("deactivate ESCAPE-FROM-HAZARD");
                escapeFromHazard = false;
            } else if (!enterBorderZone) {
                LOG.info("activate now GO-TO-BORDERS");
                enterBorderZone = true;
                setFullBoardBounds();
            } else if (!enterHazardZone) {
                LOG.info("activate now GO-TO-HAZARD");
                enterHazardZone = true;
            } else if(MAXDEEP > 1){
                LOG.info("activate MAXDEEP TO: "+MAXDEEP);
                MAXDEEP--;
            } else if (!enterDangerZone) {
                LOG.info("activate now GO-TO-DANGER-ZONE");
                enterDangerZone = true;
            } else if (!enterNoGoZone) {
                LOG.info("activate now GO-TO-NO-GO-ZONE");
                enterNoGoZone = true;
            } else {
                doomed = true;
                logState("DOOMED!", true);
                reply.doomed = true;
                return reply;
            }
        }
        return reply;
    }

    private int getAdvantage(){
        if( hungerMode ){
            return 8;
        }else {
            // how many foods-ahead we want to be...
            // is "one" really just enough?
            int advantage = 2;
            if (myLen > 19) {
                advantage++;
            }
            if (myLen > 24) {
                advantage++;
            }
            if (myLen > 29) {
                advantage++;
            }
            if (myLen > 39) {
                advantage++;
            }
            return advantage;
        }
    }

    public String checkSpecialMoves() {
        String killMove = checkKillMoves();
        if(killMove != null){
            LOG.info("GO FOR KILL " +killMove);
            return killMove;
        }

        if (myHealth < 31 || (myLen - getAdvantage() <= maxOtherSnakeLen)) {
            LOG.info("Check for FOOD! health:" + myHealth + " len:" + myLen +"(-"+getAdvantage()+")"+ "<=" + maxOtherSnakeLen);

            boolean resetSaveBounds = !enterBorderZone;
            boolean enterHazardZoneSaved = enterHazardZone;
            // ok we need to start to fetch FOOD!
            // we should move into the direction of the next FOOD!
            String possibleFoodMove = checkFoodMove();
            LOG.info("POSSIBLE FOOD MOVE: " + possibleFoodMove);

            if (possibleFoodMove != null) {
                return possibleFoodMove;
            } else {
                if (resetSaveBounds) {
                    // checkFoodMove() might set the MIN/MAX to the total bounds...
                    // this needs to be reset...
                    initSaveBoardBounds();
                }
                enterHazardZone = enterHazardZoneSaved;
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

    private String checkKillMoves(){
        String ret = checkForPossibleKillInDirection(Snake.UP, Snake.U);
        if(ret == null){
            ret = checkForPossibleKillInDirection(Snake.RIGHT, Snake.R);
        }
        if(ret == null){
            ret = checkForPossibleKillInDirection(Snake.DOWN, Snake.D);
        }
        if(ret == null){
            ret = checkForPossibleKillInDirection(Snake.LEFT, Snake.L);
        }
        return ret;
    }

    private int[][] generateMap(){
        int[][] aMap = new int[Y][X];
        for (int y = 0; y < X; y++) {
            for (int x = 0; x < X; x++) {
                if (myBody[y][x] > 0) {
                    aMap[y][x] = 1;
                } else{
                    if (snakeBodies[y][x] == 1) {
                        aMap[y][x] = 1;
                    }
                }
            }
        }
        return aMap;
    }

    private String checkForPossibleKillInDirection(int move, String retVal) {
        Point p = getNewPointForDirection(myPos, move);
        try {
            int val = snakeNextMovePossibleLocations[p.y][p.x];
            if (val > 0 && val < myLen) {
                switch (move){
                    case Snake.UP:
                        if(canMoveUp(myPos, generateMap(),0)){
                            return retVal;
                        }
                        break;

                    case Snake.RIGHT:
                        if(canMoveRight(myPos, generateMap(),0)){
                            return retVal;
                        }
                        break;

                    case Snake.DOWN:
                        if(canMoveDown(myPos, generateMap(),0)){
                            return retVal;
                        }
                        break;

                    case Snake.LEFT:
                        if(canMoveLeft(myPos, generateMap(),0)){
                            return retVal;
                        }
                        break;
                }

            }
        }catch(IndexOutOfBoundsException e){
        }
        return null;
    }

    private String checkFoodMove() {
        Point closestFood = null;
        int minDist = Integer.MAX_VALUE;

        // we remove all food's that are in direct area of other snakes heads
        // I don't want to battle for food with others (now)
        ArrayList<Point> availableFoods = new ArrayList<>(foodPlaces.size());
        availableFoods.addAll(foodPlaces);

        if (myHealth > 15) {
            if(! wrappedMode) {
                // food in CORNERS is TOXIC (but if we are already IN the corner we will
                // take it!
                if (!(myPos.x == 0 && myPos.y <= 1) || (myPos.x <= 1 && myPos.y == 0)) {
                    availableFoods.remove(new Point(0, 0));
                }
                if (!(myPos.x == X - 1 && myPos.y <= 1) || (myPos.x <= X - 2 && myPos.y == 0)) {
                    availableFoods.remove(new Point(0, X - 1));
                }
                if (!(myPos.x == 0 && myPos.y <= Y - 2) || (myPos.x <= 1 && myPos.y == Y - 1)) {
                    availableFoods.remove(new Point(Y - 1, 0));
                }
                if (!(myPos.x == X - 1 && myPos.y >= Y - 2) || (myPos.x >= X - 2 && myPos.y == Y - 1)) {
                    availableFoods.remove(new Point(Y - 1, X - 1));
                }
            }
            for (Point h : snakeHeads) {
                // food that is head of another snake that is longer or has
                // the same length should be ignored...
                if (snakeBodies[h.y][h.x] >= myLen){
                    availableFoods.remove(new Point(h.y + 1, h.x + 1));
                    availableFoods.remove(new Point(h.y + 1, h.x + 0));
                    availableFoods.remove(new Point(h.y + 0, h.x + 1));
                }
            }
        }

        TreeMap<Integer, ArrayList<Point>> foodTargetsByDistance = new TreeMap<>();
        for (Point f : availableFoods) {
            int dist = getFoodDistance(f, myPos);
            if(!isLocatedAtBorder(f) || dist < 3 || (dist < 4 && myHealth < 65) || myHealth < 16) {
                boolean addFoodAsTarget = true;
                for (Point h : snakeHeads) {
                    int otherSnakesDist = getFoodDistance(f, h);
                    boolean otherIsStronger = snakeBodies[h.y][h.x] >= myLen;
                    if(dist > otherSnakesDist || (dist == otherSnakesDist && otherIsStronger)) {
                        addFoodAsTarget = false;
                        break;
                    }
                }
                if(addFoodAsTarget){
                    ArrayList<Point> foodsInDist = foodTargetsByDistance.get(dist);
                    if(foodsInDist == null){
                        foodsInDist = new ArrayList<>();
                        foodTargetsByDistance.put(dist, foodsInDist);
                    }
                    foodsInDist.add(f);
                }
            }
        }

        if(foodTargetsByDistance.size() > 0){
            // get the list of the closest food...
            ArrayList<Point> closestFoodList = foodTargetsByDistance.get(foodTargetsByDistance.firstKey());
            if(closestFoodList.size() == 1){
                // cool only one
                closestFood = closestFoodList.get(0);
            } else {
                // ok we have to decide which of the foods in the same distance can be caught
                // most easily
                int minBlocks = Integer.MAX_VALUE;

                // ok take the first as default...
                closestFood = closestFoodList.get(0);

                // TODO: count blockingBlocks in WRAPPED MODE
                if(!wrappedMode) {
                    // need to decided which food is better?!
                    for (Point cfp : closestFoodList) {
                        int blocks = countBlockingsBetweenFoodAndHead(cfp);
                        minBlocks = Math.min(minBlocks, blocks);
                        if (minBlocks == blocks) {
                            closestFood = cfp;
                        } else {
                            LOG.info("FOOD at " + cfp + " blocked by " + blocks + " - stay on: " + closestFood + "(blocked by " + minBlocks + ")");
                        }
                    }
                }
            }

            goForFood = true;
            if (!enterBorderZone) {
                if(isLocatedAtBorder(closestFood)){
                    enterBorderZone = true;
                    escapeFromBorder = false;
                    setFullBoardBounds();
                }
            }

            //TODO: check if we have to do something special about hazardZone?!
            //and the hazardZone flags 'escapeFromHazard' & 'enterHazardZone'

            if(activeFood == null || !activeFood.equals(closestFood)){
                lastFoodState = -1;
            }
            activeFood = closestFood;

            LOG.info("TRY TO GET FOOD: at: " + closestFood +" moving: "+getMoveIntAsString(lastFoodState));
            // TODO:
            // here we have to find a smarter way to decide, in which direction we should
            // go to approach the food -> since currently me are blocked by ourselves

            int yDelta = myPos.y - closestFood.y;
            int xDelta = myPos.x - closestFood.x;
            if (lastFoodState == -1 || yDelta == 0 || xDelta == 0) {
                if (Math.abs(yDelta) > Math.abs(xDelta)) {
                    if(wrappedMode && Math.abs(yDelta) > Y/2) {
                        lastFoodState = Snake.UP;
                    } else if (yDelta > 0) {
                        lastFoodState = Snake.DOWN;
                    } else {
                        lastFoodState = Snake.UP;
                    }
                } else {
                    if((wrappedMode && Math.abs(xDelta) > X/2)){
                        lastFoodState = Snake.RIGHT;
                    }else if (xDelta > 0) {
                        lastFoodState = Snake.LEFT;
                    } else {
                        lastFoodState = Snake.RIGHT;
                    }
                }
            }

            switch (lastFoodState){
                case Snake.DOWN:
                    firstMoveToTry = Snake.DOWN;
                    return moveDown();
                case Snake.UP:
                    firstMoveToTry = Snake.UP;
                    return moveUp();
                case Snake.LEFT:
                    firstMoveToTry = Snake.LEFT;
                    return moveLeft();
                case Snake.RIGHT:
                    firstMoveToTry = Snake.RIGHT;
                    return moveRight();
            }
        } else {
            activeFood = null;
            lastFoodState = -1;
            LOG.info("NO NEARBY FOOD FOUND minDist:" + minDist + " x:" + (X / 3) + "+y:" + (Y / 3) + "=" + ((X / 3) + (Y / 3)));
        }
        return null;
    }

    private int getFoodDistance(Point food, Point other){
        if(!wrappedMode){
            return Math.abs(food.x - other.x) + Math.abs(food.y - other.y);
        }else{
            // in wrappedMode: if food.x = 0 & other.x = 11, then distance is 1
            return Math.min(Math.abs(food.x + X - other.x), Math.abs(food.x - other.x)) + Math.min(Math.abs(food.y + Y - other.y), Math.abs(food.y - other.y));
        }
    }

    private int countBlockingsBetweenFoodAndHead(Point cfp) {
        try {
            int blocks = 0;
            int yDelta = myPos.y - cfp.y;
            int xDelta = myPos.x - cfp.x;
            if (Math.abs(yDelta) > Math.abs(xDelta)) {
                if (yDelta > 0) {
                    // we need to go DOWN to the food...
                    for (int i = cfp.y + 1; i < myPos.y; i++) {
                        if (myBody[i][myPos.x] > 0 || snakeBodies[i][myPos.x] > 0) {
                            blocks++;
                        }
                    }
                } else {
                    // we need to go UP to the food...
                    for (int i = myPos.y + 1; i < cfp.y; i++) {
                        if (myBody[i][myPos.x] > 0 || snakeBodies[i][myPos.x] > 0) {
                            blocks++;
                        }
                    }
                }
            } else {
                if (xDelta > 0) {
                    // we need to go LEFT to the food...
                    for (int i = cfp.x + 1; i < myPos.x; i++) {
                        if (myBody[myPos.y][i] > 0 || snakeBodies[myPos.y][i] > 0) {
                            blocks++;
                        }
                    }
                } else {
                    // we need to go RIGHT to the food...
                    for (int i = myPos.x + 1; i < cfp.x; i++) {
                        if (myBody[myPos.y][i] > 0 || snakeBodies[myPos.y][i] > 0) {
                            blocks++;
                        }
                    }
                }
            }
            return blocks;
        }catch(IndexOutOfBoundsException e){
            LOG.error("IoB when try to count blocking... ");
            return Integer.MAX_VALUE;
        }
    }

    private boolean isLocatedAtBorder(Point p) {
        if(wrappedMode){
            return  false;//hazardNearbyPlaces.contains(p);
        }else {
            if(myLen < maxOtherSnakeLen){
                return  p.y == 0
                        || p.y == Y-1
                        || p.x == 0
                        || p.x == X - 1
                        //|| hazardNearbyPlaces.contains(p)
                        ;
            }else {
                return p.y <= yMin
                        || p.y >= yMax
                        || p.x <= xMin
                        || p.x >= xMax
                        //|| hazardNearbyPlaces.contains(p)
                        ;
            }
        }
    }

    Point getNewPointForDirection(Point aPos, int move){
        Point newPos = aPos.clone();
        if(wrappedMode) {
            switch (move) {
                case Snake.UP:
                    newPos.y = (newPos.y + 1) % Y;
                    break;
                case Snake.RIGHT:
                    newPos.x = (newPos.x + 1) % X;
                    break;
                case Snake.DOWN:
                    newPos.y = newPos.y > 0 ? newPos.y - 1 : Y - 1;
                    break;
                case Snake.LEFT:
                    newPos.x = newPos.x > 0 ? newPos.x - 1 : X - 1;
                    break;
            }
        }else{
            switch (move) {
                case Snake.UP:
                    newPos.y++;
                    break;
                case Snake.RIGHT:
                    newPos.x++;
                    break;
                case Snake.DOWN:
                    newPos.y--;
                    break;
                case Snake.LEFT:
                    newPos.x--;
                    break;
            }
        }
        return newPos;
    }

    private boolean willCreateLoop(int move, Point aPos, int[][] finalMap, int count) {
        // OK we have to check, if with the "planed" next move we will create a closed loop structure (either
        // with ourselves, with the border or with any enemy...
        try {
            count++;
            if(count <= MAXDEEP) {
                Point newPos = getNewPointForDirection(aPos, move);
                // simple check, if we can move from the new position to any other location

                // so in the finalMap we have the picture of the MOVE RESULT
                if(finalMap == null) {
                    finalMap = new int[Y][X];
                    finalMap[myPos.y][myPos.x] = 1;
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
                }
                finalMap[newPos.y][newPos.x] = 1;

                /*if(turn == 167){logMap(finalMap, count);}*/

                boolean noUP = !canMoveUp(newPos, finalMap, count);
                boolean noDW = !canMoveDown(newPos, finalMap, count);
                boolean noLF = !canMoveLeft(newPos, finalMap, count);
                boolean noRT = !canMoveRight(newPos, finalMap, count);

                if (noUP && noDW && noLF && noRT) {
                    return true;
                }
            }

        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ willCreateLoop " + getMoveIntAsString(move) + " check...", e);
        }
        return false;
    }

    private boolean canMoveUp() {
        try {
            if (escapeFromBorder && (myPos.x == 0 || myPos.x == X - 1)) {
                return false;
            } else if(escapeFromHazard && hazardZone[myPos.y][myPos.x] > 0){
                return false;
            } else {
                int newY = (myPos.y + 1) % Y;
                return  (wrappedMode || myPos.y < yMax)
                        && myBody[newY][myPos.x] == 0
                        && snakeBodies[newY][myPos.x] == 0
                        && (enterHazardZone || myHealth > 50 || hazardZone[newY][myPos.x] == 0)
                        && (enterDangerZone || snakeNextMovePossibleLocations[newY][myPos.x] < myLen)
                        && (enterNoGoZone || !willCreateLoop(Snake.UP, myPos, null,0));
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveUp check...", e);
            return false;
        }
    }

    private boolean canMoveUp(Point aPos, int[][] map, int c) {
        try {
            int newY = (aPos.y + 1) % Y;
            return  (wrappedMode || aPos.y < yMax)
                    && map[newY][aPos.x] == 0
                    && (enterNoGoZone || !willCreateLoop(Snake.UP, aPos, map, c))
                    ;
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveUpLoop check...", e);
            return false;
        }
    }

    public String moveUp() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.UP)) {
            // here we can generate randomness!
            return moveRight();
        } else {
            DoomedCheckReply r = checkDoomed(Snake.UP);
            if (r.doomed) {
                return Snake.REPEATLAST;
            }else if(r.retry && firstMoveToTry != -1){
                return checkFirstOnRetry(Snake.UP);
            }
            logState("UP");
            if (canMoveUp()) {
                LOG.debug("UP: YES");
                return Snake.U;
            } else {
                LOG.debug("UP: NO");
                // can't move...
                if (myPos.x < xMax / 2 || cmdChain.contains(Snake.LEFT)) {
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
            if (escapeFromBorder && (myPos.y == 0 || myPos.y == Y - 1)) {
                return false;
            } else if(escapeFromHazard && hazardZone[myPos.y][myPos.x] > 0){
                return false;
            } else {
                int newX = (myPos.x + 1) % X;
                return  (wrappedMode || myPos.x < xMax)
                        && myBody[myPos.y][newX] == 0
                        && snakeBodies[myPos.y][newX] == 0
                        && (enterHazardZone || myHealth > 50 || hazardZone[myPos.y][newX] == 0)
                        && (enterDangerZone || snakeNextMovePossibleLocations[myPos.y][newX] < myLen)
                        && (enterNoGoZone || !willCreateLoop(Snake.RIGHT, myPos, null, 0))
                        ;
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveRight check...", e);
            return false;
        }
    }

    private boolean canMoveRight(Point aPos, int[][] map, int c) {
        try {
            int newX = (aPos.x + 1) % X;
            return  (wrappedMode || aPos.x < xMax)
                    && map[aPos.y][newX] == 0
                    && (enterNoGoZone || !willCreateLoop(Snake.RIGHT, aPos, map, c))
                    ;
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveRightLoop check...", e);
            return false;
        }
    }

    public String moveRight() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.RIGHT)) {
            return moveDown();
        } else {
            DoomedCheckReply r = checkDoomed(Snake.RIGHT);
            if (r.doomed) {
                return Snake.REPEATLAST;
            }else if(r.retry && firstMoveToTry != -1){
                return checkFirstOnRetry(Snake.RIGHT);
            }
            logState("RI");
            if (canMoveRight()) {
                LOG.debug("RIGHT: YES");
                return Snake.R;
            } else {
                LOG.debug("RIGHT: NO");
                // can't move...
                if (myPos.x == xMax && tPhase > 0) {
                    if (myPos.y == yMax) {
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
            if (escapeFromBorder && (myPos.x == 0 || myPos.x == X - 1)) {
                return false;
            } else if(escapeFromHazard && hazardZone[myPos.y][myPos.x] > 0){
                return false;
            } else {
                int newY = myPos.y > 0 ? myPos.y - 1 : Y-1;
                return  (wrappedMode || myPos.y > yMin)
                        && myBody[newY][myPos.x] == 0
                        && snakeBodies[newY][myPos.x] == 0
                        && (enterHazardZone || myHealth > 50 || hazardZone[newY][myPos.x] == 0)
                        && (enterDangerZone || snakeNextMovePossibleLocations[newY][myPos.x] < myLen)
                        && (enterNoGoZone || !willCreateLoop(Snake.DOWN, myPos, null, 0))
                        ;
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveDown check...", e);
            return false;
        }
    }

    private boolean canMoveDown(Point aPos, int[][] map, int c) {
        try {
            int newY = aPos.y > 0 ? aPos.y - 1 : Y-1;
            return  (wrappedMode || aPos.y > yMin)
                    && map[newY][aPos.x] == 0
                    && (enterNoGoZone || !willCreateLoop(Snake.DOWN, aPos, map, c))
                    ;
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveDownLoop check...", e);
            return false;
        }
    }

    public String moveDown() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.DOWN)) {
            return moveLeft();
        } else {
            DoomedCheckReply r = checkDoomed(Snake.DOWN);
            if (r.doomed) {
                return Snake.REPEATLAST;
            }else if(r.retry && firstMoveToTry != -1){
                return checkFirstOnRetry(Snake.DOWN);
            }
            logState("DO");
            if (canMoveDown()) {
                LOG.debug("DOWN: YES");
                if (goForFood) {
                    return Snake.D;
                } else {
                    if (tPhase == 2 && myPos.y == yMin + 1) {
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
                    if (myPos.x < xMax / 2 || cmdChain.contains(Snake.LEFT)) {
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
            if (escapeFromBorder && (myPos.y == 0 || myPos.y == Y - 1)) {
                return false;
            } else if(escapeFromHazard && hazardZone[myPos.y][myPos.x] > 0){
                return false;
            } else {
                int newX = myPos.x > 0 ? myPos.x - 1 : X-1;
                return  (wrappedMode || myPos.x > xMin)
                        && myBody[myPos.y][newX] == 0
                        && snakeBodies[myPos.y][newX] == 0
                        && (enterHazardZone || myHealth > 50 || hazardZone[myPos.y][newX] == 0)
                        && (enterDangerZone || snakeNextMovePossibleLocations[myPos.y][newX] < myLen)
                        && (enterNoGoZone || !willCreateLoop(Snake.LEFT, myPos, null, 0))
                        ;
            }
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveLeft check...", e);
            return false;
        }
    }

    private boolean canMoveLeft(Point aPos, int[][] map, int c) {
        try {
            int newX = aPos.x > 0 ? aPos.x - 1 : X-1;
            return  (wrappedMode || aPos.x > xMin)
                    && map[aPos.y][newX] == 0
                    && (enterNoGoZone || !willCreateLoop(Snake.LEFT, aPos, map, c))
                    ;
        } catch (IndexOutOfBoundsException e) {
            LOG.info("IoB @ canMoveLeftLoop check...", e);
            return false;
        }
    }

    public String moveLeft() {
        if (cmdChain.size() < 4 && cmdChain.contains(Snake.LEFT)) {
            return moveUp();
        } else {
            DoomedCheckReply r = checkDoomed(Snake.LEFT);
            if (r.doomed) {
                return Snake.REPEATLAST;
            }else if(r.retry && firstMoveToTry != -1){
                return checkFirstOnRetry(Snake.LEFT);
            }
            logState("LE");
            if (canMoveLeft()) {
                LOG.debug("LEFT: YES");
                if (goForFood) {
                    return Snake.L;
                } else {
                    // even if we "could" move to left - let's check, if we should/will follow our program...
                    if (myPos.x == xMin + 1) {
                        // We are at the left-hand "border" side of the board
                        if (tPhase != 2) {
                            tPhase = 1;
                        }
                        if (myPos.y == yMax) {
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
                        if ((yMax - myPos.y) % 2 == 1) {
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
                if (myPos.x == xMin + 1) {
                    // We are at the left-hand "border" side of the board
                    if (tPhase != 2) {
                        tPhase = 1;
                    }
                    if (myPos.y == yMax) {
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
                    if ((yMax - myPos.y) % 2 == 1) {
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
        if (tPhase > 0 && !cmdChain.contains(Snake.UP) && myPos.y < yMax) {
            state = Snake.UP;
            return moveUp();
        } else {
            if (myPos.y < yMax / 2 || cmdChain.contains(Snake.DOWN)) {
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
            LOG.info("┌───────────┐");
        } else {
            LOG.info("┌───────────────────┐");
        }
        for (int y = Y - 1; y >= 0; y--) {
            StringBuffer b = new StringBuffer();
            b.append('│');
            for (int x = 0; x < X; x++) {
                if (myPos.x == x && myPos.y == y) {
                    b.append("X");
                } else if (myBody[y][x] == 1) {
                    b.append('c');
                } else if (snakeBodies[y][x] > 0) {
                    if (snakeBodies[y][x] == 1) {
                        b.append('+');
                    } else {
                        b.append('O');
                    }
                } else {
                    boolean isHazard = hazardZone[y][x] > 0;
                    boolean isFoodPlace = foodPlaces.contains(new Point(y, x));
                    if (snakeNextMovePossibleLocations[y][x] > 0) {
                        if(isFoodPlace){
                            b.append('●');
                        }else {
                            b.append('◦');
                        }
                    } else if (isFoodPlace) {
                        if(isHazard){
                            b.append('▓');
                        }else {
                            b.append('*');
                        }
                    } else if(isHazard){
                        b.append('▒');
                    } else {
                        b.append(' ');
                    }
                }
            }
            b.append('│');
            LOG.info(b.toString());
        }
        if (X == 11) {
            LOG.info("└───────────┘");
        } else {
            LOG.info("└───────────────────┘");
        }
    }

    void logState(final String method) {
        logState(method, false);
    }

    private void logState(String msg, boolean isDoomed) {
        msg = msg
                + " Tn:" + turn
                + " st:" + getMoveIntAsString(state).substring(0, 2).toUpperCase() + "[" + state + "]"
                + " ph:" + tPhase
                + (escapeFromHazard ? " GETOUTHAZD" : "")
                + " goHazd? " + enterHazardZone
                + (escapeFromBorder ? " GAWYBRD" : "")
                + " goBorder? " + enterBorderZone
                + " goDanger? " + enterDangerZone
                + " maxDeep? " + MAXDEEP
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
        switch (move) {
            case Snake.UP:
                return Snake.U;
            case Snake.RIGHT:
                return Snake.R;
            case Snake.DOWN:
                return Snake.D;
            case Snake.LEFT:
                return Snake.L;
            default:
                return "UNKNOWN";
        }
    }

    private void logMap(int[][] aMap, int c) {
        LOG.info("XXL TurnNo:"+turn+" MAXDEEP:"+MAXDEEP+" len:"+ myLen +" loopCount:"+c);
        if(X == 11){
            LOG.info("_____________");
        }else{
            LOG.info("_____________________");
        }
        for (int y = Y - 1; y >= 0; y--) {
            StringBuffer b = new StringBuffer();
            b.append('|');
            for (int x = 0; x < X; x++) {
                if(aMap[y][x]>0){
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
    }
}