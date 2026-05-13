package com.quertimizer.community.adapter.in.http.request;

import com.quertimizer.community.application.input.CommunityPostInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CommunityPostSaveReq {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 최대 200자까지 입력할 수 있습니다.")
    private String title;

    @Size(max = 500000, message = "본문은 최대 500000 Byte까지 입력할 수 있습니다.")
    private String contentJson;

    @Size(max = 2000, message = "본문 요약은 최대 2000자까지 입력할 수 있습니다.")
    private String plainTextSummary;

    @Builder.Default
    @Size(max = 100, message = "이미지는 최대 100개까지 등록할 수 있습니다.")
    private List<@NotBlank(message = "이미지 번호를 입력해 주세요.") @Size(max = 120, message = "이미지 번호는 최대 120자까지 입력할 수 있습니다.") String> imageIds =
            new ArrayList<>();

    @Builder.Default
    @Size(max = 10, message = "태그는 최대 10개까지 등록할 수 있습니다.")
    private List<@NotBlank(message = "태그를 입력해 주세요.") @Size(max = 100, message = "태그는 최대 100자까지 입력할 수 있습니다.") String> tags =
            new ArrayList<>();

    @Size(max = 20, message = "구분은 최대 20자까지 입력할 수 있습니다.")
    private String category;

    public CommunityPostInput toCommunityPostInput() {
        return new CommunityPostInput(title, contentJson, plainTextSummary, imageIds, tags, category);
    }
}
