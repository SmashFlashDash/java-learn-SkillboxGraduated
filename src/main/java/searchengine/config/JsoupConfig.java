package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jsoup-settings")
public class JsoupConfig {
    private String userAgent;
    private String reffer;
    private Integer socketTimeout;

    public Document getJsoupDocument(String url, Integer millis) throws IOException {
        if (millis != null) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Jsoup.connect(url)
                .userAgent(getUserAgent())
                .referrer(getReffer())
                .timeout(getSocketTimeout())
                .method(Connection.Method.GET)
                .execute().parse();
    }

    public Document getJsoupDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(getUserAgent())
                .referrer(getReffer())
                .timeout(getSocketTimeout())
                .method(Connection.Method.GET)
                .execute().parse();
    }
}
