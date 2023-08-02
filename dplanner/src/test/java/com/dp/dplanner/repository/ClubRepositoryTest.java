package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class ClubRepositoryTest {

    @Autowired
    ClubRepository clubRepository;

    @Autowired
    TestEntityManager testEntityManager;
    
    @Test
    @DisplayName("Club이 정상적으로 저장됨")
    public void save() throws Exception {
        //given
        Club club = Club.builder().clubName("newClub").info("newClubInfo").build();
        //when
        Club savedClub = clubRepository.save(club);
        //then
        assertThat(savedClub.getId()).isNotNull();
        assertThat(savedClub.getClubName()).isEqualTo("newClub");
        assertThat(savedClub.getInfo()).isEqualTo("newClubInfo");
    }

    @Test
    @DisplayName("clubId로 클럽 조회")
    public void findById() throws Exception {
        //given
        Club club = Club.builder().clubName("newClub").info("newClubInfo").build();
        Club savedClub = clubRepository.save(club);
        //when
        Club findClub = clubRepository.findById(savedClub.getId()).orElse(null);
        //then
        assertThat(findClub.getId()).isEqualTo(savedClub.getId());
        assertThat(findClub.getClubName()).isEqualTo("newClub");
        assertThat(findClub.getInfo()).isEqualTo("newClubInfo");
    }
}
