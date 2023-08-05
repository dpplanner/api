package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ClubMemberServiceTests {

    @Mock
    ClubMemberRepository clubMemberRepository;

    @InjectMocks
    ClubMemberService clubMemberService;

    static Long memberId;
    static Member member;

    static Long clubId;
    static Club club;
    
    @BeforeEach
    void setUp() {
        //레포지토리에 미리 저장된 member
        memberId = 1L;
        member = createMember();
        ReflectionTestUtils.setField(member, "id", memberId);

        //레포지토리에 미리 저장된 club
        clubId = 1L;
        club = createClub("club");
        ReflectionTestUtils.setField(club, "id", clubId);
    }
    
    @Test
    @DisplayName("사용자는 같은 클럽에 속한 회원들의 정보를 조회할 수 있다.")
    public void findMyClubMembers() throws Exception {
        //given
        ClubMember clubMember = createConfirmedClubMember(club, member, "member");
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3);
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]
        given(clubMemberRepository.findAllConfirmedClubMemberByClub(club)).willReturn(clubMembers);

        ClubMember unConfirmedMember = createClubMember(club, createMember(), "unConfirmedMember");
        ClubMember otherClubMember = createClubMember(
                createClub("otherClub"), createMember(), "otherClubMember");

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubId, memberId);

        //then
        assertThat(findClubMembers).as("결과가 존재해야 함").isNotNull();

        List<String> findMemberNames = findClubMembers.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(findMemberNames).as("결과에 본인이 포함되어야 함").contains("member");
        assertThat(findMemberNames).as("결과에 같은 클럽의 다른 회원들이 포함되어야 함")
                .contains("clubMember0", "clubMember1", "clubMember2");
        assertThat(findMemberNames).as("다른 클럽의 회원은 포함되지 않아야 함")
                .doesNotContain("otherClubMember");
        assertThat(findMemberNames).as("결과에 승인되지 않은 회원은 포함되지 않아야 함")
                .doesNotContain("unConfirmedMember");
    }

    @Test
    @DisplayName("관리자는 승인되지 않은 회원을 포함한 전체 클럽 회원을 조회할 수 있다.")
    public void findMyClubMemberByAdmin() throws Exception {
        //given
        ClubMember clubMember = createConfirmedClubMember(club, member, "member");
        clubMember.setAdmin();
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = prepareClubMembersIncludeUnConfirmedMember("unConfirmedMember");
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]

        given(clubMemberRepository.findAllByClub(club)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubId, memberId);

        //then
        assertThat(findClubMembers).as("결과가 존재해야 함").isNotNull();

        List<String> findMemberNames = findClubMembers.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(findMemberNames).as("결과에 승인되지 않은 회원이 포함되어야 함")
                .contains("unConfirmedMember");
    }

    @Test
    @DisplayName("매니저에게 클럽 회원 관리 권한이 있으면 승인되지 않은 회원을 포함한 전체 회원을 조회할 수 있다.")
    public void findMyClubMemberByManagerHasMEMBER_ALL() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        ClubMember clubMember = createConfirmedClubMember(club, member, "member");
        clubMember.setManager();

        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = prepareClubMembersIncludeUnConfirmedMember("unConfirmedMember");
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]

        given(clubMemberRepository.findAllByClub(club)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubId, memberId);

        //then
        assertThat(findClubMembers).as("결과가 존재해야 함").isNotNull();

        List<String> findMemberNames = findClubMembers.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(findMemberNames).as("결과에 승인되지 않은 회원이 포함되어야 함")
                .contains("unConfirmedMember");
    }
    
    @Test
    @DisplayName("다른 클럽의 회원을 조회하려 하면 NoSuchElementException")
    public void findOtherClubMemberThenException() throws Exception {
        //given
        Long otherClubId = 2L;
        given(clubMemberRepository.findByClubIdAndMemberId(otherClubId, memberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findMyClubMembers(otherClubId, memberId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("관리자는 일반 회원을 매니저로 설정할 수 있다.")
    public void makeUserToManagerByAdmin() throws Exception {
        //given
        ClubMember admin = createConfirmedClubMember(club, member, "admin");
        admin.setAdmin();

        ClubMember clubMember = createClubMember(club, createMember(), "clubMember");

        //when

        //then
    }

    /**
     * Member util method
     */
    private static Member createMember() {
        return Member.builder().build();
    }

    /**
     * Club util method
     */
    private static Club createClub(String clubName) {
        return Club.builder()
                .clubName(clubName)
                .build();
    }


    /**
     * ClubMember util method
     */
    private static List<ClubMember> createConfirmedClubMembers(Club club, int n) {
        List<ClubMember> clubMembers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Member anotherMember = createMember();
            ClubMember anotherClubMember = createConfirmedClubMember(club, anotherMember, "clubMember" + i);
            clubMembers.add(anotherClubMember);
        }
        return clubMembers;
    }

    private static ClubMember createConfirmedClubMember(Club club, Member member, String name) {
        ClubMember clubMember = createClubMember(club, member, name);
        clubMember.confirm();
        return clubMember;
    }

    private static ClubMember createClubMember(Club club, Member member, String name) {
        return ClubMember.builder()
                .club(club)
                .member(member)
                .name(name)
                .build();
    }

    private List<ClubMember> prepareClubMembersIncludeUnConfirmedMember(String unConfirmedMemberName) {
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3);

        ClubMember unConfirmedMember = createClubMember(club, createMember(), unConfirmedMemberName);
        clubMembers.add(unConfirmedMember);

        return clubMembers;
    }
}
