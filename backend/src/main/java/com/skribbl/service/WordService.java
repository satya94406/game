package com.skribbl.service;

import com.skribbl.entity.Word;
import com.skribbl.game.RoomSettings;
import com.skribbl.repository.WordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class WordService {

    private final WordRepository wordRepository;

    public WordService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    public List<String> categories(String language) {
        return wordRepository.findCategories(language);
    }

    /** A fresh random set of {@code wordCount} options for the drawer, from the room's categories. */
    public List<String> randomWords(RoomSettings settings) {
        String language = settings.getLanguage();
        List<Word> pool;
        if (settings.getCategories() == null || settings.getCategories().isEmpty()) {
            pool = wordRepository.findByLanguage(language);
        } else {
            pool = wordRepository.findByLanguageAndCategoryIn(language, settings.getCategories());
        }
        if (pool.isEmpty()) {
            pool = wordRepository.findByLanguage(language); // fall back to the whole bank
        }

        List<Word> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        int n = Math.min(settings.getWordCount(), shuffled.size());
        List<String> words = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            words.add(shuffled.get(i).getText());
        }
        return words;
    }
}
