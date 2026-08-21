package travel.attraction.dto;

import java.time.LocalDateTime;

public class AttractionWarning {
    private String warnId;
    private Long attractionId;
    private String warnType;
    private String warnLevel;
    private String warnMessage;
    private LocalDateTime createTime;
    private String status;

    public String getWarnId() { return warnId; }
    public void setWarnId(String warnId) { this.warnId = warnId; }
    public Long getAttractionId() { return attractionId; }
    public void setAttractionId(Long attractionId) { this.attractionId = attractionId; }
    public String getWarnType() { return warnType; }
    public void setWarnType(String warnType) { this.warnType = warnType; }
    public String getWarnLevel() { return warnLevel; }
    public void setWarnLevel(String warnLevel) { this.warnLevel = warnLevel; }
    public String getWarnMessage() { return warnMessage; }
    public void setWarnMessage(String warnMessage) { this.warnMessage = warnMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
