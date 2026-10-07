package com.unime.securegame.repository;

import com.unime.securegame.model.PhishingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhishingTemplateRepository extends JpaRepository<PhishingTemplate, Long> {
    List<PhishingTemplate> findAllByEnabledTrueOrderByIdAsc();
    Optional<PhishingTemplate> findByUrl(String url);
}
