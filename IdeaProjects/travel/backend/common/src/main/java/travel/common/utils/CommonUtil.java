package travel.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    /**
     * 格式化日期
     */
    public static String formatDate(Date date, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(date);
        } catch (Exception e) {
            log.error("日期格式化失败: pattern={}, error={}", pattern, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成随机字符串
     */
    public static String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成随机数字
     */
    public static String generateRandomNumber(int length) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 验证邮箱格式
     */
    public static boolean validateEmail(String email) {
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * 验证手机号格式
     */
    public static boolean validatePhone(String phone) {
        String regex = "^1[3-9]\\d{9}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phone);
        return matcher.matches();
    }

    /**
     * 验证身份证号格式
     */
    public static boolean validateIdCard(String idCard) {
        String regex = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(idCard);
        return matcher.matches();
    }

    /**
     * 计算两点之间的距离（经纬度）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 计算路线的总距离
     */
    public static double calculateTotalDistance(double[] latitudes, double[] longitudes) {
        if (latitudes == null || longitudes == null || latitudes.length != longitudes.length || latitudes.length < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < latitudes.length - 1; i++) {
            totalDistance += calculateDistance(latitudes[i], longitudes[i], latitudes[i + 1], longitudes[i + 1]);
        }
        return totalDistance;
    }

    /**
     * 计算路线的总时间
     */
    public static double calculateTotalTime(double distance, double speed) {
        if (speed <= 0) {
            return 0.0;
        }
        return distance / speed;
    }

    /**
     * 计算路线的总成本
     */
    public static double calculateTotalCost(double distance, double costPerKm) {
        return distance * costPerKm;
    }

    /**
     * 限制数字在指定范围内
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 格式化数字，保留指定小数位
     */
    public static double formatNumber(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 获取字符串的长度
     */
    public static int getLength(String str) {
        return isEmpty(str) ? 0 : str.length();
    }

    /**
     * 截断字符串到指定长度
     */
    public static String truncate(String str, int length) {
        if (isEmpty(str) || length <= 0) {
            return "";
        }
        if (str.length() <= length) {
            return str;
        }
        return str.substring(0, length) + "...";
    }
}
