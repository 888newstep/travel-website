package travel.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    // 10xx 用户模块错误
    CAPTCHA_ERROR(1001, "验证码错误或已失效"),
    PASSWORD_MISMATCH(1002, "两次输入的密码不一致"),
    AGREEMENT_ERROR(1003, "请阅读并同意用户协议"),
    USER_EXIST(1004, "该手机号已注册"),
    USER_NOT_EXIST(1005, "用户不存在"),
    PASSWORD_ERROR(1006, "账号或密码错误"),
    USER_STATUS_ERROR(1007, "用户状态异常"),
    USER_TOKEN_EXPIRED(1008, "用户登录已过期"),
    USER_TOKEN_ERROR(1009, "用户登录令牌错误"),
    USER_LOGIN_FAILED(1010, "用户登录失败"),
    USER_REGISTER_FAILED(1011, "用户注册失败"),
    USER_INFO_UPDATE_FAILED(1012, "用户信息更新失败"),
    USER_PASSWORD_UPDATE_FAILED(1013, "用户密码更新失败"),
    USER_AVATAR_UPLOAD_FAILED(1014, "用户头像上传失败"),

    // 20xx 路线模块错误
    ROUTE_NOT_EXIST(2001, "路线不存在"),
    NO_PERMISSION(2002, "无权限操作该路线"),
    ROUTE_ATTR_ORDER_DUPLICATE(2003, "同天数内景点访问顺序不能重复"),
    ROUTE_ATTR_RELATION_NOT_EXIST(2004, "路线-景点关联关系不存在"),
    ROUTE_CREATE_FAILED(2005, "路线创建失败"),
    ROUTE_UPDATE_FAILED(2006, "路线更新失败"),
    ROUTE_DELETE_FAILED(2007, "路线删除失败"),
    ROUTE_PUBLISH_FAILED(2008, "路线发布失败"),
    ROUTE_UNPUBLISH_FAILED(2009, "路线取消发布失败"),
    ROUTE_VIEW_FAILED(2010, "路线查看失败"),
    ROUTE_SHARE_FAILED(2011, "路线分享失败"),
    ROUTE_COLLECT_FAILED(2012, "路线收藏失败"),
    ROUTE_UNCOLLECT_FAILED(2013, "路线取消收藏失败"),
    ROUTE_COMMENT_FAILED(2014, "路线评论失败"),
    ROUTE_RATE_FAILED(2015, "路线评分失败"),
    ROUTE_DURATION_ERROR(2016, "路线天数设置错误"),
    ROUTE_TITLE_ERROR(2017, "路线标题错误"),
    ROUTE_DESCRIPTION_ERROR(2018, "路线描述错误"),
    ROUTE_COST_ERROR(2019, "路线预算错误"),
    ROUTE_TRANSPORT_ERROR(2020, "路线交通方式错误"),

    // 30xx 景点模块错误
    ATTRACTION_STATUS_NOT_EXIST(3001, "景点无实时数据"),
    ATTRACTION_NOT_EXIST(3002, "景点不存在"),
    ATTRACTION_VIEW_FAILED(3003, "景点查看失败"),

    // 40xx 参数错误
    PARAM_ERROR(4000, "参数错误"),
    PARAM_MISSING(4001, "参数缺失"),
    PARAM_FORMAT_ERROR(4002, "参数格式错误"),
    PARAM_RANGE_ERROR(4003, "参数范围错误"),
    PARAM_TYPE_ERROR(4004, "参数类型错误"),
    PARAM_LENGTH_ERROR(4005, "参数长度错误"),
    PARAM_VALUE_ERROR(4006, "参数值错误"),
    PARAM_DUPLICATE_ERROR(4007, "参数重复错误"),
    PARAM_CONFLICT_ERROR(4008, "参数冲突错误"),
    PARAM_VALIDATION_ERROR(4009, "参数验证错误"),

    // 50xx 系统错误
    SYSTEM_ERROR(5000, "系统内部错误"),
    SYSTEM_BUSY(5001, "系统繁忙，请稍后重试"),
    SYSTEM_MAINTENANCE(5002, "系统维护中，请稍后访问"),
    SYSTEM_TIMEOUT(5003, "系统超时，请稍后重试"),
    SYSTEM_RESOURCE_ERROR(5004, "系统资源错误"),
    SYSTEM_CONFIG_ERROR(5005, "系统配置错误"),
    SYSTEM_DEPENDENCY_ERROR(5006, "系统依赖错误"),
    SYSTEM_NETWORK_ERROR(5007, "系统网络错误"),
    SYSTEM_DATABASE_ERROR(5008, "系统数据库错误"),
    SYSTEM_REDIS_ERROR(5009, "系统缓存错误"),
    SYSTEM_FILE_ERROR(5010, "系统文件错误"),

    // 60xx 路线分享模块错误
    SHARE_NOT_EXIST(6001, "分享不存在"),
    SHARE_EXPIRED(6002, "分享已过期"),
    SHARE_CREATE_FAILED(6003, "分享创建失败"),
    SHARE_DELETE_FAILED(6004, "分享删除失败"),
    SHARE_VIEW_FAILED(6005, "分享查看失败"),
    SHARE_CODE_ERROR(6006, "分享码错误"),
    SHARE_PERMISSION_ERROR(6007, "分享权限错误"),

    // 70xx 路线评价模块错误
    COMMENT_NOT_EXIST(7001, "评论不存在"),
    NO_COMMENT_PERMISSION(7002, "无权限操作该评论"),
    COMMENT_CREATE_FAILED(7003, "评论创建失败"),
    COMMENT_UPDATE_FAILED(7004, "评论更新失败"),
    COMMENT_DELETE_FAILED(7005, "评论删除失败"),
    COMMENT_VIEW_FAILED(7006, "评论查看失败"),
    COMMENT_RATE_ERROR(7007, "评论评分错误"),
    COMMENT_CONTENT_ERROR(7008, "评论内容错误"),

    // 80xx 路线收藏模块错误
    COLLECTION_NOT_EXIST(8001, "收藏不存在"),
    NO_COLLECTION_PERMISSION(8002, "无权限操作该收藏"),
    COLLECTION_CREATE_FAILED(8003, "收藏创建失败"),
    COLLECTION_DELETE_FAILED(8004, "收藏删除失败"),
    COLLECTION_VIEW_FAILED(8005, "收藏查看失败"),
    COLLECTION_LIST_FAILED(8006, "收藏列表获取失败"),

    // 90xx 文件模块错误
    FILE_NOT_EXIST(9001, "文件不存在"),
    FILE_UPLOAD_FAILED(9002, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(9003, "文件下载失败"),
    FILE_DELETE_FAILED(9004, "文件删除失败"),
    FILE_SIZE_ERROR(9005, "文件大小错误"),
    FILE_TYPE_ERROR(9006, "文件类型错误"),
    FILE_FORMAT_ERROR(9007, "文件格式错误"),
    FILE_PATH_ERROR(9008, "文件路径错误"),
    FILE_PERMISSION_ERROR(9009, "文件权限错误"),
    FILE_STORAGE_ERROR(9010, "文件存储错误"),

    // 100xx 网络模块错误
    NETWORK_ERROR(10001, "网络错误"),
    NETWORK_TIMEOUT(10002, "网络超时"),
    NETWORK_CONNECTION_ERROR(10003, "网络连接错误"),
    NETWORK_REQUEST_ERROR(10004, "网络请求错误"),
    NETWORK_RESPONSE_ERROR(10005, "网络响应错误"),
    NETWORK_PROXY_ERROR(10006, "网络代理错误"),
    NETWORK_FIREWALL_ERROR(10007, "网络防火墙错误"),

    // 110xx Redis模块错误
    REDIS_CONNECTION_ERROR(11001, "Redis连接错误"),
    REDIS_TIMEOUT_ERROR(11002, "Redis超时错误"),
    REDIS_COMMAND_ERROR(11003, "Redis命令错误"),
    REDIS_KEY_ERROR(11004, "Redis键错误"),
    REDIS_VALUE_ERROR(11005, "Redis值错误"),
    REDIS_EXPIRE_ERROR(11006, "Redis过期时间错误"),
    REDIS_DELETE_ERROR(11007, "Redis删除错误"),
    REDIS_SET_ERROR(11008, "Redis设置错误"),
    REDIS_GET_ERROR(11009, "Redis获取错误"),

    // 120xx 数据库模块错误
    DATABASE_CONNECTION_ERROR(12001, "数据库连接错误"),
    DATABASE_TIMEOUT_ERROR(12002, "数据库超时错误"),
    DATABASE_QUERY_ERROR(12003, "数据库查询错误"),
    DATABASE_UPDATE_ERROR(12004, "数据库更新错误"),
    DATABASE_INSERT_ERROR(12005, "数据库插入错误"),
    DATABASE_DELETE_ERROR(12006, "数据库删除错误"),
    DATABASE_TRANSACTION_ERROR(12007, "数据库事务错误"),
    DATABASE_INDEX_ERROR(12008, "数据库索引错误"),
    DATABASE_LOCK_ERROR(12009, "数据库锁错误"),
    DATABASE_DEADLOCK_ERROR(12010, "数据库死锁错误"),

    // 130xx 第三方服务模块错误
    THIRD_PARTY_SERVICE_ERROR(13001, "第三方服务错误"),
    THIRD_PARTY_API_ERROR(13002, "第三方API错误"),
    THIRD_PARTY_AUTH_ERROR(13003, "第三方认证错误"),
    THIRD_PARTY_TIMEOUT_ERROR(13004, "第三方服务超时"),
    THIRD_PARTY_RATE_LIMIT_ERROR(13005, "第三方服务速率限制"),
    THIRD_PARTY_BILLING_ERROR(13006, "第三方服务计费错误"),

    // 140xx 安全模块错误
    SECURITY_ERROR(14001, "安全错误"),
    SECURITY_AUTH_ERROR(14002, "安全认证错误"),
    SECURITY_PERMISSION_ERROR(14003, "安全权限错误"),
    SECURITY_ENCRYPT_ERROR(14004, "安全加密错误"),
    SECURITY_DECRYPT_ERROR(14005, "安全解密错误"),
    SECURITY_SIGN_ERROR(14006, "安全签名错误"),
    SECURITY_VERIFY_ERROR(14007, "安全验证错误"),
    SECURITY_TOKEN_ERROR(14008, "安全令牌错误"),
    SECURITY_SESSION_ERROR(14009, "安全会话错误"),
    SECURITY_CSRF_ERROR(14010, "安全跨站请求错误"),
    SECURITY_XSS_ERROR(14011, "安全跨站脚本错误"),
    SECURITY_SQL_INJECTION_ERROR(14012, "安全SQL注入错误"),
    SECURITY_COMMAND_INJECTION_ERROR(14013, "安全命令注入错误"),

    // 150xx 配置模块错误
    CONFIG_ERROR(15001, "配置错误"),
    CONFIG_NOT_FOUND(15002, "配置不存在"),
    CONFIG_LOAD_ERROR(15003, "配置加载错误"),
    CONFIG_PARSE_ERROR(15004, "配置解析错误"),
    CONFIG_VALIDATION_ERROR(15005, "配置验证错误"),
    CONFIG_UPDATE_ERROR(15006, "配置更新错误"),
    CONFIG_RELOAD_ERROR(15007, "配置重载错误"),

    // 160xx 路线推荐模块错误
    ROUTE_RECOMMENDATION_FAILED(16001, "路线推荐失败"),
    ROUTE_RECOMMENDATION_NO_DATA(16002, "路线推荐无数据"),
    ROUTE_RECOMMENDATION_PARAM_ERROR(16003, "路线推荐参数错误"),
    ROUTE_RECOMMENDATION_TIMEOUT(16004, "路线推荐超时"),
    ROUTE_RECOMMENDATION_ALGORITHM_ERROR(16005, "路线推荐算法错误"),

    // 170xx 路线优化模块错误
    ROUTE_OPTIMIZATION_FAILED(17001, "路线优化失败"),
    ROUTE_OPTIMIZATION_NO_DATA(17002, "路线优化无数据"),
    ROUTE_OPTIMIZATION_PARAM_ERROR(17003, "路线优化参数错误"),
    ROUTE_OPTIMIZATION_TIMEOUT(17004, "路线优化超时"),
    ROUTE_OPTIMIZATION_ALGORITHM_ERROR(17005, "路线优化算法错误"),

    // 180xx 路线规划模块错误
    ROUTE_PLANNING_FAILED(18001, "路线规划失败"),
    ROUTE_PLANNING_NO_DATA(18002, "路线规划无数据"),
    ROUTE_PLANNING_PARAM_ERROR(18003, "路线规划参数错误"),
    ROUTE_PLANNING_TIMEOUT(18004, "路线规划超时"),
    ROUTE_PLANNING_ALGORITHM_ERROR(18005, "路线规划算法错误"),
    ROUTE_PLANNING_INSUFFICIENT_ATTRACTIONS(18006, "路线规划景点不足"),
    ROUTE_PLANNING_INVALID_DURATION(18007, "路线规划时长无效"),

    // 190xx 智能推荐模块错误
    INTELLIGENT_RECOMMENDATION_FAILED(19001, "智能推荐失败"),
    INTELLIGENT_RECOMMENDATION_NO_DATA(19002, "智能推荐无数据"),
    INTELLIGENT_RECOMMENDATION_PARAM_ERROR(19003, "智能推荐参数错误"),
    INTELLIGENT_RECOMMENDATION_TIMEOUT(19004, "智能推荐超时"),
    INTELLIGENT_RECOMMENDATION_ALGORITHM_ERROR(19005, "智能推荐算法错误"),

    // 200xx 实时数据模块错误
    REALTIME_DATA_FETCH_FAILED(20001, "实时数据获取失败"),
    REALTIME_DATA_PARSE_FAILED(20002, "实时数据解析失败"),
    REALTIME_DATA_UPDATE_FAILED(20003, "实时数据更新失败"),
    REALTIME_DATA_VALIDATION_ERROR(20004, "实时数据验证错误"),
    REALTIME_DATA_TIMEOUT(20005, "实时数据获取超时"),
    REALTIME_DATA_SERVICE_ERROR(20006, "实时数据服务错误"),

    // 210xx 交通模块错误
    TRANSPORT_NOT_EXIST(21001, "交通方式不存在"),
    TRANSPORT_OPERATE_FAILED(21002, "交通方式操作失败"),
    TRANSPORT_SEARCH_FAILED(21003, "交通方式搜索失败"),
    TRANSPORT_FILTER_FAILED(21004, "交通方式筛选失败"),
    TRANSPORT_DETAIL_FAILED(21005, "交通方式详情获取失败"),
    TRANSPORT_STATUS_ERROR(21006, "交通方式状态错误"),
    TRANSPORT_ROUTE_ERROR(21007, "交通路线错误"),
    TRANSPORT_COST_ERROR(21008, "交通成本错误"),
    TRANSPORT_TIME_ERROR(21009, "交通时间错误"),

    // 220xx 景点模块错误
    ATTRACTION_SEARCH_FAILED(22002, "景点搜索失败"),
    ATTRACTION_FILTER_FAILED(22003, "景点筛选失败"),
    ATTRACTION_DETAIL_FAILED(22004, "景点详情获取失败"),
    ATTRACTION_STATUS_ERROR(22005, "景点状态错误"),
    ATTRACTION_CROWD_ERROR(22006, "景点人流量数据错误"),
    ATTRACTION_TICKET_ERROR(22007, "景点门票信息错误"),
    ATTRACTION_OPEN_TIME_ERROR(22008, "景点开放时间错误"),
    ATTRACTION_ADDRESS_ERROR(22009, "景点地址错误"),
    ATTRACTION_CONTACT_ERROR(22010, "景点联系方式错误"),
    ATTRACTION_IMAGE_ERROR(22011, "景点图片错误"),
    ATTRACTION_VIDEO_ERROR(22012, "景点视频错误"),
    ATTRACTION_REVIEW_ERROR(22013, "景点评价错误"),

    // 230xx 季节和天气模块错误
    SEASON_DATA_ERROR(23001, "季节数据错误"),
    WEATHER_DATA_ERROR(23002, "天气数据错误"),
    SEASON_WEATHER_MISMATCH(23003, "季节和天气不匹配"),
    SEASON_RECOMMENDATION_ERROR(23004, "季节推荐错误"),
    WEATHER_RECOMMENDATION_ERROR(23005, "天气推荐错误"),

    // 240xx 社交网络模块错误
    SOCIAL_NETWORK_ERROR(24001, "社交网络错误"),
    SOCIAL_NETWORK_FETCH_FAILED(24002, "社交网络数据获取失败"),
    SOCIAL_NETWORK_PARSE_FAILED(24003, "社交网络数据解析失败"),
    SOCIAL_NETWORK_RECOMMENDATION_ERROR(24004, "社交网络推荐错误"),
    SOCIAL_NETWORK_AUTH_ERROR(24005, "社交网络认证错误"),

    // 250xx 反馈模块错误
    FEEDBACK_NOT_EXIST(25001, "反馈不存在"),
    FEEDBACK_CREATE_FAILED(25002, "反馈创建失败"),
    FEEDBACK_UPDATE_FAILED(25003, "反馈更新失败"),
    FEEDBACK_DELETE_FAILED(25004, "反馈删除失败"),
    FEEDBACK_REPLY_FAILED(25005, "反馈回复失败"),

    // 260xx 通知模块错误
    NOTIFICATION_NOT_EXIST(26001, "通知不存在"),
    NOTIFICATION_CREATE_FAILED(26002, "通知创建失败"),
    NOTIFICATION_UPDATE_FAILED(26003, "通知更新失败"),
    NOTIFICATION_DELETE_FAILED(26004, "通知删除失败"),

    // 270xx 旅行计划模块错误
    PLAN_NOT_EXIST(27001, "旅行计划不存在"),
    PLAN_CREATE_FAILED(27002, "旅行计划创建失败"),
    PLAN_UPDATE_FAILED(27003, "旅行计划更新失败"),
    PLAN_DELETE_FAILED(27004, "旅行计划删除失败"),
    PLAN_SHARE_FAILED(27005, "旅行计划分享失败"),

    // 280xx 权限模块错误
    PERMISSION_DENIED(28001, "权限不足"),
    UNAUTHORIZED(28002, "未授权访问");

    private ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;
}
