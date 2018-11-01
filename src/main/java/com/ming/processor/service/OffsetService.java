package com.ming.processor.service;

import com.ming.processor.entity.*;
import com.ming.processor.repository.TblDataOffsetRepository;
import com.ming.processor.repository.TblFilteredOffsetRepository;
import com.ming.processor.repository.TblOriginOffsetRepository;
import com.ming.processor.util.Inclinator;
import com.ming.processor.util.MyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class OffsetService {

    /**
     * 日志对象
     */
    private final static Logger LOGGER = LogManager.getLogger(OffsetService.class);

    @Autowired
    private FilterService filterService;

    @Autowired
    private TblOriginOffsetRepository tblOriginOffsetRepository;

    @Autowired
    private TblDataOffsetRepository tblDataOffsetRepository;

    @Autowired
    private TblFilteredOffsetRepository tblFilteredOffsetRepository;

    private BsonTimestamp collectTime = null;//采集时间

    private final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Value("${canIds}")
    String canIds;//倾角仪ID字符串

    @Value("${canDistance}")
    String canDistance;//倾角仪距离字符串

    @Value("${canNumber}")
    int canNumber;//倾角仪数量

    @Value("${folderPath}")
    String folderPath;

    /**
     * 读取CSV文件并提取有效数据
     *
     * @return
     */
    @Scheduled(initialDelay = 48 * 60 * 1000, fixedRate = 48 * 60 * 1000)
    public void getOriData() {
        File folder = new File(folderPath);
        File file = null;
        LinkedHashMap<String, String> offsetMap = new LinkedHashMap<>(canNumber);
        StringBuilder date = new StringBuilder(47);//当日年月日
        date.append(MyUtils.getToday());

        int index = 0;
        try {
            File[] fileList = folder.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].length() == 0) {
                    file = fileList[i];
                    file.delete();
                    continue;
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
                        String time = record.get("系统时间").substring(2, 14);
                        String canId = record.get("ID号");
                        String data = record.get("数据").substring(3, 26).replace(" ", "");

                        //获取x个传感器数据
                        if (!offsetMap.containsKey(canId)) {
                            offsetMap.put(canId, date.append(time).append("@").append(canId).append("@").append(data).toString());
                            date.delete(0, date.length());
                            date.append(MyUtils.getToday());
                        }
                        //检验数据是否为x个
                        if (offsetMap.size() == canNumber) {
                            //检验数据首尾差值是否在20ms以内
                            if (getJetLag(offsetMap)) {
//                                file.delete();
                                onReceive(offsetMap);
                                LOGGER.info("已获取一组数据");
                                offsetMap.clear();
                            } else if (!records.iterator().hasNext()) {
                                break;
                            } else {
                                offsetMap.clear();
                                continue;
                            }
                        }
                    }
                    LOGGER.info("总共收集" + index + "条数据");
                    clearFile(file);
                } else {
                    System.out.println("此文件不是CSV文件！");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * 接收到倾角仪数据
     */
    @Transactional
    public void onReceive(LinkedHashMap<String, String> offsetMap) throws ParseException {
        collectTime = new BsonTimestamp(new Date().getTime());//设置时间
        List<String> dataList = new ArrayList<>(canNumber);

        for (Map.Entry<String, String> entry : offsetMap.entrySet()) {
            dataList.add(entry.getValue());
        }

        if (dataList != null && dataList.size() == canNumber) {
            dataList.forEach(item -> System.out.println(item));
            List<TblOriginOffset> originOffsetList = toOriOffset(dataList, collectTime);
            if (!originOffsetList.isEmpty()) tblOriginOffsetRepository.insert(originOffsetList);
        }
    }

    @Transactional
    @Scheduled(fixedRate = 60 * 1000)
    public void doFilter() {
        LOGGER.info("滤波仪式 启动！");
        List<TblMeasurePointOffset> measurePointList = getMeasurePoints();
        Long time = System.currentTimeMillis() - 48 * 60 * 1000;
        String headTime = sdf.format(time - 2 * 60 * 1000 - 1);//2min
        String endTime = sdf.format(time + 1);

        List<TblOriginOffset> originOffsetList = tblOriginOffsetRepository.findByDataTimeValueBetweenOrderByDataTimeValue(headTime, endTime);
        LOGGER.info("参加滤波仪式的英灵有" + originOffsetList.size() + "位");
        List<TblFilteredOffset> filteredList = new ArrayList<>();

        filterService.throughFilter(originOffsetList, filteredList, canNumber);
        tblFilteredOffsetRepository.insert(filteredList);
        LOGGER.info("滤波仪式 完成！");

        headTime = sdf.format(time - 1.5 * 60 * 1000 - 1);//30s
        endTime = sdf.format(time - 0.5 * 60 * 1000 + 1);//1min30s
        filteredList = tblFilteredOffsetRepository.findByDataTimeValueBetweenOrderByDataTimeValue(headTime, endTime);
        HashMap<String, Double> canIdRadianMap = new HashMap<>(canNumber);
        List<TblFilteredOffset> filterTempList = new ArrayList<>(canNumber);

        for (int i = 0; i < filteredList.size(); i++) {
            filterTempList.add(filteredList.get(i));
            if (filterTempList.size() == 7) {
                //存放倾角值
                filterTempList.forEach(item -> canIdRadianMap.put(item.getCanId(), parseCoordinateRadian(item.getValueX(), item.getValueY()).getY()));
                if (canIdRadianMap.size() == measurePointList.size()) {
                    addEightPointOffset(canIdRadianMap, measurePointList, filteredList.get(i).getDataTime(), "BlackStoneBridge");
                }
                canIdRadianMap.clear();
                filterTempList.clear();
            }
        }
        LOGGER.info("挠度数据 召唤完成！");
    }

    /**
     * 挠度计算
     *
     * @param canIdRadianMap
     * @param measurePointList
     * @param acTime
     * @param bridgeId
     */
    private void addEightPointOffset(HashMap<String, Double> canIdRadianMap, List<TblMeasurePointOffset> measurePointList, BsonTimestamp acTime, String bridgeId) {
        List<Double> positionList = new ArrayList<>();
        List<Double> radianList = new ArrayList<>();
        for (TblMeasurePointOffset measurePoint : measurePointList) {
            positionList.add(measurePoint.getPosition());
            radianList.add(canIdRadianMap.get(measurePoint.getCanId()));
        }
        //计算8分点挠度，同时保存计算结果
        List<TblDataOffset> offsetList = Inclinator.getEightPointDeflection(positionList, radianList);
        //只保留四分点和跨中
        List<TblDataOffset> storeOffsetList = new ArrayList<>();
        Collections.addAll(storeOffsetList, offsetList.get(2), offsetList.get(4), offsetList.get(6));
        for (int i = 0; i < storeOffsetList.size(); i++) {
            storeOffsetList.get(i).setBridgeId(bridgeId);
            storeOffsetList.get(i).setMeasurePoint(String.format("WY-%02d-Q-01", i + 1));
            storeOffsetList.get(i).setAcTime(acTime);
            storeOffsetList.get(i).setAcTimeValue(sdf.format(acTime.getValue()));
        }
        //保存计算结果
        tblDataOffsetRepository.insert(storeOffsetList);
    }

    /**
     * 获取测量点位
     */
    private List<TblMeasurePointOffset> getMeasurePoints() {
        List<TblMeasurePointOffset> measurePointList = new ArrayList<>(canNumber);
        String[] canIdArr = canIds.split(",");
        String[] canDisArr = canDistance.split(",");
        for (int i = 0; i < canIdArr.length; i++) {
            TblMeasurePointOffset pointOffset = new TblMeasurePointOffset();
            pointOffset.setCanId(canIdArr[i]);
            pointOffset.setPosition(Double.valueOf(canDisArr[i]));
            measurePointList.add(pointOffset);
        }
        return measurePointList;
    }

    /**
     * 数据字符串转换为数据对象
     *
     * @param dataList
     * @param collectionTime
     * @return
     */
    private List<TblOriginOffset> toOriOffset(List<String> dataList, BsonTimestamp collectionTime) throws ParseException {
        if (dataList.isEmpty()) return null;
        List<TblOriginOffset> originOffsetList = new ArrayList<>(dataList.size());
        for (int i = 0; i < dataList.size(); i++) {
            //解析倾角仪数据
            TblOriginOffset offset = new TblOriginOffset();
            String[] param = dataList.get(i).split("@");
            String time = param[0];
            offset.setCanId(param[1]);
            offset.setData(param[2]);

            String xStr = offset.getData().substring(0, 8);
            String yStr = offset.getData().substring(8);
            offset.setValueX(MyUtils.decimalParse(xStr));
            offset.setValueY(MyUtils.decimalParse(yStr));
            offset.setDataTime(new BsonTimestamp(sdf.parse(time).getTime()));
            offset.setDataTimeValue(time);
            offset.setCollectTime(collectionTime);
            offset.setCollectTimeValue(sdf.format(collectionTime.getValue()));
            originOffsetList.add(offset);
        }
        return originOffsetList;
    }

    /**
     * 解析倾角值
     */
    private CoordinateRadian parseCoordinateRadian(double x, double y) {
        return new CoordinateRadian(toRadian(x), toRadian(y));
    }

    /**
     * 转为弧度
     */
    private double toRadian(double angle) {
        return angle * Math.PI / 180D;
    }

    //判断是否是csv文件
    private boolean isCsv(String fileName) {
        return fileName.matches("^.+\\.(?i)(csv)$");
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

    /**
     * 判断数据是否在20ms以内
     *
     * @param map
     * @return
     */
    private boolean getJetLag(LinkedHashMap<String, String> map) throws ParseException, NoSuchFieldException, IllegalAccessException {
        boolean isUseful = false;
        long headTime = sdf.parse(getHead(map).getValue().split("@")[0]).getTime();
        long tailTime = sdf.parse(getTailByReflection(map).getValue().split("@")[0]).getTime();
        if (tailTime - headTime <= 20) {
            isUseful = true;
        }
        return isUseful;
    }

    public void clearFile(File file) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(file);
        pw.close();
    }


}
