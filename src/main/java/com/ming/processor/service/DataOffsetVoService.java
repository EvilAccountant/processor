package com.ming.processor.service;

import com.ming.processor.entity.TblDataOffsetToOrcl;
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

    public void insertAll(List<TblDataOffsetToOrcl> offsetList) {
        System.out.println("开始存入Oracle数据库");
        Connection connection;
        PreparedStatement preStat;

        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
            connection.setAutoCommit(false);
            String sql = "INSERT INTO TBL_DATA_OFFSET(ID,BRIDGE_ID,MEASURE_POINT,OFFSET,AC_TIME) VALUES(?,?,?,?,?)";
            preStat = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

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
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
