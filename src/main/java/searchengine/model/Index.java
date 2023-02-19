package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@Getter
@Setter
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long pageId;

    @Column(nullable = false)
    Long lemmaId;

    @Column(name = "`rank`", nullable = false)
    Float rank;

}
