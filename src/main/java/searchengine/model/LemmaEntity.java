package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"})
)
@NoArgsConstructor
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String lemma;

    @Column(nullable = false)
    Integer frequency;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<IndexEntity> indexes;

//    @ManyToMany(mappedBy = "lemmas", fetch = FetchType.LAZY)
//    List<PageEntity> pages;

    public LemmaEntity(SiteEntity site, String lemma, Integer frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
