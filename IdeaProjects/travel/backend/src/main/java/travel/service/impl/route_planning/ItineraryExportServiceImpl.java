package travel.service.impl.route_planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.ItineraryExportService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryExportServiceImpl implements ItineraryExportService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private AttractionService attractionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> exportItinerary(Integer routeId, String format) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        String content;
        String contentType;

        switch (format.toLowerCase()) {
            case "html":
                content = generateItineraryHtml(routeId);
                contentType = "text/html";
                break;
            case "ics":
            case "calendar":
                content = generateCalendarFile(routeId);
                contentType = "text/calendar";
                break;
            case "json":
                content = generateItineraryJson(routeId);
                contentType = "application/json";
                break;
            case "pdf":
            case "excel":
            default:
                content = generateItineraryHtml(routeId);
                contentType = "text/html";
        }

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("format", format);
        result.put("contentType", contentType);
        result.put("content", content);
        result.put("fileName", generateFileName(routeId, format));
        result.put("downloadUrl", "/api/itinerary/export/" + routeId + "/" + format);

        return result;
    }

    @Override
    public String generateItineraryHtml(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return "<html><body>路线不存在</body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(route.getTitle()).append(" - 行程单</title>\n");
        html.append("<style>\n");
        html.append(generateHtmlStyles());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // 头部信息
        html.append("<div class=\"header\">\n");
        html.append("<h1>").append(route.getTitle()).append("</h1>\n");
        html.append("<p class=\"subtitle\">").append(route.getDescription()).append("</p>\n");
        html.append("<p class=\"info\">");
        html.append("天数: ").append(route.getDurationDays()).append("天 | ");
        html.append("难度: ").append(route.getDifficulty()).append(" | ");
        html.append("城市: ").append(route.getCity() != null ? route.getCity().getName() : "未知");
        html.append("</p>\n");
        html.append("</div>\n");

        // 每日行程
        html.append("<div class=\"itinerary\">\n");
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        // 按天分组
        Map<Integer, List<RouteAttraction>> dailyAttractions = new HashMap<>();
        for (RouteAttraction ra : attractions) {
            dailyAttractions.computeIfAbsent(ra.getDayNumber(), k -> new ArrayList<>()).add(ra);
        }

        for (int day = 1; day <= route.getDurationDays(); day++) {
            html.append("<div class=\"day\">\n");
            html.append("<h2>第").append(day).append("天</h2>\n");

            List<RouteAttraction> dayAttractions = dailyAttractions.getOrDefault(day, new ArrayList<>());
            for (RouteAttraction ra : dayAttractions) {
                Attraction attraction = attractionService.getById(ra.getAttractionId());
                if (attraction != null) {
                    html.append("<div class=\"attraction\">\n");
                    html.append("<h3>").append(ra.getVisitOrder()).append(". ").append(attraction.getName()).append("</h3>\n");
                    html.append("<p class=\"address\">📍 ").append(attraction.getAddress()).append("</p>\n");
                    html.append("<p class=\"time\">⏰ ").append(attraction.getOpeningHours()).append("</p>\n");
                    html.append("<p class=\"price\">💰 门票: ¥").append(attraction.getTicketPrice()).append("</p>\n");
                    html.append("<p class=\"desc\">").append(attraction.getDescription()).append("</p>\n");

                    // 导航链接
                    Map<String, String> navLinks = getNavigationLinks(null, attraction.getId(), "all");
                    if (!navLinks.isEmpty()) {
                        html.append("<div class=\"nav-links\">\n");
                        html.append("<p>导航:</p>\n");
                        navLinks.forEach((name, link) -> {
                            html.append("<a href=\"").append(link).append("\" target=\"_blank\">").append(name).append("</a>\n");
                        });
                        html.append("</div>\n");
                    }

                    html.append("</div>\n");
                }
            }

            html.append("</div>\n");
        }

        html.append("</div>\n");

        // 页脚
        html.append("<div class=\"footer\">\n");
        html.append("<p>Generated by 智旅 - 智能旅游规划平台</p>\n");
        html.append("<p>导出时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p>\n");
        html.append("</div>\n");

        html.append("</body>\n</html>");

        return html.toString();
    }

    @Override
    public String generateCalendarFile(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return "";
        }

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\n");
        ics.append("VERSION:2.0\n");
        ics.append("PRODID:-//智旅//智能行程//CN\n");
        ics.append("CALSCALE:GREGORIAN\n");
        ics.append("METHOD:PUBLISH\n");

        String uidBase = UUID.randomUUID().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        String now = dateFormat.format(new Date());

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        int eventIndex = 0;

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null) {
                ics.append("BEGIN:VEVENT\n");
                ics.append("UID:").append(uidBase).append("-").append(eventIndex++).append("@zhilv\n");
                ics.append("DTSTAMP:").append(now).append("\n");
                ics.append("DTSTART;VALUE=DATE:").append(calculateEventDate(ra.getDayNumber())).append("\n");
                ics.append("SUMMARY:").append(attraction.getName()).append("\n");
                ics.append("DESCRIPTION:").append(attraction.getDescription()).append("\n");
                ics.append("LOCATION:").append(attraction.getAddress()).append("\n");
                ics.append("END:VEVENT\n");
            }
        }

        ics.append("END:VCALENDAR");

        return ics.toString();
    }

    @Override
    public Map<String, String> getNavigationLinks(Integer fromAttractionId, Integer toAttractionId, String transportType) {
        Map<String, String> links = new HashMap<>();

        Attraction toAttraction = attractionService.getById(toAttractionId);
        if (toAttraction == null || toAttraction.getLatitude() == null || toAttraction.getLongitude() == null) {
            return links;
        }

        String lat = String.valueOf(toAttraction.getLatitude());
        String lng = String.valueOf(toAttraction.getLongitude());
        String name = toAttraction.getName();

        // 高德地图
        links.put("高德地图", "https://uri.amap.com/marker?position=" + lng + "," + lat + "&name=" + name);

        // 百度地图
        links.put("百度地图", "http://api.map.baidu.com/marker?location=" + lat + "," + lng + "&title=" + name + "&content=" + name + "&output=html");

        // 腾讯地图
        links.put("腾讯地图", "https://apis.map.qq.com/tools/poimarker?type=0&marker=coord:" + lat + "," + lng + ";title:" + name + "&key=your_key");

        // Google Maps (国际用户)
        links.put("Google Maps", "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);

        return links;
    }

    @Override
    public Map<String, String> getBookingLinks(Integer attractionId, String bookingType) {
        Map<String, String> links = new HashMap<>();
        Attraction attraction = attractionService.getById(attractionId);

        if (attraction == null) {
            return links;
        }

        String name = attraction.getName();

        switch (bookingType) {
            case "ticket":
                links.put("携程", "https://hotels.ctrip.com/hotels/listPage?keyword=" + name);
                links.put("美团", "https://www.meituan.com/s/" + name);
                links.put("飞猪", "https://www.fliggy.com/search?q=" + name);
                break;
            case "hotel":
                links.put("携程酒店", "https://hotels.ctrip.com/hotels/listPage?keyword=" + name);
                links.put("美团酒店", "https://hotel.meituan.com/" + name);
                links.put("Booking", "https://www.booking.com/searchresults.html?ss=" + name);
                break;
            case "restaurant":
                links.put("大众点评", "https://www.dianping.com/search/keyword/1/0_" + name);
                links.put("美团", "https://www.meituan.com/s/" + name);
                break;
        }

        return links;
    }

    @Override
    public Map<String, Object> generateShareLink(Integer routeId) {
        Map<String, Object> share = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            share.put("success", false);
            share.put("message", "路线不存在");
            return share;
        }

        String shareCode = generateShareCode(routeId);
        String shareUrl = "https://zhilv.com/share/" + shareCode;

        share.put("success", true);
        share.put("routeId", routeId);
        share.put("shareCode", shareCode);
        share.put("shareUrl", shareUrl);
        share.put("title", route.getTitle());
        share.put("description", route.getDescription());
        share.put("expiresIn", 30); // 30天有效期

        return share;
    }

    @Override
    public Map<String, Object> generateQRCode(Integer routeId) {
        Map<String, Object> qrCode = new HashMap<>();

        Map<String, Object> shareInfo = generateShareLink(routeId);
        if (!(Boolean) shareInfo.get("success")) {
            qrCode.put("success", false);
            qrCode.put("message", shareInfo.get("message"));
            return qrCode;
        }

        String shareUrl = (String) shareInfo.get("shareUrl");

        qrCode.put("success", true);
        qrCode.put("routeId", routeId);
        qrCode.put("qrCodeUrl", "/api/qrcode/generate?data=" + shareUrl);
        qrCode.put("shareUrl", shareUrl);
        qrCode.put("size", 300); // 二维码尺寸

        return qrCode;
    }

    @Override
    public boolean sendItineraryToEmail(Integer routeId, String email) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                return false;
            }

            String htmlContent = generateItineraryHtml(routeId);

            // 这里应该调用邮件服务发送邮件
            log.info("发送行程单到邮箱: routeId={}, email={}, contentLength={}", routeId, email, htmlContent.length());

            // 模拟发送成功
            return true;
        } catch (Exception e) {
            log.error("发送行程单失败: routeId={}, email={}, error={}", routeId, email, e.getMessage());
            return false;
        }
    }

    @Override
    public String generatePrintableItinerary(Integer routeId) {
        String html = generateItineraryHtml(routeId);

        // 添加打印样式
        String printStyles = "<style>\n" +
                "@media print {\n" +
                "  body { font-size: 12pt; }\n" +
                "  .no-print { display: none; }\n" +
                "  .page-break { page-break-after: always; }\n" +
                "}\n" +
                "</style>";

        return html.replace("</head>", printStyles + "</head>");
    }

    @Override
    public Map<String, Object> generateItineraryReport(Integer routeId) {
        Map<String, Object> report = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            report.put("success", false);
            report.put("message", "路线不存在");
            return report;
        }

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        report.put("success", true);
        report.put("routeId", routeId);
        report.put("routeName", route.getTitle());
        report.put("totalAttractions", attractions.size());
        report.put("durationDays", route.getDurationDays());
        report.put("totalDistance", calculateTotalDistance(attractions));
        report.put("estimatedCost", calculateEstimatedCost(attractions));
        report.put("attractionTypes", analyzeAttractionTypes(attractions));
        report.put("difficulty", route.getDifficulty());
        report.put("generatedAt", new Date());

        return report;
    }

    @Override
    public Map<String, Object> batchExportItineraries(List<Integer> routeIds, String format) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> exports = new ArrayList<>();

        for (Integer routeId : routeIds) {
            Map<String, Object> export = exportItinerary(routeId, format);
            exports.add(export);
        }

        result.put("success", true);
        result.put("totalCount", routeIds.size());
        result.put("exports", exports);
        result.put("downloadPackageUrl", "/api/itinerary/export/batch/" + UUID.randomUUID().toString());

        return result;
    }

    // 辅助方法
    private String generateHtmlStyles() {
        return "body { font-family: 'Microsoft YaHei', Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }\n" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 20px; }\n" +
                ".header h1 { margin: 0 0 10px 0; font-size: 28px; }\n" +
                ".subtitle { font-size: 16px; opacity: 0.9; margin: 0 0 10px 0; }\n" +
                ".info { font-size: 14px; opacity: 0.8; margin: 0; }\n" +
                ".itinerary { background: white; border-radius: 10px; padding: 20px; }\n" +
                ".day { margin-bottom: 30px; border-bottom: 2px solid #eee; padding-bottom: 20px; }\n" +
                ".day:last-child { border-bottom: none; }\n" +
                ".day h2 { color: #667eea; font-size: 22px; margin: 0 0 15px 0; }\n" +
                ".attraction { background: #f8f9fa; border-radius: 8px; padding: 15px; margin-bottom: 15px; }\n" +
                ".attraction h3 { color: #333; font-size: 18px; margin: 0 0 10px 0; }\n" +
                ".attraction p { margin: 5px 0; color: #666; font-size: 14px; }\n" +
                ".address:before { content: '📍 '; }\n" +
                ".time:before { content: '⏰ '; }\n" +
                ".price:before { content: '💰 '; }\n" +
                ".nav-links { margin-top: 10px; }\n" +
                ".nav-links a { display: inline-block; margin-right: 10px; padding: 5px 10px; background: #667eea; color: white; text-decoration: none; border-radius: 4px; font-size: 12px; }\n" +
                ".footer { text-align: center; margin-top: 30px; padding: 20px; color: #999; font-size: 12px; }\n";
    }

    private String generateItineraryJson(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return "{}";
        }

        Map<String, Object> itinerary = new HashMap<>();
        itinerary.put("routeId", routeId);
        itinerary.put("title", route.getTitle());
        itinerary.put("description", route.getDescription());
        itinerary.put("durationDays", route.getDurationDays());
        itinerary.put("difficulty", route.getDifficulty());

        List<Map<String, Object>> days = new ArrayList<>();
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        Map<Integer, List<RouteAttraction>> dailyAttractions = new HashMap<>();
        for (RouteAttraction ra : attractions) {
            dailyAttractions.computeIfAbsent(ra.getDayNumber(), k -> new ArrayList<>()).add(ra);
        }

        for (int day = 1; day <= route.getDurationDays(); day++) {
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("dayNumber", day);

            List<Map<String, Object>> dayAttractions = new ArrayList<>();
            for (RouteAttraction ra : dailyAttractions.getOrDefault(day, new ArrayList<>())) {
                Attraction attraction = attractionService.getById(ra.getAttractionId());
                if (attraction != null) {
                    Map<String, Object> attrInfo = new HashMap<>();
                    attrInfo.put("order", ra.getVisitOrder());
                    attrInfo.put("name", attraction.getName());
                    attrInfo.put("address", attraction.getAddress());
                    attrInfo.put("openingHours", attraction.getOpeningHours());
                    attrInfo.put("ticketPrice", attraction.getTicketPrice());
                    attrInfo.put("navigation", getNavigationLinks(null, attraction.getId(), "all"));
                    dayAttractions.add(attrInfo);
                }
            }

            dayInfo.put("attractions", dayAttractions);
            days.add(dayInfo);
        }

        itinerary.put("days", days);

        try {
            return objectMapper.writeValueAsString(itinerary);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    private String calculateEventDate(Integer dayNumber) {
        // 简化处理，实际应该根据出发日期计算
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, dayNumber - 1);
        return new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
    }

    private String generateShareCode(Integer routeId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((routeId + "-" + System.currentTimeMillis()).getBytes());
    }

    private String generateFileName(Integer routeId, String format) {
        Route route = routeService.getById(routeId);
        String routeName = route != null ? route.getTitle() : "行程单";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return routeName + "_" + timestamp + "." + format.toLowerCase();
    }

    private Double calculateTotalDistance(List<RouteAttraction> attractions) {
        // 简化计算，实际需要根据景点坐标计算
        return attractions.size() * 5.0; // 假设每个景点间平均5公里
    }

    private Double calculateEstimatedCost(List<RouteAttraction> attractions) {
        double total = 0.0;
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getTicketPrice() != null) {
                total += attraction.getTicketPrice().doubleValue();
            }
        }
        return total;
    }

    private Map<String, Integer> analyzeAttractionTypes(List<RouteAttraction> attractions) {
        Map<String, Integer> types = new HashMap<>();
        types.put("文化古迹", 0);
        types.put("自然风光", 0);
        types.put("现代建筑", 0);
        types.put("主题公园", 0);

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getDescription() != null) {
                String desc = attraction.getDescription();
                if (desc.contains("博物馆") || desc.contains("古迹")) {
                    types.put("文化古迹", types.get("文化古迹") + 1);
                } else if (desc.contains("公园") || desc.contains("山") || desc.contains("湖")) {
                    types.put("自然风光", types.get("自然风光") + 1);
                } else if (desc.contains("塔") || desc.contains("大厦")) {
                    types.put("现代建筑", types.get("现代建筑") + 1);
                } else if (desc.contains("乐园") || desc.contains("迪士尼")) {
                    types.put("主题公园", types.get("主题公园") + 1);
                }
            }
        }

        return types;
    }
}
