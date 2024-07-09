package com.dp.dplanner.repository;

import com.dp.dplanner.domain.ClubMemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMemberBlockRepository extends JpaRepository<ClubMemberBlock, ClubMemberBlock.ClubMemberBlockId> {
}
