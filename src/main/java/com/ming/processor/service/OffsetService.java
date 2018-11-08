package com.ming.processor.service;

import com.mathworks.toolbox.javabuilder.MWException;
import com.ming.processor.entity.*;
import com.ming.processor.repository.TblDataOffsetRepository;
import com.ming.processor.repository.TblFilteredOffsetRepository;
import com.ming.processor.repository.TblOriginOffsetRepository;
import com.ming.processor.util.Inclinator;
import com.ming.processor.util.MyUtils;
import com.mongodb.MongoClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unchecked")
@EnableAsync
@Service
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

    private final MongoTemplate mongoTemplate = new MongoTemplate(new MongoClient("localhost", 27017), "processor");

    private final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final DateFormat sdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Value("${bridgeId}")
    String bridgeId;//桥梁id
    @Value("${canIds}")
    String canIds;//倾角仪ID字符串
    @Value("${canDistance}")
    String canDistance;//倾角仪距离字符串
    @Value("${canNumber}")
    int canNumber;//倾角仪数量

//    @Value("${canTrigger}")
//    boolean canTrigger;

    @Value("${folderPath}")
    String folderPath;//目录位置

    @Value("${oracleUrl}")
    String oracleUrl;//oracle数据库地址
    @Value("${oracleUsername}")
    String oracleUsername;//用户名
    @Value("${oraclePassword}")
    String oraclePassword;//密码

    /**
     * 读取CSV文件
     *
     * @return
     */
    @Transactional
    @Scheduled(fixedDelay = 30 * 1000)
    public void getOriData() {
        File folder = new File(folderPath);
        File file = null;
        RandomAccessFile raf;
        FileChannel channel;
        FileLock fileLock;

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
                Iterable<CSVRecord> records;
                try (Reader in = new InputStreamReader(new FileInputStream(file), "GBK")) {
                    records = CSVFormat.EXCEL
                            .withHeader("序号", "系统时间", "时间标识", "CAN通道", "传输方向", "ID号", "帧类型", "帧格式", "长度", "数据")
                            .parse(in)
                            .getRecords();
                }
                raf = new RandomAccessFile(file, "rw");
                channel = raf.getChannel();
                fileLock = channel.tryLock();

                if (records != null && ((List<CSVRecord>) records).size() > 0) {
                    analysisRecord(records);//获取数据
                }

                clearFile(file);
                fileLock.release();
                raf.close();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * 从CSV文件获取数据
     *
     * @param records
     */
    public void analysisRecord(Iterable<CSVRecord> records) throws ParseException {
        StringBuilder date = new StringBuilder(47);
        date.append(MyUtils.getToday());//当日年月日

        LinkedHashMap<String, ArrayList<String>> timeMap = new LinkedHashMap<>();
        String time, canId, data;
        int index = 0;

        for (CSVRecord record : records) {
            if (index == 0) {
                index++;
                continue;
            }
            index++;
            canId = record.get("ID号");
            time = record.get("系统时间").substring(2, 14);
            data = record.get("数据").substring(3, 26).replace(" ", "");
//            //052缺陷情况临时处理
//            if (canId.equals("0x0582") && canTrigger) {
//                continue;
//            }

            String actualTime = (date.toString() + time).substring(0, 18);
            if (!timeMap.containsKey(actualTime)) {
                timeMap.put(actualTime, new ArrayList<>());
            }
            //获取x个传感器数据
            timeMap.get(actualTime).add(date.append(time).append("@").append(canId).append("@").append(data).toString());
            date.delete(0, date.length());
            date.append(MyUtils.getToday());
        }
        buildSecMap(timeMap);//组装数据
        LOGGER.info("从CSV文件收集" + index + "条原始数据");
    }

    /**
     * 以时间为key 按id分组整理顺序
     *
     * @param timeMap
     */
    private void buildSecMap(LinkedHashMap<String, ArrayList<String>> timeMap) throws ParseException {
        ArrayList<String> tempList;
        String canId;

        for (String key : timeMap.keySet()) {
            LinkedHashMap<String, ArrayList<String>> secMap = new LinkedHashMap<>();
            tempList = timeMap.get(key);

            for (int i = 0; i < tempList.size(); i++) {
                canId = tempList.get(i).split("@")[1];
                if (!secMap.containsKey(canId)) {
                    secMap.put(canId, new ArrayList<>());
                }
                secMap.get(canId).add(tempList.get(i));
            }
            buildDataTeam(secMap);
            secMap.clear();
        }
    }

    /**
     * 按顺序组装数据
     *
     * @param secMap
     */
    private void buildDataTeam(LinkedHashMap<String, ArrayList<String>> secMap) throws ParseException {
        List<String> tempList = new ArrayList<>(canNumber);
        int length = 999;
        //获取最小长度
        for (String key : secMap.keySet()) {
            if (secMap.get(key).size() < length) {
                length = secMap.get(key).size();
            }
        }
        //组装为一组数据
        for (int i = 0; i < length; i++) {
            for (String key : secMap.keySet()) {
                tempList.add(secMap.get(key).get(i));
            }
            if (tempList.size() == canNumber) {
                onReceive(tempList);
                tempList.clear();
            }
        }
    }

    /**
     * 将数据持久化
     */
    public void onReceive(List<String> dataList) throws ParseException {
        List<TblOriginOffset> originOffsetList = toOriOffset(dataList);
        tblOriginOffsetRepository.insert(originOffsetList);
    }

    @Transactional
    @Scheduled(fixedRate = 60 * 1000)
    public void doFilter() {

        long currentTime = System.currentTimeMillis();
        long headTime = currentTime - 2 * 60 * 1000 - 1;//2min
        long endTime = currentTime + 1;
        long headTime2 = currentTime - 90 * 1000 - 1;//30s
        long endTime2 = currentTime - 30 * 1000 + 1;//1min30s

        List<TblMeasurePointOffset> measurePointList = getMeasurePoints();
        List<TblOriginOffset> originOffsetList = mongoTemplate.find(new Query(Criteria.where("dataTime.value").gte(headTime).lte(endTime)), TblOriginOffset.class);
        LOGGER.info("获取时间范围内的原始数据：" + originOffsetList.size());

        if (originOffsetList.size() > 0) {
            try {
                List<TblFilteredOffset> filteredList = new ArrayList<>();
                String[] canIdArr = canIds.split(",");
                filterService.throughFilter(originOffsetList, filteredList, canNumber, canIdArr);
                tblFilteredOffsetRepository.insert(filteredList);
                LOGGER.info("完成滤波数据" + filteredList.size() + "条");

                filteredList = mongoTemplate.find(new Query(Criteria.where("dataTime.value").gte(headTime2).lte(endTime2)), TblFilteredOffset.class);
                List<TblFilteredOffset> filterTempList = new ArrayList<>(canNumber);
                HashMap<String, Double> canIdRadianMap = new HashMap<>(canNumber);

                for (int i = 0; i < filteredList.size(); i++) {
                    filterTempList.add(filteredList.get(i));
                    if (filterTempList.size() == canNumber) {
                        //存放倾角值
                        filterTempList.forEach(item -> canIdRadianMap.put(item.getCanId(), parseCoordinateRadian(item.getValueX(), item.getValueY()).getY()));
                        if (canIdRadianMap.size() == measurePointList.size()) {
                            addEightPointOffset(canIdRadianMap, measurePointList, filteredList.get(i).getDataTime(), bridgeId);
                        }
                        canIdRadianMap.clear();
                        filterTempList.clear();
                    }
                }
                LOGGER.info("挠度换算数据完成" + filteredList.size() + "条");
            } catch (MWException e) {
                LOGGER.error(e);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * 以秒为单位取出位移值并计算最大值、最小值、平均值
     */
    @Transactional
    @Scheduled(fixedRate = 3 * 60 * 1000)
    public void getOffsetResult() {
        Query query = new Query();
        query.addCriteria(Criteria.where("uploaded").is("0"));
        Update update = Update.update("uploaded", "1");

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("uploaded").is("0")),
                Aggregation.match(Criteria.where("measurePoint").is("WY-02-Q-01")),
                Aggregation.group("minZone", "measurePoint")
                        .min("offset").as("minOffset")
                        .max("offset").as("maxOffset")
                        .avg("offset").as("avgOffset"),
                Aggregation.sort(Sort.Direction.ASC, "minZone"));
        AggregationResults<DataOffsetVo> output = mongoTemplate.aggregate(aggregation, "tblDataOffset", DataOffsetVo.class);
        LOGGER.info("聚合查询结果：" + output.getMappedResults().size() + "条");
        mongoTemplate.updateMulti(query, update, TblDataOffset.class);
        if (output.getMappedResults().size() > 0) {
            List<DataOffsetVo> voList = new ArrayList<>();
            for (Iterator<DataOffsetVo> iterator = output.getMappedResults().iterator(); iterator.hasNext(); ) {
                DataOffsetVo vo = iterator.next();
                voList.add(vo);
            }
            try {
                insertAll(transformOrcList(voList));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        mongoTemplate.remove(new Query().addCriteria(Criteria.where("uploaded").is("1")), TblDataOffset.class, "tblDataOffset");
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
        //计算挠度，同时保存计算结果
        List<TblDataOffset> offsetList = Inclinator.getEightPointDeflection(positionList, radianList);
        //只保留四分点和跨中
        List<TblDataOffset> storeOffsetList = new ArrayList<>();
        Collections.addAll(storeOffsetList, offsetList.get(2), offsetList.get(4), offsetList.get(6));
        for (int i = 0; i < storeOffsetList.size(); i++) {
            storeOffsetList.get(i).setBridgeId(bridgeId);
            storeOffsetList.get(i).setMeasurePoint(String.format("WY-%02d-Q-01", i + 1));
            storeOffsetList.get(i).setAcTime(acTime);
            storeOffsetList.get(i).setMinZone(sdfm.format(acTime.getValue()));
            storeOffsetList.get(i).setUploaded("0");
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
     * @return
     */
    private List<TblOriginOffset> toOriOffset(List<String> dataList) throws ParseException {
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
            originOffsetList.add(offset);
        }
        return originOffsetList;
    }

    /**
     * 转化为ORACLE数据库存储对象
     *
     * @param aggVoList
     * @return
     */
    private List<TblDataOffsetToOrcl> transformOrcList(List<DataOffsetVo> aggVoList) throws ParseException {
        List<TblDataOffsetToOrcl> dataOffsetList = new ArrayList<>();
        int size = aggVoList.size();
        DataOffsetVo dataOffsetVo;
        for (int i = 0; i < size; i++) {
            dataOffsetVo = aggVoList.get(i);
            for (int j = 0; j < 3; j++) {
                TblDataOffsetToOrcl orcVo = new TblDataOffsetToOrcl();
                orcVo.setId(MyUtils.generateUUID());
                orcVo.setBridgeId(bridgeId);
                orcVo.setMeasurePoint(dataOffsetVo.getMeasurePoint());
                if (j == 0) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getMinOffset()));
                }
                if (j == 1) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getMaxOffset()));
                }
                if (j == 2) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getAvgOffset()));
                }
                orcVo.setAcTime(new Timestamp(sdfm.parse(dataOffsetVo.getMinZone()).getTime()));
                dataOffsetList.add(orcVo);
            }
        }
        return dataOffsetList;
    }

    private void insertAll(List<TblDataOffsetToOrcl> offsetList) throws SQLException, ClassNotFoundException {
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String sql = "INSERT INTO TBL_DATA_OFFSET(ID,BRIDGE_ID,MEASURE_POINT,OFFSET,AC_TIME) VALUES(?,?,?,?,?)";

        try (Connection connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
             PreparedStatement preStat = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            TblDataOffsetToOrcl offset;
            for (int i = 0; i < offsetList.size(); i++) {
                offset = offsetList.get(i);
                preStat.setString(1, offset.getId());
                preStat.setString(2, offset.getBridgeId());
                preStat.setString(3, offset.getMeasurePoint());
                preStat.setDouble(4, offset.getOffset());
                preStat.setTimestamp(5, offset.getAcTime());
                preStat.addBatch();
            }
            preStat.executeBatch();
            connection.commit();
            LOGGER.info("存入ORACLE数据库" + offsetList.size());
        }

    }

    /**
     * 解析倾角值
     */
    private CoordinateRadian parseCoordinateRadian(double x, double y) {
        return new CoordinateRadian(MyUtils.toRadian(x), MyUtils.toRadian(y));
    }

    /**
     * 清空文件，以便作删除特征
     *
     * @param file
     * @throws FileNotFoundException
     */
    public void clearFile(File file) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(file);
        pw.close();
    }


}
