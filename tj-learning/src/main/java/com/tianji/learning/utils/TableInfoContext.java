package com.tianji.learning.utils;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/03  13:37
 */

public class TableInfoContext {

    public final static ThreadLocal<String> TL = new ThreadLocal<>();

    // 存
    public static void setInfo(String info) {
        TL.set(info);
    }

    // 取
    public static String getInfo() {
        return TL.get();
    }

    // 移除
    public static void remove() {
        TL.remove();
    }
}
