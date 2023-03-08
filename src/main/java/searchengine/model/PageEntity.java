package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "page")
// hibernate не поддерживает указать длину индекса на поле TEXT
// что обязательно в mySQL, поэтому индекс создается в schema.sql
//@Table(name = "page", indexes = @Index(columnList = "path(50)"))
@NoArgsConstructor
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    //    @Column(name = "siteId",nullable = false)
//    Long siteId;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "siteId", insertable = false, updatable = false)
//    SiteEntity siteEntity;
    // заменить на entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "TEXT", nullable = false)
    String path;

    @Column(nullable = false)
    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    List<IndexEntity> indexes;

    public PageEntity(SiteEntity site, String path, Integer code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    // TODO: поле получить все lemma для данной page
//     @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//     @JoinColumn(name = "site_id", nullable = false)
//     List<LemmaEntity> lemmas;
//     @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//     List<LemmaEntity> lemmas;

//     @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//     @JoinTable(name = "`index`",
//             joinColumns = {@JoinColumn(name = "lemma_id")},
//             inverseJoinColumns = {@JoinColumn(name = "page_id")}
//     )
//     List<LemmaEntity> lemmas;

    // @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    // @JoinTable(name = "`index`",
    //         joinColumns = {@JoinColumn(name = "page_id")},
    //         inverseJoinColumns = {@JoinColumn(name = "lemma_id")}
    // )
    // Set<PageEntity> page;
}
