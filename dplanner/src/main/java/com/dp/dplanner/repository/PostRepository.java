package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByClubId(Long id);
}
