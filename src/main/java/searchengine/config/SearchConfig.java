package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.services.search.Snippet;
import searchengine.services.search.SnippetParser;

import javax.annotation.PostConstruct;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "search-settings")
public class SearchConfig {
    public Integer snippetLength;

    @PostConstruct
    private void init() {
        SnippetParser.setMaxSnippetLength(snippetLength);
        Snippet.setMaxSnippetLength(snippetLength);
    }
}
