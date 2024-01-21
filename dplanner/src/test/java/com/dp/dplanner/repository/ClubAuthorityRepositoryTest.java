package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class ClubAuthorityRepositoryTest {

    @Autowired
    ClubAuthorityRepository clubAuthorityRepository;

    @Autowired
    TestEntityManager testEntityManager;


    @Test
    @DisplayName("ClubAuthority 저장")
    public void save() throws Exception {
        //given
        Club club = Club.builder().build();
        testEntityManager.persist(club);

        //when
        ClubAuthority clubAuthority = ClubAuthority.builder()
                .club(club)
                .clubAuthorityTypes(List.of(ClubAuthorityType.MESSAGE_ALL,ClubAuthorityType.MEMBER_ALL))
                .build();

        clubAuthorityRepository.save(clubAuthority);

        //then
        List<ClubAuthority> findAuthorities = clubAuthorityRepository.findAllByClub(club);
        List<ClubAuthorityType> authorityTypes = findAuthorities.get(0).getClubAuthorityTypes();

        assertThat(authorityTypes).as("메세지 권한과 회원 권한을 포함해야 함")
                .contains(ClubAuthorityType.MESSAGE_ALL, ClubAuthorityType.MEMBER_ALL);
        assertThat(authorityTypes).as("스케줄 권한과 게시판 권한을 포함하지 않아야 함")
                .doesNotContain(ClubAuthorityType.POST_ALL, ClubAuthorityType.SCHEDULE_ALL);
    }

    @Test
    @DisplayName("특정 Club의 ClubAuthority 전체 삭제")
    public void deleteAllByClub() throws Exception {
        //given
        Club club = Club.builder().build();
        testEntityManager.persist(club);

        ClubAuthority clubAuthority = ClubAuthority.builder()
                .club(club)
                .clubAuthorityTypes(List.of(ClubAuthorityType.MESSAGE_ALL,ClubAuthorityType.MEMBER_ALL))
                .build();

        clubAuthorityRepository.save(clubAuthority);

        //when
        clubAuthorityRepository.deleteAllByClub(club);

        //then
        List<ClubAuthority> findAuthorities = clubAuthorityRepository.findAllByClub(club);
        assertThat(findAuthorities.isEmpty()).as("해당 클럽에 ClubAuthority 데이터가 없어야 함.").isTrue();
    }

}
