package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.service.ClubService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import({TestConfig.class})
@Transactional
public class ClubServiceIntegrationTests {

    @Autowired
    ClubAuthorityRepository clubAuthorityRepository;
    @Autowired
    ClubService clubService;
    @Autowired
    EntityManager entityManager;

    ClubMember clubMember;

    @BeforeEach
    void setup() {
        Member member = Member.builder().build();
        Club club = Club.builder().clubName("club").build();
        clubMember = ClubMember.builder().club(club).member(member).build();
        clubMember.setAdmin();
        clubMember.confirm();

        entityManager.persist(member);
        entityManager.persist(club);
        entityManager.persist(clubMember);
    }

    @Test
    @DisplayName("관리자가 클럽 매니저의 권한을 변경하면 변경사항이 DB에 반영되어야 함")
    public void setManagerAuthorityByAdmin() throws Exception {
        //given
        Member member = clubMember.getMember();
        Club club = clubMember.getClub();

        ClubAuthority clubAuthority = new ClubAuthority(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.SCHEDULE_ALL), "name", "description");

        entityManager.persist(clubAuthority);

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(
                clubAuthority.getId(),
                club.getId(),
                "updateName",
                "updateDescription",
                List.of(ClubAuthorityType.POST_ALL.name()));

        clubService.updateClubAuthority(member.getId(), updateDto);

        //then
        List<ClubAuthority> authorities = clubAuthorityRepository.findAllByClub(club);
        List<ClubAuthorityType> authorityTypes = authorities.get(0).getClubAuthorityTypes();

        assertThat(authorityTypes).as("게시판 권한이 포함되어야 함")
                .contains( ClubAuthorityType.POST_ALL);
        assertThat(authorityTypes).as("회원 권한과 스케줄 권한이 포함되지 않아야 함")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.SCHEDULE_ALL);
    }
}



