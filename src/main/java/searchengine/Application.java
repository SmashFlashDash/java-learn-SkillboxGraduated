package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // включить в IDE Build-AnnotationPocessor-enable
        // установить в IDE lombock-plugin чтобы видел методы сгенрированные аннотациями
        SpringApplication.run(Application.class, args);
    }
}
