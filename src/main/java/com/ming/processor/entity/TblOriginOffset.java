package com.ming.processor.entity;

import org.bson.BsonTimestamp;

public final class TblOriginOffset {

    private String canId;

    private String data;

    private double valueX;

    private double valueY;

    private BsonTimestamp dataTime;

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

    @Override
    public String toString() {
        return " canId : " + canId + "\n" +
                " valueX : " + valueX + " valueY : " + valueY + "\n" +
                " data : " + data + "\n";
    }
}
