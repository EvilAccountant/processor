package com.ming.processor.service;

import Filter.Filter;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.ming.processor.entity.TblFilteredOffset;
import com.ming.processor.entity.TblOriginOffset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class FilterService {

    private final static Logger LOGGER = LogManager.getLogger(FilterService.class);

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
     *
     * @param offsetList
     * @param filteredList
     */
    public void throughFilter(List<TblOriginOffset> offsetList, List<TblFilteredOffset> filteredList, int canNumber) {

        LinkedHashMap<String, List<Double>> canMapOVX = initCanMap(canNumber);
        LinkedHashMap<String, List<Double>> canMapOVY = initCanMap(canNumber);

        HashMap<String, Integer> dataXCount = new HashMap<>(canNumber);
        HashMap<String, Integer> dataYCount = new HashMap<>(canNumber);
        for (int i = 1; i <= canNumber; i++) {
            dataXCount.put("0x058" + i, 0);
            dataYCount.put("0x058" + i, 0);
        }

        List<double[]> tempListX = new ArrayList<>(canNumber);
        List<double[]> tempListY = new ArrayList<>(canNumber);
        String tempId;
        int idTail;
        double tempDataX, tempDataY;

        for (TblOriginOffset offset : offsetList) {
            tempId = offset.getCanId();
            tempDataX = offset.getValueX();
            tempDataY = offset.getValueY();
            canMapOVX.get(tempId).add(tempDataX);
            canMapOVY.get(tempId).add(tempDataY);
        }

        canMapOVX.forEach((k, v) -> tempListX.add(lowPassFilter(v)));
        canMapOVY.forEach((k, v) -> tempListY.add(lowPassFilter(v)));

        for (TblOriginOffset originOffset : offsetList) {
            tempId = originOffset.getCanId();
            idTail = Integer.valueOf(tempId.substring(5)) - 1;
            TblFilteredOffset filteredOffset = new TblFilteredOffset();

            filteredOffset.setCanId(tempId);
            filteredOffset.setDataTime(originOffset.getDataTime());
            filteredOffset.setDataTimeValue(originOffset.getDataTimeValue());
            filteredOffset.setOriData(originOffset.getData());
            filteredOffset.setOriValueX(originOffset.getValueX());
            filteredOffset.setOriValueY(originOffset.getValueY());
            filteredOffset.setValueX(tempListX.get(idTail)[dataXCount.get(tempId)]);
            filteredOffset.setValueY(tempListY.get(idTail)[dataYCount.get(tempId)]);
            dataXCount.put(tempId, dataXCount.get(tempId) + 1);
            dataYCount.put(tempId, dataYCount.get(tempId) + 1);
            filteredList.add(filteredOffset);
        }
    }

    /**
     * 滤波处理
     *
     * @param signalList
     * @return
     */
    public double[] lowPassFilter(List<Double> signalList) {
//        double[] signal = signalList.stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
        double[] signal = signalList.stream().mapToDouble(d -> d).toArray(); //identity function, Java unboxes automatically to get the double value
        double[] outData = new double[signal.length];

//        double fPass = 0.1;//通带截止频率
//        double fStop = 2;//阻带截止频率
//        double aPass = 1;//通带最大衰减
//        double aStop = 40;//阻带最小衰减
//        double fSample = 50;//采样频率
        Filter filter = null;
        try {
            filter = new Filter();
            Object[] result = filter.doFilter(1, fPass, fStop, aPass, aStop, fSample, signal);
            MWNumericArray temp = (MWNumericArray) result[0];
            double[][] weights = (double[][]) temp.toDoubleArray();
            outData = weights[0];
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (filter != null) filter.dispose();
        }
        return outData;
    }

    /**
     * 初始化各canId的数组Map
     *
     * @return
     */
    private LinkedHashMap<String, List<Double>> initCanMap(int canNumber) {
        LinkedHashMap<String, List<Double>> canMap = new LinkedHashMap<>(canNumber);
        for (int i = 1; i <= canNumber; i++) {
            canMap.put("0x058" + i, new ArrayList<>());
        }
        return canMap;
    }


//    private void readExcel() {
//        List<Double> tempList = new ArrayList<Double>();
//        try {
//            File file = new File("src/main/resources/file/testData.xlsx");
//            Workbook workbook = WorkbookFactory.create(file);
//            Sheet sheet = workbook.getSheetAt(0);
//            for (int i = 1; i < sheet.getLastRowNum() - 1; i++) {
//                Row row = sheet.getRow(i);
//                Cell cell = row.getCell(0);
//                if (cell != null) {
//                    tempList.add(cell.getNumericCellValue());
//                }
//            }
//            double[] arr = lowPassFilter(tempList);
//            outPutExcel(arr);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//
//    private void outPutExcel(double[] data) {
//        XSSFWorkbook workbook = new XSSFWorkbook();
//        XSSFSheet sheet = workbook.createSheet("testData");
//        XSSFRow row = null;
//        XSSFCell cell = null;
//        for (int i = 0; i < data.length; i++) {
//            row = sheet.createRow(i);
//            cell = row.createCell(0);
//            cell.setCellValue(data[i]);//设置值
//        }
//        OutputStream os = null;
//        String path = "C:\\Users\\Administrator\\Desktop\\dataTest.xlsx";
//        try {
//            os = new FileOutputStream(new File(path));
//            workbook.write(os);
//        } catch (FileNotFoundException e1) {
//            e1.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void main(String[] args) {
//        FilterService filterService = new FilterService();
//        filterService.readExcel();
//    }

}

