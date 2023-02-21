package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "page")
// hibernate не поддерживает указать длину индекса на поле TEXT
// что обязательно в mySQL, поэтому индекс создается в schema.sql
//@Table(indexes = @Index(columnList = "path(50)"))
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "siteId",nullable = false)
    Long siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    String path;

    @Column(nullable = false)
    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", insertable = false, updatable = false)
    SiteEntity siteEntity;
}
