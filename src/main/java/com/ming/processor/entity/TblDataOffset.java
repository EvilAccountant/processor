package com.ming.processor.entity;


import org.bson.BsonTimestamp;

import java.util.Date;

public final class TblDataOffset {

    private String id;

    private String bridgeId;

    private String measurePoint;

    private double offset;

    private BsonTimestamp acTime;

    private Date sendTime;

    private BsonTimestamp dataTime;

    private BsonTimestamp collectTime;

    private String dataTimeValue;

    private String collectTimeValue;

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

    public BsonTimestamp getAcTime() {
        return acTime;
    }

    public void setAcTime(BsonTimestamp acTime) {
        this.acTime = acTime;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
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
}
