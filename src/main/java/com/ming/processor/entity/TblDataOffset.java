package com.ming.processor.entity;


import org.bson.BsonTimestamp;

import java.util.Date;

public final class TblDataOffset {

    private String id;

    private String bridgeId;

    private String measurePoint;

    private double offset;

    private BsonTimestamp acTime;

    private String acTimeValue;

    private Date sendTime;

    private String minZone;

    private String uploaded;


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

    public String getAcTimeValue() {
        return acTimeValue;
    }

    public void setAcTimeValue(String acTimeValue) {
        this.acTimeValue = acTimeValue;
    }

    public String getMinZone() {
        return minZone;
    }

    public void setMinZone(String minZone) {
        this.minZone = minZone;
    }

    public String getUploaded() {
        return uploaded;
    }

    public void setUploaded(String uploaded) {
        this.uploaded = uploaded;
    }
}
