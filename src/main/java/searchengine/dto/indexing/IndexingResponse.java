package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean b) {
    }
}
