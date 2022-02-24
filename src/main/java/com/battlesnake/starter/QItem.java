package com.battlesnake.starter;

class QItem {
    int row;
    int col;
    int dist;
    public QItem(int row, int col, int dist)
    {
        this.row = row;
        this.col = col;
        this.dist = dist;
    }

    public int getX() {
        return row;
    }

    public void setX(int row) {
        this.row = row;
    }

    public int getY() {
        return col;
    }

    public void setY(int col) {
        this.col = col;
    }

    public int getDist() {
        return dist;
    }

    public void setDist(int dist) {
        this.dist = dist;
    }
}
