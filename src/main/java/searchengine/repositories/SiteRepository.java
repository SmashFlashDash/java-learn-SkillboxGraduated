package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    @Transactional
    Integer deleteByName(String name);
    @Transactional
    List<SiteEntity> deleteAllByNameIn(List<String> names);
    List<SiteEntity> findAllByNameIn(List<String> names);
    SiteEntity findByName(String name);
}
