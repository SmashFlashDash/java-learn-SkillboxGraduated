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

    // TODO: если здесь добавить cascade не работает startIndexing при delete site
    //  если не добавить не работает pageReposituy.delete(PageEntity) в Indexpag
    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
    List<IndexEntity> indexes;

    @ManyToMany(mappedBy = "pages", fetch = FetchType.LAZY)
    List<LemmaEntity> lemmas;

    public PageEntity(SiteEntity site, String path, Integer code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
