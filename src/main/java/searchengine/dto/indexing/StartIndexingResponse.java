package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class StartIndexingResponse {
    private boolean result;
    private String error;

    public StartIndexingResponse(boolean b) {
    }
}
