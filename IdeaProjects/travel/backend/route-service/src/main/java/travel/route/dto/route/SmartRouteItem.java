package travel.route.dto.route;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;

public class SmartRouteItem {
    private Integer routeId;
    private String title;
    private String description;
    private Integer durationDays;
    private String difficulty;
    private String coverImage;
    private Integer viewCount;
    private Integer likeCount;
    private Integer attractionCount;
    private Double similarity;
    private String preference;
    private String season;
    private String theme;
    private OptimalRoute route;

    public SmartRouteItem() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getAttractionCount() { return attractionCount; }
    public void setAttractionCount(Integer attractionCount) { this.attractionCount = attractionCount; }
    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public OptimalRoute getRoute() { return route; }
    public void setRoute(OptimalRoute route) { this.route = route; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SmartRouteItem item = new SmartRouteItem();
        public Builder routeId(Integer routeId) { item.routeId = routeId; return this; }
        public Builder title(String title) { item.title = title; return this; }
        public Builder description(String description) { item.description = description; return this; }
        public Builder durationDays(Integer durationDays) { item.durationDays = durationDays; return this; }
        public Builder difficulty(String difficulty) { item.difficulty = difficulty; return this; }
        public Builder coverImage(String coverImage) { item.coverImage = coverImage; return this; }
        public Builder viewCount(Integer viewCount) { item.viewCount = viewCount; return this; }
        public Builder likeCount(Integer likeCount) { item.likeCount = likeCount; return this; }
        public Builder attractionCount(Integer attractionCount) { item.attractionCount = attractionCount; return this; }
        public Builder similarity(Double similarity) { item.similarity = similarity; return this; }
        public Builder preference(String preference) { item.preference = preference; return this; }
        public Builder season(String season) { item.season = season; return this; }
        public Builder theme(String theme) { item.theme = theme; return this; }
        public Builder route(OptimalRoute route) { item.route = route; return this; }
        public SmartRouteItem build() { return item; }
    }
}
