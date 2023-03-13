package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }

    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
