package com.battlesnake.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Session {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    Point pos;
    int len;
    int health;
    int state = 0;
    int tPhase = 0;
    int X = -1;
    int Y = -1;
    int[][] enemeyBodies = null;
    int[][] enemyHeads = null;
    int[][] myBody = null;
    ArrayList<Point> foodPlaces = null;
    boolean patched = false;
    int stateToRestore = -1;
    int Xmin, Ymin, Xmax, Ymax;

    public String moveUp(boolean reset) {
        LOG.info("U");
        if (pos.y < Ymax &&
                myBody[pos.y + 1][pos.x] == 0
                && enemeyBodies[pos.y + 1][pos.x] == 0
                && (enemeyBodies.length < pos.y + 3 || enemyHeads[pos.y + 2][pos.x] < len)
        ) {
            return Snake.U;
        }else{
            state = Snake.RIGHT;
            if(reset) {
                patched = false;
                Ymax = Y - 1;
                Xmax = X - 1;
            }
            return moveRight(reset);
        }
    }

    public String moveRight(boolean reset) {
        LOG.info("R");
        if (pos.x < Xmax &&
                myBody[pos.y][pos.x + 1] == 0
                && enemeyBodies[pos.y][pos.x + 1] == 0
                && (enemeyBodies[pos.y].length < pos.x + 3 || enemyHeads[pos.y][pos.x + 2] < len)
        ) {
            return Snake.R;
        } else {
            if(pos.x == Xmax && tPhase == 1){
                state = Snake.LEFT;
                // check if we can MOVE UP?!
                return moveUp(reset);//U;
            } else {
                state = Snake.DOWN;
                if(reset) {
                    patched = false;
                    Ymax = Y - 1;
                    Xmax = X - 1;
                }
                return moveDown(reset);
            }
        }
    }

    public String moveDown(boolean reset) {
        LOG.info("D");
        if (pos.y > Ymin &&
                myBody[pos.y - 1][pos.x] == 0
                && enemeyBodies[pos.y - 1][pos.x] == 0
                && (pos.y < 2 || enemyHeads[pos.y - 2][pos.x] < len)
        ) {
            return Snake.D;
        } else {
            if(tPhase == 1){
                state = Snake.RIGHT;
                if(reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveRight(reset);
            }else {
                state = Snake.LEFT;
                if (reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveLeft(reset);
            }
        }
    }

    public String moveLeft(boolean reset) {
        LOG.info("L");
        boolean canMoveLeft = pos.x > Xmin;
        boolean isSpace = myBody[pos.y][pos.x - 1] == 0
                && enemeyBodies[pos.y][pos.x - 1] == 0
                && (pos.x < 2 || enemyHeads[pos.y][pos.x - 2] < len);

        if (canMoveLeft && (isSpace || tPhase == 1)) {
            if(pos.x == 1){
                if(pos.y == Ymax){
                    tPhase = 1;
                    state = Snake.DOWN;
                    // TODO check if we can MOVE LEFT
                    return Snake.L;
                }else {
                    tPhase = 1;
                    state = Snake.RIGHT;
                    // check if we can MOVE UP
                    return moveUp(reset);// U;
                }
            } else {
                if(isSpace) {
                    if(tPhase == 1 && (Ymax - pos.y)%2 == 1){
                        return moveUp(reset);
                    }else {
                        return Snake.L;
                    }
                }else{
                    return moveUp(reset);
                }
            }
        } else {
            state = Snake.UP;
            if (reset) {
                patched = false;
                Ymin = 0;
                Xmin = 0;
            }
            return moveUp(reset);
        }
    }
}
