package com.project.BharatConnect.dto.common;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorPageResponse<T> {

    private List<T> items;

    private String nextCursor;

    private boolean hasMore;
}
