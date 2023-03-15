package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "site")
@NoArgsConstructor
public class SiteEntity {

    public SiteEntity(String name, String url, EnumSiteStatus status) {
        this.name = name;
        this.url = url;
        this.status = status;
    }

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

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<PageEntity> pages;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<LemmaEntity> lemmas;

//    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
//    List<PageEntity> pages;
//
//    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
//    List<LemmaEntity> lemmas;
}
