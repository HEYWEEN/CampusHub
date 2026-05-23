package com.campushub.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "edu_forbidden_word_hit")
public class ForbiddenWordHit {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    @Column(name = "cooldown_until")
    private Instant cooldownUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ForbiddenWordHit() {}

    public ForbiddenWordHit(Long userId) {
        this.userId = userId;
    }

    public void incrementHit() {
        this.hitCount++;
        this.updatedAt = Instant.now();
    }

    public void startCooldown(Instant until) {
        this.cooldownUntil = until;
        this.updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public int getHitCount() { return hitCount; }
    public Instant getCooldownUntil() { return cooldownUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}
