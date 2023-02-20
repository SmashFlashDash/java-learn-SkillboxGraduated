package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.classes.SiteIndexingTask;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;

    @Override
    public void startIndexingSites() {
        // TODO: надо индексировать сайты потоком на каждый сайт ForkJoinPool
//        В сервисе индексации сайтов пропишите код, который будет брать из
//        конфигурации приложения список сайтов и по каждому сайту:
//удалять все имеющиеся данные по этому сайту (записи из таблиц site и page);
//создавать в таблице site новую запись со статусом INDEXING;
//обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
//в процессе обхода постоянно обновлять дату и время в поле status_time таблицы site на текущее;
//по завершении обхода изменять статус (поле status) на INDEXED;
//если произошла ошибка и обход завершить не удалось, изменять статус на FAILED и вносить в поле last_error понятную информацию о произошедшей ошибке.
//Для перехода по очередной ссылке должен создаваться новый поток при
//        помощи Fork-Join. Этот поток будет получать содержимое страницы и
//        перечень ссылок, которые есть на этой странице (значений атрибутов
//        href HTML-тегов <a>), при помощи JSOUP

        // можно сделать потокобезопасный метод, который делает записи в БД в сервисе
        // можно сделать очередь, класть туда запросы,
        List<Site> sitesList = sites.getSites();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        for (Site s : sitesList){
//            forkJoinPool.invoke(new SiteIndexingTask(s));
//        }
        // TODO: надо написать репозиторий для page и site и подключить в сервис
    }

    @Override
    public void stopIndexingSites() {

    }
}
