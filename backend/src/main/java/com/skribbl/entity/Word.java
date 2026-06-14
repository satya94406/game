package com.skribbl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A guessable word in the word bank, grouped by category and language.
 * Seeded from {@code words.json} on first startup (see {@link com.skribbl.config.DataSeeder}).
 */
@Entity
@Table(name = "words",
        uniqueConstraints = @UniqueConstraint(columnNames = {"text", "category", "language"}))
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String text;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 8)
    private String language = "en";

    protected Word() {
        // for JPA
    }

    public Word(String text, String category, String language) {
        this.text = text;
        this.category = category;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
