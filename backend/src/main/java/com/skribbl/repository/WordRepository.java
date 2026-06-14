package com.skribbl.repository;

import com.skribbl.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Query("select distinct w.category from Word w where w.language = :language order by w.category")
    List<String> findCategories(@Param("language") String language);

    List<Word> findByLanguageAndCategoryIn(String language, Collection<String> categories);

    List<Word> findByLanguage(String language);
}
