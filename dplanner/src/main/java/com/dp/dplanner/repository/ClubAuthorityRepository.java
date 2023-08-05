package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubAuthorityRepository extends JpaRepository<ClubAuthority, Long> {

    List<ClubAuthority> findAllByClub(Club club);

    void deleteAllByClub(Club club);
}
