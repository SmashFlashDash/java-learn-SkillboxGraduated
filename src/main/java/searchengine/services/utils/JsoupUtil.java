package searchengine.services.utils;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class JsoupUtil {
    public static Elements documentContnetSelector(Document document) {
        return document.select(" h1,h2,h3,h4,h5,h6,p,ul,ol,div[class*=content]")
                .not("nav,aside,header,footer,[class*=menu]");
                // .select("p, ul, ol");
                // .select("h1 ~ *, h2 ~ *, h3 ~ *, h4 ~ *");
    }
}
