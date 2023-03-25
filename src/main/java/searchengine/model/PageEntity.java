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
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "TEXT", nullable = false)
    String path;

    @Column(nullable = false)
    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    String content;

    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<IndexEntity> indexes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "index",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
    List<LemmaEntity> lemmas;

    public PageEntity(SiteEntity site, String path, Integer code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

}
