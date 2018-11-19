package com.ming.processor.entity;

import java.util.Date;

public final class TblFilteredOffset {

    private String canId;

    private Date dataTime;

    private double oriValueX;

    private double oriValueY;

    private double valueX;

    private double valueY;

    public String getCanId() {
        return canId;
    }

    public void setCanId(String canId) {
        this.canId = canId;
    }

    public Date getDataTime() {
        return dataTime;
    }

    public void setDataTime(Date dataTime) {
        this.dataTime = dataTime;
    }

    public double getOriValueX() {
        return oriValueX;
    }

    public void setOriValueX(double oriValueX) {
        this.oriValueX = oriValueX;
    }

    public double getOriValueY() {
        return oriValueY;
    }

    public void setOriValueY(double oriValueY) {
        this.oriValueY = oriValueY;
    }

    public double getValueX() {
        return valueX;
    }

    public void setValueX(double valueX) {
        this.valueX = valueX;
    }

    public double getValueY() {
        return valueY;
    }

    public void setValueY(double valueY) {
        this.valueY = valueY;
    }
}
