-- 数据库初始化脚本
-- 创建必要的索引和约束

-- 1. 景点表索引
CREATE INDEX idx_attractions_city_id ON attractions(city_id);
CREATE INDEX idx_attractions_name ON attractions(name);
CREATE INDEX idx_attractions_rating ON attractions(rating);

-- 2. 路线表索引
CREATE INDEX idx_routes_city_id ON routes(city_id);
CREATE INDEX idx_routes_user_id ON routes(user_id);
CREATE INDEX idx_routes_view_count ON routes(view_count);
CREATE INDEX idx_routes_like_count ON routes(like_count);

-- 3. 路线景点关联表索引
CREATE INDEX idx_route_attractions_route_id ON route_attractions(route_id);
CREATE INDEX idx_route_attractions_attraction_id ON route_attractions(attraction_id);
CREATE INDEX idx_route_attractions_day_number ON route_attractions(day_number);

-- 4. 路线交通表索引
CREATE INDEX idx_route_transport_route_id ON route_transport(route_id);
CREATE INDEX idx_route_transport_from_attraction_id ON route_transport(from_attraction_id);
CREATE INDEX idx_route_transport_to_attraction_id ON route_transport(to_attraction_id);
CREATE INDEX idx_route_transport_transport_id ON route_transport(transport_id);

-- 5. 景点实时状态表索引
CREATE INDEX idx_attraction_realtime_status_attraction_id ON attraction_realtime_status(attraction_id);
CREATE INDEX idx_attraction_realtime_status_update_time ON attraction_realtime_status(update_time);
CREATE INDEX idx_attraction_realtime_status_crowd_level ON attraction_realtime_status(crowd_level);

-- 6. 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);

-- 7. 交通表索引
CREATE INDEX idx_transport_transport_type ON transport(transport_type);
CREATE INDEX idx_transport_avg_speed_kmh ON transport(avg_speed_kmh);
CREATE INDEX idx_transport_cost_per_km ON transport(cost_per_km);

-- 8. 城市表索引
CREATE INDEX idx_cities_name ON cities(name);
