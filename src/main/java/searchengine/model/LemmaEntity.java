package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "lemma")
// TODO: должен быть уникальный по siteId и lemma
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String lemma;

    @Column(nullable = false)
    Integer frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", insertable = false, updatable = false)
    SiteEntity siteEntity;

}
