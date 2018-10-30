package com.ming.processor.entity;

import org.bson.BsonTimestamp;

public final class TblOriginOffset {

    private String canId;

    private String data;

    private double valueX;

    private double valueY;

    private BsonTimestamp dataTime;

    private BsonTimestamp collectTime;

    private String dataTimeValue;

    private String collectTimeValue;

    public String getCanId() {
        return canId;
    }

    public void setCanId(String canId) {
        this.canId = canId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    @Override
    public String toString() {
        return " canId : " + canId + "\n" +
                " dataTimeValue : " + dataTimeValue + "\n" +
                " collectTimeValue : " + collectTimeValue + "\n" +
                " valueX : " + valueX + " valueY : " + valueY + "\n" +
                " data : " + data + "\n";
    }
}
