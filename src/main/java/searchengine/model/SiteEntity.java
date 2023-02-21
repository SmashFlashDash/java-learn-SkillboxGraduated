package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

//    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')",
//            nullable = false)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    EnumSiteStatus status;

    @UpdateTimestamp
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date statusTime;

    @Column(columnDefinition = "TEXT")
    String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteEntity", cascade = CascadeType.REMOVE)
    List<PageEntity> pages;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteEntity", cascade = CascadeType.REMOVE)
    List<LemmaEntity> lemmas;
}
