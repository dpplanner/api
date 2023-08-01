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

import java.util.ArrayList;
import java.util.List;
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
    
    @Test
    @DisplayName("사용자는 clubId로 클럽을 조회할 수 있음")
    public void findClubByClubId() throws Exception {
        //given
        Long clubId = 1L;
        Club club = Club.builder().clubName("newClub").info("newClubInfo").build();
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        //when
        ClubDTO clubDTO = clubService.findClubById(clubId);
        
        //then
        assertThat(clubDTO).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(clubDTO.getClubName()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽이름)").isEqualTo("newClub");
        assertThat(clubDTO.getInfo()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽정보)").isEqualTo("newClubInfo");

    }

    @Test
    @DisplayName("clubId로 조회시 클럽이 없으면 NoSuchElementException")
    public void findClubByWrongIdThenException() throws Exception {
        //given
        Long wrongId = 2L;
        given(clubRepository.findById(wrongId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubService.findClubById(wrongId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("사용자는 자신이 속한 클럽들을 조회할 수 있음")
    public void findMyClub() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        List<Club> clubs = preparedClubs(5); // clubs = [club0, club1, club2, club3, club4]
        setMyClubs(clubs, 2, 4);         // myClubs = [club2, club3]

        //when
        List<ClubDTO> clubDTOS = clubService.findMyClubs(memberId);

        //then
        assertThat(clubDTOS).as("결과가 존재해야 함").isNotNull();

        List<String> clubNames = clubDTOS.stream().map(ClubDTO::getClubName).toList();
        assertThat(clubNames).as("내가 속한 클럽만 포함해야 함").containsExactly("club2", "club3");
    }

    @Test
    @DisplayName("자신이 속한 클럽이 없으면 빈 리스트를 반환")
    public void findMyClubThenEmptyList() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        List<Club> clubs = preparedClubs(5); // clubs = [club0, club1, club2, club3, club4]

        //when
        List<ClubDTO> clubDTOS = clubService.findMyClubs(memberId);

        //then
        assertThat(clubDTOS).as("결과가 존재해야 함").isNotNull();
        assertThat(clubDTOS.isEmpty()).as("빈 리스트가 반환되어야 함").isTrue();
    }

    @Test
    @DisplayName("가입되지 않은 사용자가 클럽을 조회하면 NoSuchElementException")
    public void findMyClubByAbstractMemberThenException() throws Exception {
        //given
        Long abstractMemberId = 2L;
        given(memberRepository.findById(abstractMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubService.findMyClubs(abstractMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }
    
    @Test
    @DisplayName("관리자는 자신의 클럽 정보를 수정할 수 있다.")
    public void updateClubInfoByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = Club.builder().info("clubInfo").build();
        ClubMember clubMember = prepareClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.of(clubMember));
        
        //when
        ClubDTO clubDTO = clubService.updateClubInfo(clubId, memberId, "updatedClubInfo");
        
        //then
        assertThat(clubDTO).as("결과가 존재해야 함").isNotNull();
        assertThat(clubDTO.getInfo()).as("클럽 정보가 요청한대로 변경되어야 함").isEqualTo("updatedClubInfo");
    }

    @Test
    @DisplayName("관리자가 아닌 사용자가 클럽정보를 수정하려 하면 IllegalStateException")
    public void updateClubInfoByNotAdminThenException() throws Exception {
        //given
        Long clubId1 = 1L;
        Club club1 = Club.builder().info("clubInfo1").build();
        ClubMember clubMember1 = ClubMember.builder().club(club1).member(member).build();
        given(clubMemberRepository.findByClubIdAndMemberId(clubId1, memberId)).willReturn(Optional.of(clubMember1));

        Long clubId2 = 2L;
        Club club2 = Club.builder().info("clubInfo2").build();
        ClubMember clubMember2 = ClubMember.builder().club(club2).member(member).build();
        clubMember2.setManager();
        given(clubMemberRepository.findByClubIdAndMemberId(clubId2, memberId)).willReturn(Optional.of(clubMember2));

        assert clubMember1.getRole() != ClubRole.ADMIN && clubMember2.getRole() != ClubRole.ADMIN;

        //when
        //then
        assertThatThrownBy(() -> clubService.updateClubInfo(clubId1, memberId, "updatedClubInfo"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> clubService.updateClubInfo(clubId2, memberId, "updatedClubInfo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("클럽에 가입되지 않은 회원이 클럽 정보를 수정하려 하면 IllegalStateException")
    public void updateClubInfoByNotClubMemberThenException() throws Exception {
        //given
        Long clubId = 1L;
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubService.updateClubInfo(clubId, memberId, "updatedClubInfo"))
                .isInstanceOf(IllegalStateException.class);
    }



    private ClubMember prepareClubMemberAsAdmin(Club club) {
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }

    private void setMyClubs(List<Club> clubs, int from, int to) {
        List<Club> myClubs = clubs.subList(from, to);
        myClubs.forEach(this::prepareClubMemberAsAdmin);
    }

    private List<Club> preparedClubs(int size) {
        List<Club> clubs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Club club = Club.builder().clubName("club" + i).build();
            clubs.add(club);
        }
        return clubs;
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
