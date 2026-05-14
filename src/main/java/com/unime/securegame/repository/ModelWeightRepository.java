package com.unime.securegame.repository;

import com.unime.securegame.model.ModelWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelWeightRepository extends JpaRepository<ModelWeight, Long> {
    Optional<ModelWeight> findByFeatureName(String featureName);
    List<ModelWeight> findAllByOrderByFeatureIndexAsc();
}
