package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.dto.ClubDTO;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ClubServiceTest {

    @Mock
    ClubRepository clubRepository;

    @Mock
    ClubMemberRepository clubMemberRepository;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    ClubService clubService;

    Long memberId;
    Member member;
    @BeforeEach
    void setUp() {
        //레포지토리에 미리 저장된 member
        memberId = 1L;
        member = Member.builder().name("user").build();
    }

    @Test
    @DisplayName("사용자는 클럽을 생성할 수 있음")
    public void createClubSuccess() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> invocation.getArgument(0));

        //when
        ClubDTO clubDTO = clubService.createClub(memberId, "newClub", "newClubInfo");

        //then
        assertThat(clubDTO).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(clubDTO.getClubName()).as("반환된 DTO의 이름이 요청한 이름과 동일해야 함").isEqualTo("newClub");
        assertThat(clubDTO.getInfo()).as("반환된 DTO의 정보가 요청한 정보와 동일해야 함").isEqualTo("newClubInfo");

        Club club = captureClubFromMockRepository();

        assertThat(club).as("레포지토리에 실제 클럽 객체가 저장되어야 함").isNotNull();
        assertThat(club.getClubName()).as("저장된 클럽의 이름이 요청한 이름과 동일해야 함").isEqualTo("newClub");
        assertThat(club.getInfo()).as("저장된 클럽의 정보가 요청한 정보와 동일해야 함").isEqualTo("newClubInfo");
    }

    @Test
    @DisplayName("클럽을 생성한 사용자는 자동으로 클럽회원에 등록됨")
    public void createClubThenAdmin() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> invocation.getArgument(0));

        //when
        clubService.createClub(memberId, "newClub", "newClubInfo");

        //then
        Club club = captureClubFromMockRepository();
        ClubMember clubMember = captureClubMemberFromMockRepository();

        assertThat(clubMember).as("레포지토리에 실제 ClubMember 객체가 저장되어야 함").isNotNull();
        assertThat(clubMember.getMember()).as("ClubMember는 클럽을 생성한 회원을 담고 있어야 함").isEqualTo(member);
        assertThat(clubMember.getClub()).as("ClubMember는 생성된 클럽을 담고 있어야 함").isEqualTo(club);
        assertThat(clubMember.getRole()).as("클럽을 생성한 회원은 ADMIN 권한을 가져야 함").isEqualTo(ClubRole.ADMIN);
        assertThat(clubMember.getIsConfirmed()).as("클럽을 생성한 회원은 자동으로 승인되어야 함").isTrue();
    }

    @Test
    @DisplayName("서비스에 가입되지 않은 회원이 클럽을 생성하려고 하면 NoSuchElementException")
    public void createClubByAbstractMemberThenException() throws Exception {
        //given
        Long abstractMemberId = 2L;
        given(memberRepository.findById(abstractMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubService.createClub(abstractMemberId, "newClub", "newClubInfo"))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Club captureClubFromMockRepository() {
        ArgumentCaptor<Club> captor = ArgumentCaptor.forClass(Club.class);
        then(clubRepository).should().save(captor.capture());
        return captor.getValue();
    }

    private ClubMember captureClubMemberFromMockRepository() {
        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        then(clubMemberRepository).should().save(captor.capture());
        return captor.getValue();
    }

}
