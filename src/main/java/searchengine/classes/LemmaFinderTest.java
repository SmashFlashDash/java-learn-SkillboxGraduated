package searchengine.classes;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import searchengine.Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LemmaFinderTest {

    @SneakyThrows
    public static void main(String[] args) {
        LemmaFinder lf = LemmaFinder.getInstance();
        String text = new String(Files.readAllBytes(Paths.get("test_html_remove_tags.html")));

        lf.collectLemmas("леса плывут по моюре и лсм непонятное");
        lf.getLemmaSet("леса плывут по моюре и лсм непонятное");
    }
}
