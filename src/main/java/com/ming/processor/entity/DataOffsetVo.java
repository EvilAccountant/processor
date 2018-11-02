package com.ming.processor.entity;

public class DataOffsetVo {

    private String minZone;

    private String measurePoint;

    private String minOffset;

    private String maxOffset;

    private String avgOffset;

    @Override
    public String toString() {
        return minZone + " " + measurePoint + " " + minOffset + " " + maxOffset + " " + avgOffset;
    }

    public String getMinZone() {
        return minZone;
    }

    public void setMinZone(String minZone) {
        this.minZone = minZone;
    }

    public String getMeasurePoint() {
        return measurePoint;
    }

    public void setMeasurePoint(String measurePoint) {
        this.measurePoint = measurePoint;
    }

    public String getMinOffset() {
        return minOffset;
    }

    public void setMinOffset(String minOffset) {
        this.minOffset = minOffset;
    }

    public String getMaxOffset() {
        return maxOffset;
    }

    public void setMaxOffset(String maxOffset) {
        this.maxOffset = maxOffset;
    }

    public String getAvgOffset() {
        return avgOffset;
    }

    public void setAvgOffset(String avgOffset) {
        this.avgOffset = avgOffset;
    }
}
