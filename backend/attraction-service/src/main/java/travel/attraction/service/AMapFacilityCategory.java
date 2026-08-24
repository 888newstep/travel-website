package travel.attraction.service;

import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;

import java.util.Arrays;

public enum AMapFacilityCategory {
    RESTAURANT("restaurant", "餐饮", "050000"),
    PARKING("parking", "停车场", "150900"),
    RESTROOM("restroom", "公共厕所", "200300"),
    TRANSIT("transit", "公共交通", "150500|150700");

    private final String value;
    private final String label;
    private final String amapTypes;

    AMapFacilityCategory(String value, String label, String amapTypes) {
        this.value = value;
        this.label = label;
        this.amapTypes = amapTypes;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String amapTypes() {
        return amapTypes;
    }

    public static AMapFacilityCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return RESTAURANT;
        }
        return Arrays.stream(values())
                .filter(category -> category.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCodeEnum.PARAM_ERROR.getCode(),
                        "category must be restaurant, parking, restroom or transit"));
    }
}
