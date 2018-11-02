package com.ming.processor.service;

import com.ming.processor.entity.DataOffsetVo;
import com.ming.processor.util.MyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;

@Service
public class DataOffsetVoService {

    @Value("${oracleUrl}")
    String oracleUrl;
    @Value("${oracleUsername}")
    String oracleUsername;
    @Value("${oraclePassword}")
    String oraclePassword;

    public void insertAll(List<DataOffsetVo> voList) {

        Connection connection;
        PreparedStatement preStat;

        try {
            connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
            connection.setAutoCommit(false);
            String sql = "INSERT INTO TblDataOffset(id,minZone,measurePoint,minOffset,maxOffset,avgOffset) VALUES(?,?,?,?,?,?)";
            preStat = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            DataOffsetVo vo;
            for (int i = 0; i < voList.size(); i++) {
                vo = voList.get(i);
                preStat.setString(1, MyUtils.generateUUID());
                preStat.setString(2, vo.getMinZone());
                preStat.setString(3, vo.getMeasurePoint());
                preStat.setString(4, vo.getMinOffset());
                preStat.setString(5, vo.getMaxOffset());
                preStat.setString(6, vo.getAvgOffset());
                preStat.addBatch();
            }

            preStat.executeBatch();
            connection.commit();
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
