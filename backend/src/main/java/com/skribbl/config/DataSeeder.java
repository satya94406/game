package com.skribbl.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skribbl.entity.Word;
import com.skribbl.repository.WordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seeds the word bank from {@code words.json} on first startup.
 * Structure of the file: {@code { "<language>": { "<category>": ["word", ...] } }}.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final WordRepository wordRepository;
    private final ObjectMapper objectMapper;

    public DataSeeder(WordRepository wordRepository, ObjectMapper objectMapper) {
        this.wordRepository = wordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        long existing = wordRepository.count();
        if (existing > 0) {
            log.info("Word bank already seeded ({} words); skipping.", existing);
            return;
        }

        try (InputStream in = new ClassPathResource("words.json").getInputStream()) {
            Map<String, Map<String, List<String>>> data =
                    objectMapper.readValue(in, new TypeReference<>() {
                    });

            List<Word> toSave = new ArrayList<>();
            data.forEach((language, categories) ->
                    categories.forEach((category, words) ->
                            words.forEach(w -> toSave.add(
                                    new Word(w.trim().toLowerCase(), category, language)))));

            wordRepository.saveAll(toSave);
            log.info("Seeded {} words across {} language(s).", toSave.size(), data.size());
        }
    }
}
