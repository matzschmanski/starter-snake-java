package com.battlesnake.starter;

class QItem {
    int x;
    int y;
    int dist;
    public QItem(int x, int col, int dist)
    {
        this.x = x;
        this.y = col;
        this.dist = dist;
    }

    public int getX() {
        return x;
    }

    public void setX(int row) {
        this.x = row;
    }

    public int getY() {
        return y;
    }

    public void setY(int col) {
        this.y = col;
    }

    public int getDist() {
        return dist;
    }

    public void setDist(int dist) {
        this.dist = dist;
    }

    @Override
    public String toString() {
        return "QItem{" +
                "X=" + x +
                ", Y=" + y +
                ", distance=" + dist +
                '}';
    }
}
