package com.campushub.credit.repository;

import com.campushub.credit.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {

    Optional<CreditAccount> findByUserId(long userId);

    boolean existsByUserId(long userId);
}
