package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    LemmaEntity findBySiteIdAndLemma(Long siteId, String lemma);

    @Modifying
    @Query("DELETE FROM LemmaEntity l WHERE l.id IN (:indexes) AND l.frequency = 1")
    void deleteOneFrequencyLemmaByIndexes(List<Long> indexes);

    @Modifying
    @Query("UPDATE LemmaEntity l SET l.frequency = l.frequency - 1 WHERE l.id IN (:indexes)")
    void updateBeforeDeleteIndexes(List<Long> indexes);

    List<LemmaEntity> findAllByLemmaInOrderByFrequencyAsc(Iterable<String> s);

    List<LemmaEntity> findAllByLemmaInAndSiteEqualsOrderByFrequencyAsc(Iterable<String> s, SiteEntity site);
}
