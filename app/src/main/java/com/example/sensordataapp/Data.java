package com.example.sensordataapp;

public class Data {

    private int time;
    private float accx, accy, accz, lat, lon, verticalAcc;



    public Data(int time, float accx, float accy, float accz, float lat, float lon, float verticalAcc) {
        this.time = time;
        this.accx = accx;
        this.accy = accy;
        this.accz = accz;
        this.lat = lat;
        this.lon = lon;
        this.verticalAcc = verticalAcc;
    }

    public int getTime() {
        return time;
    }

    public float getAccx() {
        return accx;
    }

    public float getAccy() {
        return accy;
    }

    public float getAccz() {
        return accz;
    }

    public float getAccMagnitude() {
        return (float) Math.sqrt((accx*accx) + (accy*accy) + (accz*accz));
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    public float getVerticalAcc() {
        return verticalAcc;
    }
}
