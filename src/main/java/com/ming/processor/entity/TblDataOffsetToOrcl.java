package com.ming.processor.entity;

import java.sql.Timestamp;

public class TblDataOffsetToOrcl {

    //ID
    private String id;

    //BRIDGE_ID
    private String bridgeId;

    //MEASURE_POINT
    private String measurePoint;

    //OFFSET
    private double offset;

    //AC_TIME
    private Timestamp acTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getMeasurePoint() {
        return measurePoint;
    }

    public void setMeasurePoint(String measurePoint) {
        this.measurePoint = measurePoint;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public Timestamp getAcTime() {
        return acTime;
    }

    public void setAcTime(Timestamp acTime) {
        this.acTime = acTime;
    }
}
