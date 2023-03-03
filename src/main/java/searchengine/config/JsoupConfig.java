package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jsoup-settings")
public class JsoupConfig {
    private String userAgent;
    private String reffer;
    private Integer socketTimeout;
}
