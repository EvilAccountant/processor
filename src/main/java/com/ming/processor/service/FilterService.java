package com.ming.processor.service;

import Filter.Filter;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.ming.processor.entity.TblFilteredOffset;
import com.ming.processor.entity.TblOriginOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class FilterService {

    @Value("${fPass}")
    double fPass;//通带截止频率
    @Value("${fStop}")
    double fStop;//阻带截止频率
    @Value("${aPass}")
    double aPass;//通带最大衰减
    @Value("${aStop}")
    double aStop;//阻带最小衰减
    @Value("${fSample}")
    double fSample;//采样频率

    /**
     * 将数据滤波并保存
     * X Y数据分开按ID分组，一共 canNumber*2 组数据进行滤波
     *
     * @param offsetList
     * @param filteredList
     */
    public void throughFilter(List<TblOriginOffset> offsetList, List<TblFilteredOffset> filteredList, int canNumber, String[] canIdArr) throws MWException {
        System.out.println("滤波仪式，现在开始");
        //按ID存放数据
        LinkedHashMap<String, List<Double>> canMapOVX = initCanMap(canNumber, canIdArr);
        LinkedHashMap<String, List<Double>> canMapOVY = initCanMap(canNumber, canIdArr);
        //计数MAP
        HashMap<String, Integer> dataXCount = new HashMap<>(canNumber);
        HashMap<String, Integer> dataYCount = new HashMap<>(canNumber);
        //存储滤波后数据
        List<double[]> tempListX = new ArrayList<>(canNumber);
        List<double[]> tempListY = new ArrayList<>(canNumber);
        double tempDataX, tempDataY;
        String tempId;
        int idTail;

        for (int i = 0; i < canNumber; i++) {
            dataXCount.put(canIdArr[i], 0);
            dataYCount.put(canIdArr[i], 0);
        }

        for (TblOriginOffset offset : offsetList) {
            tempId = offset.getCanId();
            tempDataX = offset.getValueX();
            tempDataY = offset.getValueY();
            canMapOVX.get(tempId).add(tempDataX);
            canMapOVY.get(tempId).add(tempDataY);
        }

        for (List<Double> oriList : canMapOVX.values()) {
            tempListX.add(lowPassFilter(oriList));
        }
        for (List<Double> oriList : canMapOVY.values()) {
            tempListY.add(lowPassFilter(oriList));
        }

        for (TblOriginOffset originOffset : offsetList) {
            tempId = originOffset.getCanId();
            idTail = Integer.valueOf(tempId.substring(5)) - 1;

            TblFilteredOffset filteredOffset = new TblFilteredOffset();
            filteredOffset.setCanId(tempId);
            filteredOffset.setDataTime(originOffset.getDataTime());
            filteredOffset.setOriValueX(originOffset.getValueX());
            filteredOffset.setOriValueY(originOffset.getValueY());
            filteredOffset.setValueX(tempListX.get(idTail)[dataXCount.get(tempId)]);
            filteredOffset.setValueY(tempListY.get(idTail)[dataYCount.get(tempId)]);
            //计数map后移一位
            dataXCount.put(tempId, dataXCount.get(tempId) + 1);
            dataYCount.put(tempId, dataYCount.get(tempId) + 1);
            filteredList.add(filteredOffset);
        }
        System.out.println("滤波仪式结束");
    }

    /**
     * 滤波处理
     *
     * @param signalList
     * @return
     */
    public double[] lowPassFilter(List<Double> signalList) throws MWException {
//        double[] signal = signalList.stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
        double[] signal = signalList.stream().mapToDouble(d -> d).toArray(); //identity function, Java unboxes automatically to get the double value

        Filter filter = new Filter();
        Object[] result = filter.doFilter(1, fPass, fStop, aPass, aStop, fSample, signal);
        MWNumericArray temp = (MWNumericArray) result[0];
        double[][] weights = (double[][]) temp.toDoubleArray();
        double[] outData = weights[0];

        Filter.disposeAllInstances();
        return outData;
    }

    /**
     * 初始化各canId的数组Map
     *
     * @return
     */
    private LinkedHashMap<String, List<Double>> initCanMap(int canNumber, String[] canArr) {
        LinkedHashMap<String, List<Double>> canMap = new LinkedHashMap<>(canNumber);
        for (int i = 0; i < canNumber; i++) {
            canMap.put(canArr[i], new ArrayList<>());
        }
        return canMap;
    }

}

