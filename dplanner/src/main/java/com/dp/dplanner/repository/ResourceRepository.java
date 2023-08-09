package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource,Long> {

    List<Resource> findByClubId(Long clubId);

}
