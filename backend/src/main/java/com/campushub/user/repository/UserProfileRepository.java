package com.campushub.user.repository;

import com.campushub.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /** admin 用户搜索（按昵称模糊，最多取前若干条）。 */
    List<UserProfile> findTop20ByNicknameContainingIgnoreCaseOrderByUserIdAsc(String nickname);
}
