package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponse {
    private Boolean result;
    private String error;
}
