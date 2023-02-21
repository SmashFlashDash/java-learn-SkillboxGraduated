package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    void deleteByName(String name);
    void deleteAllByNameIn(List<String> names);
}
