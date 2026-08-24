CREATE TABLE IF NOT EXISTS route_visit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '访问明细ID',
    route_id INT NOT NULL COMMENT '路线ID',
    user_id INT NULL COMMENT '已认证用户ID，匿名访问为空',
    visitor_hash CHAR(64) NOT NULL COMMENT '加盐SHA-256访问者标识',
    visitor_type VARCHAR(20) NOT NULL COMMENT 'AUTHENTICATED/ANONYMOUS',
    visit_date DATE NOT NULL COMMENT '访问日期',
    visited_at DATETIME(3) NOT NULL COMMENT '访问时间',
    CONSTRAINT fk_route_visit_route
        FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    CONSTRAINT fk_route_visit_user
        FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_route_visit_date (route_id, visit_date),
    INDEX idx_route_visitor_date (route_id, visitor_hash, visit_date),
    INDEX idx_route_visit_user_time (user_id, visited_at)
) COMMENT='路线访问明细' ENGINE=InnoDB;
