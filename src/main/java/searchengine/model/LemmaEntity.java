package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
// @Table(name = "lemma",
//         indexes = {@Index(name = "uniqueLemmaSite", columnList = "site_id, lemma", unique = true)
// })
@Table(name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"})
)
@NoArgsConstructor
public class LemmaEntity {

    public LemmaEntity(SiteEntity site, String lemma, Integer frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // @Column(nullable = false)
    // Long siteId;
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "siteId", insertable = false, updatable = false)
    // SiteEntity siteEntity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String lemma;

    @Column(nullable = false)
    Integer frequency;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY)
    List<IndexEntity> indexes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "index",
            joinColumns = { @JoinColumn(name = "lemma_id") },
            inverseJoinColumns = { @JoinColumn(name = "page_id")}
    )
    List<PageEntity> pages;
}
