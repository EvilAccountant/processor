package com.ming.processor.entity;

import org.bson.BsonTimestamp;

public final class TblFilteredOffset {

    private String canId;

    private String oriData;

    private String data;

    private BsonTimestamp dataTime;

    private BsonTimestamp collectTime;

    private String dataTimeValue;

    private String collectTimeValue;

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

    public String getOriData() {
        return oriData;
    }

    public void setOriData(String oriData) {
        this.oriData = oriData;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BsonTimestamp getDataTime() {
        return dataTime;
    }

    public void setDataTime(BsonTimestamp dataTime) {
        this.dataTime = dataTime;
    }

    public BsonTimestamp getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(BsonTimestamp collectTime) {
        this.collectTime = collectTime;
    }

    public String getDataTimeValue() {
        return dataTimeValue;
    }

    public void setDataTimeValue(String dataTimeValue) {
        this.dataTimeValue = dataTimeValue;
    }

    public String getCollectTimeValue() {
        return collectTimeValue;
    }

    public void setCollectTimeValue(String collectTimeValue) {
        this.collectTimeValue = collectTimeValue;
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
