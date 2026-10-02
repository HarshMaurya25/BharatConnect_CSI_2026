package com.project.BharatConnect.mapper;

import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {

    public ContentResponseDto toResponse(Content content , String displayName , String username){
        return ContentResponseDto
                .builder()
                .id(content.getId())
                .username(username)
                .displayName(displayName)
                .contentName(content.getContentName())
                .text(content.getText())
                .contentType(content.getContentType())
                .contentUrl(content.getContentUrl())
                .likeCount(content.getLikeCount())
                .commentCount(content.getCommentCount())
                .updatedAt(content.getUpdatedAt())
                .build();
    }

}
