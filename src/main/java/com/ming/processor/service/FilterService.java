package com.ming.processor.service;

import Filter.Filter;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.ming.processor.entity.TblFilteredOffset;
import com.ming.processor.entity.TblOriginOffset;
import com.ming.processor.util.MyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class FilterService {

    private final static Logger LOGGER = LogManager.getLogger(FilterService.class);

    @Value("${folderPath}")
    String folderPath;

    private static final double fPass = 0.1;//通带截止频率
    private static final double fStop = 2;//阻带截止频率
    private static final double aPass = 1;//通带最大衰减
    private static final double aStop = 40;//阻带最小衰减
    private static final double fSample = 50;//采样频率

    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 读取CSV文件并提取8条有效数据
     *
     * @return
     */
    public List<String> getOriData(int canNumber) {
        File folder = new File(folderPath);
        File file = null;
        LinkedHashMap<String, String> offsetMap = new LinkedHashMap<>();
        List<String> dataList = new ArrayList<>();
        String time, canId, data;
        String date = MyUtils.getToday();//当日年月日

        int index = 0;
        try {
            File[] fileList = folder.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].length() == 0) {
                    fileList[i].delete();
                } else {
                    file = fileList[i];
                    break;
                }
            }

            if (file != null) {
                if (isCsv(file.getName())) {
                    Iterable<CSVRecord> records;
                    try (Reader in = new InputStreamReader(new FileInputStream(file), "GBK")) {
                        records = CSVFormat.EXCEL
                                .withHeader("序号", "系统时间", "时间标识", "CAN通道", "传输方向", "ID号", "帧类型", "帧格式", "长度", "数据")
                                .parse(in)
                                .getRecords();
                    }
                    for (CSVRecord record : records) {
                        if (index == 0) {
                            index++;
                            continue;
                        }

                        index++;
                        time = record.get("系统时间").substring(2, 14);
                        canId = record.get("ID号");
                        data = record.get("数据").substring(3, 26).replace(" ", "");

                        //获取8个传感器数据
                        if (!offsetMap.containsKey(canId)) {
                            offsetMap.put(canId, date + time + "@" + canId + "@" + data);
                        }

                        //检验数据是否为8个
                        if (offsetMap.size() == canNumber) {
                            //检验数据首尾差值是否在20ms以内
                            if (getJetLag(offsetMap)) {
                                file.delete();
                                LOGGER.info("已获取数据，文件已删除");
                                break;
                            } else {
                                offsetMap.clear();
                                continue;
                            }
                        }
                        fileList = folder.listFiles();
                        if (fileList.length > 1) {
                            break;
                        }
                    }
                } else {
                    System.out.println("此文件不是CSV文件！");
                }
                for (int i = fileList.length - 1; i > 0; i--) {
                    File temp = fileList[i];
                    Files.deleteIfExists(Paths.get(temp.toURI()));
                }
                if (offsetMap.size() == canNumber) {
                    for (Map.Entry<String, String> entry : offsetMap.entrySet()) {
                        dataList.add(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            return dataList;
        }
    }

//    private List<String> readCSV(int canNumber) {
//        File folder = new File(folderPath);
//        File file = null;
//        LinkedHashMap<String, String> offsetMap = new LinkedHashMap<>();
//        List<String> dataList = new ArrayList<>();
//        String time, canId, data;
//        String date = MyUtils.getToday();//当日年月日
//
//        int index = 0;
//        try {
//            if (file != null) {
//                if (isCsv(file.getName())) {
//                    Iterable<CSVRecord> records;
//                    try (Reader in = new InputStreamReader(new FileInputStream(file), "GBK")) {
//                        records = CSVFormat.EXCEL
//                                .withHeader("序号", "系统时间", "时间标识", "CAN通道", "传输方向", "ID号", "帧类型", "帧格式", "长度", "数据")
//                                .parse(in)
//                                .getRecords();
//                    }
//                    for (CSVRecord record : records) {
//                        if (index == 0) {
//                            index++;
//                            continue;
//                        }
//
//                        index++;
//                        time = record.get("系统时间").substring(2, 14);
//                        canId = record.get("ID号");
//                        data = record.get("数据").substring(3, 26).replace(" ", "");
//
//                        //获取8个传感器数据
//                        if (!offsetMap.containsKey(canId)) {
//                            offsetMap.put(canId, date + time + "@" + canId + "@" + data);
//                        }
//
//                        //检验数据是否为8个
//                        if (offsetMap.size() == canNumber) {
//                            //检验数据首尾差值是否在20ms以内
//                            if (getJetLag(offsetMap)) {
//                                break;
//                            } else {
//                                offsetMap.clear();
//                                continue;
//                            }
//                        }
//                    }
//                }
//                if (offsetMap.size() == canNumber) {
//                    for (Map.Entry<String, String> entry : offsetMap.entrySet()) {
//                        dataList.add(entry.getValue());
//                    }
//                }
//            } else {
//                System.out.println("此文件不是CSV文件！");
//            }
//        } catch (Exception e) {
//            LOGGER.error(e);
//        } finally {
//            return dataList;
//        }
//    }


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

    //判断是否是csv文件
    private boolean isCsv(String fileName) {
        return fileName.matches("^.+\\.(?i)(csv)$");
    }


    /**
     * 将数据滤波并保存
     *
     * @param offsetList
     * @param filteredList
     */
    public void throughFilter(List<TblOriginOffset> offsetList, List<TblFilteredOffset> filteredList, int canNumber,
                              LinkedHashMap<String, List<Double>> canMapOVX, LinkedHashMap<String, List<Double>> canMapOVY) {

        HashMap<String, Integer> dataXCount = new HashMap<>();
        HashMap<String, Integer> dataYCount = new HashMap<>();
        for (int i = 1; i <= canNumber; i++) {
            dataXCount.put("0x058" + i, 0);
            dataYCount.put("0x058" + i, 0);
        }

        List<double[]> tempListX = new ArrayList<>();
        List<double[]> tempListY = new ArrayList<>();
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
            idTail = Integer.valueOf(tempId.substring(5));
            TblFilteredOffset filteredOffset = new TblFilteredOffset();

            filteredOffset.setCanId(tempId);
            filteredOffset.setCollectTime(originOffset.getCollectTime());
            filteredOffset.setCollectTimeValue(originOffset.getCollectTimeValue());
            filteredOffset.setDataTime(originOffset.getDataTime());
            filteredOffset.setDataTimeValue(originOffset.getDataTimeValue());
            filteredOffset.setOriData(originOffset.getData());
            filteredOffset.setOriValueX(originOffset.getValueX());
            filteredOffset.setOriValueY(originOffset.getValueY());
            filteredOffset.setValueX(tempListX.get(idTail)[dataXCount.get(tempId)]);
            filteredOffset.setValueY(tempListY.get(idTail)[dataYCount.get(tempId)]);
            dataXCount.put(tempId, dataXCount.get(tempId)+1);
            dataYCount.put(tempId, dataYCount.get(tempId)+1);
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
        try {
            Filter filter = new Filter();
            Object[] result = filter.doFilter(1, fPass, fStop, aPass, aStop, fSample, signal);
            MWNumericArray temp = (MWNumericArray) result[0];
            double[][] weights = (double[][]) temp.toDoubleArray();
            outData = weights[0];
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return outData;
    }

    /**
     * 判断数据是否在20ms以内
     *
     * @param map
     * @return
     */
    private boolean getJetLag(LinkedHashMap<String, String> map) {
        boolean isUseful = false;
        try {
            long headTime = sdf.parse(getHead(map).getValue().split("@")[0]).getTime();
            long tailTime = sdf.parse(getTailByReflection(map).getValue().split("@")[0]).getTime();
            if (tailTime - headTime <= 20) {
                isUseful = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isUseful;
    }

    public <K, V> Map.Entry<K, V> getHead(LinkedHashMap<K, V> map) {
        return map.entrySet().iterator().next();
    }

    public <K, V> Map.Entry<K, V> getTailByReflection(LinkedHashMap<K, V> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Map.Entry<K, V>) tail.get(map);
    }

}

