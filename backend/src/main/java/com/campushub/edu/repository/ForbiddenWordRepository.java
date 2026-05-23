package com.campushub.edu.repository;

import com.campushub.edu.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {

    List<ForbiddenWord> findAllByOrderByWordAsc();
}
