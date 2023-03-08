package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
public class IndexKey implements Serializable {
    @Column(name = "page_id")
    Long pageId;

    @Column(name = "lemma_id")
    Long lemmaId;
}
