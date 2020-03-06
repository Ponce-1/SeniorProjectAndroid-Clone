package com.parkking491prototype.parkking;

public class StatusDot {
    private boolean parkingStatus;
    private int x;
    private int y;
    private int canvasX;
    private int canvasY;

    public StatusDot(int x, int y){
        this.x = x;
        this.y = y;
    }


    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public void setStatus(boolean parkingStatus){
        this.parkingStatus = parkingStatus;
    }

    public boolean getStatus(){
        return parkingStatus;
    }

    public void setCanvasX(int canvasX){
        this.canvasX = canvasX;
    }

    public void setCanvasY(int canvasY){
        this.canvasY = canvasY;
    }

    public int getCanvasX(){
        return canvasX;
    }

    public int getCanvasY(){
        return canvasY;
    }

}