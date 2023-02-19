package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    String path;

    @Column(nullable = false)
    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    String content;
}
