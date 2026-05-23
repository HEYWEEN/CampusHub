package com.campushub.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "admin_forbidden_word",
    indexes = @Index(name = "uk_admin_forbidden_word", columnList = "word", unique = true)
)
public class ForbiddenWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word", length = 100, nullable = false, unique = true)
    private String word;

    public ForbiddenWord() {}

    public ForbiddenWord(String word) {
        this.word = word;
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
}
