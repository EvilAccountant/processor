package com.ming.processor.service;

import com.ming.processor.entity.*;
import com.ming.processor.repository.TblDataOffsetRepository;
import com.ming.processor.repository.TblFilteredOffsetRepository;
import com.ming.processor.repository.TblOriginOffsetRepository;
import com.ming.processor.util.Inclinator;
import com.ming.processor.util.MyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OffsetService {

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    private TblOriginOffsetRepository tblOriginOffsetRepository;

    @Autowired
    private TblDataOffsetRepository tblDataOffsetRepository;

    @Autowired
    private TblFilteredOffsetRepository tblFilteredOffsetRepository;

    @Autowired
    private FilterService filterService;

    private final static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /**
     * 日志对象
     */
    private final static Logger LOGGER = LogManager.getLogger(OffsetService.class);

    /**
     * 采集时间
     */
    private BsonTimestamp collectTime = null;

    /**
     * 存储一次计算用的数据
     */
    private HashMap<String, Double> canIdRadianMap = new HashMap<>();


    @Value("${canIds}")
    String canIds;//倾角仪ID字符串


    @Value("${canDistance}")
    String canDistance;//倾角仪距离字符串

    //倾角仪数量
    private final int canNumber = canIds.split(",").length;

    /**
     * 接收到倾角仪数据
     */
    @Transactional
    @Scheduled(initialDelay = 20, fixedDelay = 20)
    public void onReceive() {
        collectTime = new BsonTimestamp(new Date().getTime());//设置时间
        List<String> dataList = filterService.getOriData(canNumber);//原始8数据
        if (dataList != null && dataList.size() == canNumber) {
            dataList.forEach(item -> System.out.println(item));
            List<TblOriginOffset> originOffsetList = toOriOffset(dataList, collectTime);
            if (!originOffsetList.isEmpty()) tblOriginOffsetRepository.insert(originOffsetList);
        }
    }

    @Transactional
    @Scheduled(initialDelay = 2 * 60 * 1000, fixedDelay = 60 * 1000)
    public void doFilter() {
        List<TblMeasurePointOffset> measurePointList = getMeasurePoints();
        collectTime = new BsonTimestamp(new Date().getTime());//设置时间
        Long time = System.currentTimeMillis();
        String headTime = sdf.format(time - 2 * 60 * 1000 - 1);//2min
        String endTime = sdf.format(time + 1);

        List<TblOriginOffset> originOffsetList = tblOriginOffsetRepository.findByCollectTimeValueBetweenOrderByCollectTimeValue(headTime, endTime);
        List<TblFilteredOffset> filteredList = new ArrayList<>();
        LinkedHashMap<String, List<Double>> canMapOVX = initCanMap();
        LinkedHashMap<String, List<Double>> canMapOVY = initCanMap();
        filterService.throughFilter(originOffsetList, filteredList, canNumber, canMapOVX, canMapOVY);
        tblFilteredOffsetRepository.insert(filteredList);
        LOGGER.info("数据已滤波并储存");

        headTime = sdf.format(time - 1.5 * 60 * 1000 - 1);//30s
        endTime = sdf.format(time - 0.5 * 60 * 1000 + 1);//1min30s
        filteredList = tblFilteredOffsetRepository.findByCollectTimeValueBetweenOrderByCollectTimeValue(headTime, endTime);
        //存放倾角值
        filteredList.forEach(item -> canIdRadianMap.put(item.getCanId(), parseCoordinateRadian(item.getValueX(), item.getValueY()).getY()));
        if (canIdRadianMap.size() == measurePointList.size()) {
            addEightPointOffset(canIdRadianMap, measurePointList, collectTime, "BlackStoneBridge");

        }
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
            storeOffsetList.get(i).setAcTime(acTime);
            storeOffsetList.get(i).setMeasurePoint(String.format("WY-%02d-Q-01", i + 1));
        }
        //保存计算结果
        tblDataOffsetRepository.insert(storeOffsetList);
        LOGGER.info("挠度数据计算完成并保存");
    }

    /**
     * 获取测量点位
     */
    private List<TblMeasurePointOffset> getMeasurePoints() {
        List<TblMeasurePointOffset> measurePointList = new ArrayList<>();
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
     * 初始化各canId的数组Map
     *
     * @return
     */
    private LinkedHashMap<String, List<Double>> initCanMap() {
        String[] canIdArr = canIds.split(",");
        LinkedHashMap<String, List<Double>> canMap = new LinkedHashMap<>();
        for (int i = 0; i < canIdArr.length; i++) {
            canMap.put(canIdArr[i], new ArrayList<>());
        }
        return canMap;
    }

    /**
     * 数据字符串转换为数据对象
     *
     * @param dataList
     * @param collectionTime
     * @return
     */
    private List<TblOriginOffset> toOriOffset(List<String> dataList, BsonTimestamp collectionTime) {
        if (dataList.isEmpty()) return null;
        List<TblOriginOffset> originOffsetList = new ArrayList<>();
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
            try {
                offset.setDataTime(new BsonTimestamp(sdf.parse(time).getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
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

}
