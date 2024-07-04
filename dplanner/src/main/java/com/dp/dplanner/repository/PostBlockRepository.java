package com.dp.dplanner.repository;

import com.dp.dplanner.domain.PostBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostBlockRepository extends JpaRepository<PostBlock, PostBlock.PostBlockId> {
}
