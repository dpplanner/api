package com.dp.dplanner.repository;

import com.dp.dplanner.domain.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    Optional<InviteCode> findInviteCodeByCode(String code);
}
