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
@NoArgsConstructor
//@Table(name = "page", indexes = @Index(columnList = "path(50)"))
//@Table(name = "page",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"path"})
//)
// Caused by: java.sql.SQLSyntaxErrorException: BLOB/TEXT column 'path' used in key specification without a key length
// hibernate не поддерживает указать длину индекса на поле TEXT что обязательно в mySQL, поэтому индекс создается в schema.sql
// создать индекс отдельно в schema.sql
// или создать таблицы для бд в schema.sql и выключить ddl-auto
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

    // TODO: если здесь добавьит cascade не работает startIndexing при delete site
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
