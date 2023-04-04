package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    void deleteAllByName(String name);

    SiteEntity findByName(String name);

    SiteEntity findByUrlEquals(String url);
}
