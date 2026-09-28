package com.schoolmate.utils;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * 星座工具：根据生日自动计算星座。
 *
 * @author Albot
 */
public class ConstellationUtil {

    private static final String[] NAMES = {
        "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座",
        "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座"
    };

    /** 各星座起始日（月-日），按时间顺序排列 */
    private static final MonthDay[] START_DAYS = {
        MonthDay.of(1, 20),   // 水瓶座
        MonthDay.of(2, 19),   // 双鱼座
        MonthDay.of(3, 21),   // 白羊座
        MonthDay.of(4, 20),   // 金牛座
        MonthDay.of(5, 21),   // 双子座
        MonthDay.of(6, 22),   // 巨蟹座
        MonthDay.of(7, 23),   // 狮子座
        MonthDay.of(8, 23),   // 处女座
        MonthDay.of(9, 23),   // 天秤座
        MonthDay.of(10, 24),  // 天蝎座
        MonthDay.of(11, 23),  // 射手座
        MonthDay.of(12, 22)   // 摩羯座
    };

    private ConstellationUtil() {
    }

    /**
     * 根据生日计算星座，生日为空返回 null。
     */
    public static String of(LocalDate birthday) {
        if (birthday == null) {
            return null;
        }
        MonthDay day = MonthDay.from(birthday);
        for (int i = START_DAYS.length - 1; i >= 0; i--) {
            if (!day.isBefore(START_DAYS[i])) {
                return NAMES[i];
            }
        }
        // 1 月 1 日 ~ 1 月 19 日为摩羯座
        return NAMES[NAMES.length - 1];
    }
}
