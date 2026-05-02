package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import travel.entity.travel_recommendation.City;

import java.time.LocalDateTime;
import java.util.List;

@TableName("travel_notes")
public class TravelNote {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("cover_image")
    private String coverImage;

    @TableField("images")
    private String images;

    @TableField("city_id")
    private Integer cityId;

    @TableField("views_count")
    private Integer viewsCount;

    @TableField("likes_count")
    private Integer likesCount;

    @TableField("comments_count")
    private Integer commentsCount;

    @TableField("is_public")
    private Boolean isPublic;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 关联关系
    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private City city;

    @TableField(exist = false)
    private List<TravelNoteComment> comments;

    @TableField(exist = false)
    private List<TravelNoteLike> likes;

    @TableField(exist = false)
    private List<TravelNoteTag> tags;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public List<TravelNoteComment> getComments() {
        return comments;
    }

    public void setComments(List<TravelNoteComment> comments) {
        this.comments = comments;
    }

    public List<TravelNoteLike> getLikes() {
        return likes;
    }

    public void setLikes(List<TravelNoteLike> likes) {
        this.likes = likes;
    }

    public List<TravelNoteTag> getTags() {
        return tags;
    }

    public void setTags(List<TravelNoteTag> tags) {
        this.tags = tags;
    }
}