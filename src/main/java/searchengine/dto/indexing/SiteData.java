package searchengine.dto.indexing;

import lombok.Data;

import java.net.URL;

@Data
public class SiteData {
    String name;
    URL url;
    Integer millis;
}
