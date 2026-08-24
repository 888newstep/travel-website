package travel.collection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import travel.common.entity.user_community.TravelNote;

import java.util.List;

@Data
public class TravelNoteWriteRequest {

    @NotNull(message = "游记内容不能为空")
    @Valid
    private TravelNotePayload travelNote;

    @Size(max = 20, message = "游记标签最多20个")
    private List<@NotBlank(message = "标签不能为空") @Size(max = 30, message = "单个标签最多30个字符") String> tags;

    @Data
    public static class TravelNotePayload {
        private Integer id;
        private Integer userId;

        @NotBlank(message = "游记标题不能为空")
        @Size(max = 100, message = "游记标题最多100个字符")
        private String title;

        @NotBlank(message = "游记内容不能为空")
        @Size(max = 20000, message = "游记内容最多20000个字符")
        private String content;

        @Size(max = 2048, message = "封面地址过长")
        private String coverImage;

        @Size(max = 20000, message = "图片数据过长")
        private String images;

        @Positive(message = "城市ID必须为正整数")
        private Integer cityId;

        private Boolean isPublic;

        public TravelNote toEntity() {
            TravelNote note = new TravelNote();
            note.setTitle(title);
            note.setContent(content);
            note.setCoverImage(coverImage);
            note.setImages(images);
            note.setCityId(cityId);
            note.setIsPublic(isPublic);
            return note;
        }
    }
}
