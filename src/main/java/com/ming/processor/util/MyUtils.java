package com.ming.processor.util;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public final class MyUtils {

    /**
     * 获取当日年月日
     *
     * @return
     */
    public static String getToday() {

        Calendar calendar = Calendar.getInstance();//日历对象
        calendar.setTime(new Date());//设置当前日期

        String yearStr = calendar.get(Calendar.YEAR) + "";//获取年份

        int month = calendar.get(Calendar.MONTH) + 1;//获取月份
        String monthStr = month < 10 ? "0" + month : month + "";

        int day = calendar.get(Calendar.DATE);//获取日
        String dayStr = day < 10 ? "0" + day : day + "";

        return yearStr + "-" + monthStr + "-" + dayStr + " ";
    }

    /**
     * 解析数据至十进制
     */
    public static double decimalParse(String oriString) {
        //符号位
        int symbol = Integer.valueOf(oriString.substring(0, 1));
        double value = Double.valueOf(oriString.substring(1, 4) + "." + oriString.substring(4, 8));
        if (symbol == 1) return -value;
        else return value;
    }

    /**
     * 生成32位UUID
     *
     * @return
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 转为弧度
     */
    public static double toRadian(double angle) {
        return angle * Math.PI / 180D;
    }

}
