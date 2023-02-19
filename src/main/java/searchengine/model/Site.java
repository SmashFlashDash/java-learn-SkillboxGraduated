package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')",
            nullable = false)
//    @Column(nullable = false)
//    @Enumerated(EnumType.STRING)
    EnumSiteStatus status;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date statusTime;

    @Column(columnDefinition = "TEXT")
    String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String name;
}
