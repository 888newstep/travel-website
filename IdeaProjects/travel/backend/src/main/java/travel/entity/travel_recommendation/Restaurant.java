package travel.entity.travel_recommendation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("restaurants")
public class Restaurant {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;

    @TableField("city_id")
    private Integer cityId;

    @TableField("address")
    private String address;

    @TableField("latitude")
    private Double latitude;

    @TableField("longitude")
    private Double longitude;

    @TableField("rating")
    private Double rating;

    @TableField("price_level")
    private String priceLevel;

    @TableField("average_cost")
    private BigDecimal averageCost;

    @TableField("cuisine_type")
    private String cuisineType;

    @TableField("feature")
    private String feature;

    @TableField("phone")
    private String phone;

    @TableField("opening_hours")
    private String openingHours;

    @TableField("image_url")
    private String imageUrl;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 关联关系
    @TableField(exist = false)
    private City city;
}
