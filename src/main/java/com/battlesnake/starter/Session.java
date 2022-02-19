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
    int[][] enemyBodies = null;
    int[][] enemyHeads = null;
    int[][] myBody = null;
    ArrayList<Point> foodPlaces = null;
    boolean patched = false;
    int stateToRestore = -1;
    int Xmin, Ymin, Xmax, Ymax;

    public String moveUp(int c, boolean reset) {
        logState("UP", c);
        if (c > 4) {
            LOG.error("WE ARE DOOMED");
            return Snake.D;
        }
        if (pos.y < Ymax &&
                myBody[pos.y + 1][pos.x] == 0
                && enemyBodies[pos.y + 1][pos.x] == 0
                && enemyHeads[pos.y + 1][pos.x] == 0
            //&& (enemyHeads.length < pos.y + 3 || enemyHeads[pos.y + 2][pos.x] < len)
        ) {
            return Snake.U;
        } else {
            state = Snake.RIGHT;
            if (reset) {
                patched = false;
                Ymax = Y - 1;
                Xmax = X - 1;
            }
            return moveRight(++c, reset);
        }
    }

    public String moveRight(int c, boolean reset) {
        logState("RIGHT", c);
        if (c > 4) {
            LOG.error("WE ARE DOOMED");
            return Snake.L;
        }

        if (pos.x < Xmax &&
                myBody[pos.y][pos.x + 1] == 0
                && enemyBodies[pos.y][pos.x + 1] == 0
                && enemyHeads[pos.y][pos.x + 1] == 0
            //&& (enemyHeads[pos.y].length < pos.x + 3 || enemyHeads[pos.y][pos.x + 2] < len)
        ) {
            return Snake.R;
        } else {
            if (pos.x == Xmax && tPhase > 0) {
                state = Snake.LEFT;
                // check if we can MOVE UP?!
                return moveUp(++c, reset);//U;
            } else {
                state = Snake.DOWN;
                if (reset) {
                    patched = false;
                    Ymax = Y - 1;
                    Xmax = X - 1;
                }
                return moveDown(++c, reset);
            }
        }
    }

    public String moveDown(int c, boolean reset) {
        logState("DOWN", c);
        if (c > 4) {
            LOG.error("WE ARE DOOMED");
            return Snake.U;
        }

        if (pos.y > Ymin &&
                myBody[pos.y - 1][pos.x] == 0
                && enemyBodies[pos.y - 1][pos.x] == 0
                && enemyHeads[pos.y - 1][pos.x] == 0
            //&& (pos.y < 2 || enemyHeads[pos.y - 2][pos.x] < len)
        ) {
            if (tPhase == 2 && pos.y == 1) {
                tPhase = 1;
                state = Snake.RIGHT;
                return moveRight(++c, reset);
            } else {
                return Snake.D;
            }
        } else {
            if (tPhase > 0) {
                state = Snake.RIGHT;
                if (reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveRight(++c, reset);
            } else {
                state = Snake.LEFT;
                if (reset) {
                    patched = false;
                    Ymin = 0;
                    Xmin = 0;
                }
                return moveLeft(++c, reset);
            }
        }
    }

    public String moveLeft(int c, boolean reset) {
        logState("LEFT", c);
        if (c > 4) {
            LOG.error("WE ARE DOOMED");
            return Snake.R;
        }

        boolean canMoveLeft = pos.x > Xmin;
        boolean isSpace = myBody[pos.y][pos.x - 1] == 0
                && enemyBodies[pos.y][pos.x - 1] == 0
                && enemyHeads[pos.y][pos.x - 1] == 0
                //&& (pos.x < 2 || enemyHeads[pos.y][pos.x - 2] < len)
                ;

        if (canMoveLeft && (isSpace || tPhase > 0)) {
            if (pos.x == 1) {
                if (pos.y == Ymax) {
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
                    return moveUp(++c, reset);// U;
                }
            } else {
                if (isSpace) {
                    if (tPhase == 1 && (Ymax - pos.y) % 2 == 1) {
                        tPhase = 2;
                        return moveUp(++c, reset);
                    } else {
                        return Snake.L;
                    }
                } else {
                    return moveUp(++c, reset);
                }
            }
        } else {
            state = Snake.UP;
            if (reset) {
                patched = false;
                Ymin = 0;
                Xmin = 0;
            }
            return moveUp(++c, reset);
        }
    }

    private void logState(String method, int c) {
        LOG.info(method + " " + tPhase + " [" + c + "]");
        for (int y = Ymax; y >= 0; y--) {
            StringBuffer b = new StringBuffer();

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
                            if(enemyHeads[y][x] ==1){
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
        LOG.info("-------------------------------");
    }
}
