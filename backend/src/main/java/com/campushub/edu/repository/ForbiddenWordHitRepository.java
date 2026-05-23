package com.campushub.edu.repository;

import com.campushub.edu.entity.ForbiddenWordHit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForbiddenWordHitRepository extends JpaRepository<ForbiddenWordHit, Long> {
}
