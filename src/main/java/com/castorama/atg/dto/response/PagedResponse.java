package com.castorama.atg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Generic paginated wrapper.
 * ATG analogy: RQL QueryOptions maxResults/startIndex pagination metadata
 * returned alongside query results.
 *
 * @param <T> the element type
 */
@Data
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
