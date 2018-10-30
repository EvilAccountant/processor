package com.ming.processor.util;

import Jama.Matrix;
import com.ming.processor.entity.TblDataOffset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Inclinator {

    private double position;

    private double angle;

    public Inclinator(double position, double angle) {
        this.position = position;
        this.angle = angle;
    }

    public double getPosition() {
        return position;
    }

    public double getAngle() {
        return angle;
    }

    /** 根据传入的位置及理论值获取倾角对象 */
    public static List<Inclinator> of(double[] positionArray, double[] angleArray) {
        if (positionArray.length != angleArray.length) throw new RuntimeException("position array length is not equals angle array");
        List<Inclinator> results = new ArrayList<>();
        for (int i=0; i<positionArray.length; i++) {
            results.add(new Inclinator(positionArray[i], angleArray[i]));
        }
        return results;
    }

    /**
     * 计算8分点挠度值，单位毫米
     * @param positionArray  位置矩阵
     * @param angleArray     倾角矩阵
     * @return               8分点挠度矩阵
     */
    public static List<TblDataOffset> getEightPointDeflection(double[] positionArray, double[] angleArray) {
        List<TblDataOffset> offsetList = new ArrayList<>();
        //建立矩阵方程，计算挠度
        List<Inclinator> inclinatorList = Inclinator.of(positionArray, angleArray);
        Matrix A = new Matrix(Inclinator.generateParamMatrix(inclinatorList));
        Matrix B = new Matrix(Inclinator.generateConstMatrix(inclinatorList));
        Matrix X = A.solve(B);
        double[][] array = X.getArray();
        double interval = positionArray[positionArray.length-1] / 8;
        for (int i=0; i<=8; i++) {
            TblDataOffset offset = new TblDataOffset();
            double position = interval * i;
            int index = getEightPointIndex(positionArray, position);
            double a = array[(index - 1) * 4][0];
            double b = array[(index - 1) * 4 + 1][0];
            double c = array[(index - 1) * 4 + 2][0];
            double d = array[(index - 1) * 4 + 3][0];
            offset.setMeasurePoint(String.format("WY-%02d-Q-01", i+1));
            //单位为毫米保存
            offset.setOffset((a * Math.pow(position, 3) + b * Math.pow(position, 2) + c * position + d) * 1000);
            offsetList.add(offset);
        }
        return offsetList;
    }

    /**
     * 计算8分点挠度值
     * @param positionList  位置矩阵
     * @param angleList     倾角矩阵
     * @return               8分点挠度矩阵
     */
    public static List<TblDataOffset> getEightPointDeflection(List<Double> positionList, List<Double> angleList) {
        double[] positionArray = new double[positionList.size()];
        double[] angleArray = new double[angleList.size()];
        for (int i=0; i<positionArray.length; i++) {
            positionArray[i] = positionList.get(i);
            angleArray[i] = angleList.get(i);
        }
        return getEightPointDeflection(positionArray, angleArray);
    }

    /** AX=B的A */
    private static double[][] generateParamMatrix(List<Inclinator> inclinatorList) {
        //矩阵的尺寸
        int size = (inclinatorList.size() - 1) * 4;
        double[][] matrix = new double[size+inclinatorList.size()-1][size];
        //前面两行支座位移为0
        matrix[0][3] = 1;
        matrix[1][size - 1] = 1;
        matrix[1][size - 2] = inclinatorList.get(inclinatorList.size()-1).getPosition();
        matrix[1][size - 3] = Math.pow(inclinatorList.get(inclinatorList.size()-1).getPosition(), 2);
        matrix[1][size - 4] = Math.pow(inclinatorList.get(inclinatorList.size()-1).getPosition(), 3);
        //挠度连续,n-2
        for (int i=2; i<inclinatorList.size(); i++) {
            matrix[i][(i-2)*4] = Math.pow(inclinatorList.get(i-1).getPosition(), 3);
            matrix[i][(i-2)*4+1] = Math.pow(inclinatorList.get(i-1).getPosition(), 2);
            matrix[i][(i-2)*4+2] = inclinatorList.get(i-1).getPosition();
            matrix[i][(i-2)*4+3] = 1;
            matrix[i][(i-2)*4+4] = -matrix[i][(i-2)*4];
            matrix[i][(i-2)*4+5] = -matrix[i][(i-2)*4+1];
            matrix[i][(i-2)*4+6] = -matrix[i][(i-2)*4+2];
            matrix[i][(i-2)*4+7] = -matrix[i][(i-2)*4+3];
        }
        //倾角连续
        for (int i=inclinatorList.size(); i<2 * inclinatorList.size()-2; i++) {
            matrix[i][(i-inclinatorList.size())*4] = 3 * Math.pow(inclinatorList.get(i-inclinatorList.size()+1).getPosition(), 2);
            matrix[i][(i-inclinatorList.size())*4+1] = 2 * inclinatorList.get(i-inclinatorList.size()+1).getPosition();
            matrix[i][(i-inclinatorList.size())*4+2] = 1;
            matrix[i][(i-inclinatorList.size())*4+4] = -matrix[i][(i-inclinatorList.size())*4];
            matrix[i][(i-inclinatorList.size())*4+5] = -matrix[i][(i-inclinatorList.size())*4+1];
            matrix[i][(i-inclinatorList.size())*4+6] = -matrix[i][(i-inclinatorList.size())*4+2];
        }
        //曲率连续
        for (int i=2 * inclinatorList.size()-2; i<3 * inclinatorList.size()-4; i++) {
            matrix[i][(i-2 * inclinatorList.size()+2)*4] = 6 * inclinatorList.get(i-2 * inclinatorList.size()+3).getPosition();
            matrix[i][(i-2 * inclinatorList.size()+2)*4+1] = 2;
            matrix[i][(i-2 * inclinatorList.size()+2)*4+4] = -matrix[i][(i-2 * inclinatorList.size()+2)*4];
            matrix[i][(i-2 * inclinatorList.size()+2)*4+5] = -matrix[i][(i-2 * inclinatorList.size()+2)*4+1];
        }
        //实测倾角值
        for (int i=3*inclinatorList.size()-4; i<4*inclinatorList.size()-4; i++) {
            //最后一个值适用于最后一个倾角仪
            if (i < 4*inclinatorList.size()-5) {
                matrix[i][(i - 3 * inclinatorList.size() + 4) * 4] = 3 * Math.pow(inclinatorList.get(i - 3 * inclinatorList.size() + 4).getPosition(), 2);
                matrix[i][(i - 3 * inclinatorList.size() + 4) * 4 + 1] = 2 * inclinatorList.get(i - 3 * inclinatorList.size() + 4).getPosition();
                matrix[i][(i - 3 * inclinatorList.size() + 4) * 4 + 2] = 1;
            } else {
                matrix[i][(i - 3 * inclinatorList.size() + 3) * 4] = 3 * Math.pow(inclinatorList.get(i - 3 * inclinatorList.size() + 4).getPosition(), 2);
                matrix[i][(i - 3 * inclinatorList.size() + 3) * 4 + 1] = 2 * inclinatorList.get(i - 3 * inclinatorList.size() + 4).getPosition();
                matrix[i][(i - 3 * inclinatorList.size() + 3) * 4 + 2] = 1;
            }
        }
        //各跨径跨中倾角修正
        double[] avgCol = new double[inclinatorList.size()-1];
        for (int i=0; i<inclinatorList.size()-1; i++) {
            avgCol[i] = (inclinatorList.get(i).getPosition() + inclinatorList.get(i+1).getPosition()) / 2;
        }
        for (int i=0; i<avgCol.length; i++) {
            matrix[4 * inclinatorList.size() - 4 + i][4 * i] = 3 * Math.pow(avgCol[i], 2);
            matrix[4 * inclinatorList.size() - 4 + i][4 * i + 1] = 2 * avgCol[i];
            matrix[4 * inclinatorList.size() - 4 + i][4 * i + 2] = 1;
        }
        return matrix;
    }

    /** AX=B的B */
    private static double [][] generateConstMatrix(List<Inclinator> inclinatorList) {
        int size = (inclinatorList.size()-1) * 5;
        double[][] matrix = new double[size][1];
        for (int i=(inclinatorList.size()-1) * 4-1; i>(inclinatorList.size()-1) * 3-2; i--) {
            matrix[i][0] = inclinatorList.get(i-(inclinatorList.size()-1) * 3+1).getAngle();
        }
        //各跨径跨中倾角修正
        for (int i=0; i<inclinatorList.size()-1; i++) {
            matrix[(inclinatorList.size()-1) * 4+i][0] = getMidSpanParamMatrix(inclinatorList, (inclinatorList.get(i).getPosition() + inclinatorList.get(i+1).getPosition()) / 2D);
        }
        return matrix;
    }

    /** 计算各跨跨中B矩阵 */
    private static double getMidSpanParamMatrix(List<Inclinator> inclinatorList, double position) {
        //获取一元三次方程参数
        double[] tempPositions = new double[3];
        double[] tempAngles = new double[3];
        for (int i=0; i<inclinatorList.size(); i++) {
            if (position < inclinatorList.get(i).getPosition()) {
                if ((i != 0 && (i % 2) == 0) || ((i % 2) != 0 && i == inclinatorList.size()-1)) {
                    tempPositions[0] = inclinatorList.get(i-2).getPosition();
                    tempPositions[1] = inclinatorList.get(i-1).getPosition();
                    tempPositions[2] = inclinatorList.get(i).getPosition();
                    tempAngles[0] = inclinatorList.get(i-2).getAngle();
                    tempAngles[1] = inclinatorList.get(i-1).getAngle();
                    tempAngles[2] = inclinatorList.get(i).getAngle();
                } else if ((i % 2) != 0 && i < inclinatorList.size()-1){
                    tempPositions[0] = inclinatorList.get(i-1).getPosition();
                    tempPositions[1] = inclinatorList.get(i).getPosition();
                    tempPositions[2] = inclinatorList.get(i+1).getPosition();
                    tempAngles[0] = inclinatorList.get(i-1).getAngle();
                    tempAngles[1] = inclinatorList.get(i).getAngle();
                    tempAngles[2] = inclinatorList.get(i+1).getAngle();
                }
                break;
            }
        }
        //求解abc
        double[][] X_ARR = new double[3][3];
        double[][] B_ARR = new double[3][1];
        for (int i=0; i <3; i++) {
            X_ARR[i][0] = Math.pow(tempPositions[i], 2);
            X_ARR[i][1] = tempPositions[i];
            X_ARR[i][2] = 1;
            B_ARR[i][0] = tempAngles[i];
        }
        Matrix X = new Matrix(X_ARR);
        Matrix B = new Matrix(B_ARR);
        double[] A_ARR = X.solve(B).getColumnPackedCopy();
        return A_ARR[0] * Math.pow(position, 2) + A_ARR[1] * position + A_ARR[2];
    }

    /** 判断8分点的哪一段 */
    private static int getEightPointIndex(double[] points, double targetPoint) {
        Arrays.sort(points);
        for (int i=0; i<points.length; i++) {
            if (targetPoint >= points[i]) {
                if (i<points.length-1 && targetPoint < points[i+1]) {
                    return i+1;
                } else if (i == points.length-1){
                    return i;
                }
            }
        }
        return -1;
    }
}
