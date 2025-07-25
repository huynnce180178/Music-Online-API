package com.ra.base_spring_boot.dto.req;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequestDTO {
    private Long songId;
    private String content;
    private Long parentId;
}
