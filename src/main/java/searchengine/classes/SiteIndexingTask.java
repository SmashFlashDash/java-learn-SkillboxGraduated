package searchengine.classes;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.JsoupConfig;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveAction;

//@Component
//@Scope
//@RequiredArgsConstructor
// TODO: нужно ли делать callable или RecursiveTask чтобы обновлял интрефейс в процессе индексации
public class SiteIndexingTask extends RecursiveAction implements Callable<String> {

    // TODO: можно ли внедрить service и config не делая класс Component
    //  или сделать компонентом и настроить Scope(prototype)
    // private final JsoupConfig jsoupConfig;
    private final IndexingService indexingService;

    public SiteIndexingTask(String url, JsoupConfig jsoupConfig, IndexingService indexingService) {
        this.indexingService = indexingService;
        try {
            System.out.println("Started: " + url);
            // TODO: проверить есть ли в таблице запись
            //  если нет то вставить и начать парсить индексацию
            //  если есть Exception

            Document doc = Jsoup.connect(url)
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReffer())
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void compute() {

    }

    /**
     * вызвать сервис у которого аннотация transactional
     */
    private void writeToDB() {

    }

    @Override
    public String call() throws Exception {
        return null;
    }
}
