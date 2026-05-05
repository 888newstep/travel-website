package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 一键导出行程单服务
 * 含导航链接/预订入口
 */
public interface ItineraryExportService {

    /**
     * 导出行程单
     * @param routeId 路线ID
     * @param format 导出格式（pdf, excel, ics-calendar, json）
     * @return 导出结果
     */
    Map<String, Object> exportItinerary(Integer routeId, String format);

    /**
     * 生成行程单HTML
     * @param routeId 路线ID
     * @return HTML内容
     */
    String generateItineraryHtml(Integer routeId);

    /**
     * 生成日程日历文件（ICS格式）
     * @param routeId 路线ID
     * @return ICS文件内容
     */
    String generateCalendarFile(Integer routeId);

    /**
     * 获取导航链接
     * @param fromAttractionId 出发景点ID
     * @param toAttractionId 目的景点ID
     * @param transportType 交通类型
     * @return 导航链接
     */
    Map<String, String> getNavigationLinks(Integer fromAttractionId, Integer toAttractionId, String transportType);

    /**
     * 获取预订入口链接
     * @param attractionId 景点ID
     * @param bookingType 预订类型（ticket-门票, hotel-酒店, restaurant-餐厅）
     * @return 预订链接
     */
    Map<String, String> getBookingLinks(Integer attractionId, String bookingType);

    /**
     * 生成行程分享链接
     * @param routeId 路线ID
     * @return 分享链接
     */
    Map<String, Object> generateShareLink(Integer routeId);

    /**
     * 生成行程二维码
     * @param routeId 路线ID
     * @return 二维码信息
     */
    Map<String, Object> generateQRCode(Integer routeId);

    /**
     * 发送行程到邮箱
     * @param routeId 路线ID
     * @param email 邮箱地址
     * @return 发送结果
     */
    boolean sendItineraryToEmail(Integer routeId, String email);

    /**
     * 打印行程单
     * @param routeId 路线ID
     * @return 打印友好的HTML
     */
    String generatePrintableItinerary(Integer routeId);

    /**
     * 生成行程统计报告
     * @param routeId 路线ID
     * @return 统计报告
     */
    Map<String, Object> generateItineraryReport(Integer routeId);

    /**
     * 批量导出行程
     * @param routeIds 路线ID列表
     * @param format 导出格式
     * @return 批量导出结果
     */
    Map<String, Object> batchExportItineraries(List<Integer> routeIds, String format);
}
