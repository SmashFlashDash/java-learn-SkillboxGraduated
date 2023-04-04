package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "`index`",
        uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "lemma_id"})
)
@NoArgsConstructor
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    PageEntity page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    LemmaEntity lemma;

    @Column(name = "`rank`", nullable = false)
    Float rank;

    public IndexEntity(PageEntity page, LemmaEntity lemma, Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}