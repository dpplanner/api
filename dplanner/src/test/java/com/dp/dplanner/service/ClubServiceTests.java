package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.exception.*;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.util.InviteCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ClubServiceTests {

    @Mock
    ClubRepository clubRepository;

    @Mock
    ClubMemberRepository clubMemberRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    ClubAuthorityRepository clubAuthorityRepository;

    @InjectMocks
    ClubService clubService;


    static Long memberId;
    static Member member;
    @BeforeEach
    void setUp() {
        //레포지토리에 미리 저장된 member
        memberId = 1L;
        member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);
    }

    /**
     * createClub
     */
    @Test
    @DisplayName("사용자는 클럽을 생성할 수 있음")
    public void createClub() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> invocation.getArgument(0));

        //when
        ClubDto.Create createDto = new ClubDto.Create("newClub", "newClubInfo");
        ClubDto.Response responseDto = clubService.createClub(memberId, createDto);

        //then
        assertThat(responseDto).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(responseDto.getClubName()).as("반환된 DTO의 이름이 요청한 이름과 동일해야 함").isEqualTo("newClub");
        assertThat(responseDto.getInfo()).as("반환된 DTO의 정보가 요청한 정보와 동일해야 함").isEqualTo("newClubInfo");

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
        ClubDto.Create createDto = new ClubDto.Create("newClub", "newClubInfo");
        clubService.createClub(memberId, createDto);

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
    @DisplayName("서비스에 가입되지 않은 회원이 클럽을 생성하려고 하면 MEMBER_NOT_FOUND")
    public void createClubByAbstractMemberThenException() throws Exception {
        //given
        Long abstractMemberId = 2L;
        given(memberRepository.findById(abstractMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubDto.Create createDto = new ClubDto.Create("newClub", "newClubInfo");
        BaseException exception = assertThrows(MemberException.class, () -> clubService.createClub(abstractMemberId, createDto));
        assertThat(exception.getErrorResult()).as("회원 데이터가 없는 경우 MEMBER_NOT_FOUND 예외를 던진다").isEqualTo(MEMBER_NOT_FOUND);
    }


    /**
     * findClubById
     */
    @Test
    @DisplayName("사용자는 clubId로 클럽을 조회할 수 있음")
    public void findClubByClubId() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "newClub", "newClubInfo");
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        //when
        ClubDto.Response responseDto = clubService.findClubById(clubId);
        
        //then
        assertThat(responseDto).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(responseDto.getClubName()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽이름)").isEqualTo("newClub");
        assertThat(responseDto.getInfo()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽정보)").isEqualTo("newClubInfo");

    }

    @Test
    @DisplayName("clubId로 조회시 클럽이 없으면 CLUB_NOT_FOUND")
    public void findClubByWrongIdThenException() throws Exception {
        //given
        Long wrongId = 2L;
        given(clubRepository.findById(wrongId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        BaseException exception = assertThrows(ClubException.class, () -> clubService.findClubById(wrongId));
        assertThat(exception.getErrorResult()).as("클럽이 없는 경우 CLUB_NOT_FOUND 예외를 던진다").isEqualTo(CLUB_NOT_FOUND);
    }


    /**
     * findMyClubs
     */
    @Test
    @DisplayName("사용자는 자신이 속한 클럽들을 조회할 수 있음")
    public void findMyClub() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        List<Club> clubs = preparedClubs(5); // clubs = [club0, club1, club2, club3, club4]
        setMyClubs(clubs, 2, 4);         // myClubs = [club2, club3]

        //when
        List<ClubDto.Response> responseDto = clubService.findMyClubs(memberId);

        //then
        assertThat(responseDto).as("결과가 존재해야 함").isNotNull();

        List<String> clubNames = responseDto.stream().map(ClubDto.Response::getClubName).toList();
        assertThat(clubNames).as("내가 속한 클럽만 포함해야 함").containsExactlyInAnyOrder("club2", "club3");
    }

    @Test
    @DisplayName("자신이 속한 클럽이 없으면 빈 리스트를 반환")
    public void findMyClubThenEmptyList() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        List<Club> clubs = preparedClubs(5); // clubs = [club0, club1, club2, club3, club4]

        //when
        List<ClubDto.Response> responseDtoList = clubService.findMyClubs(memberId);

        //then
        assertThat(responseDtoList).as("결과가 존재해야 함").isNotNull();
        assertThat(responseDtoList.isEmpty()).as("빈 리스트가 반환되어야 함").isTrue();
    }

    @Test
    @DisplayName("가입되지 않은 사용자가 클럽을 조회하면 MEMBER_NOT_FOUND")
    public void findMyClubByAbstractMemberThenException() throws Exception {
        //given
        Long abstractMemberId = 2L;
        given(memberRepository.findById(abstractMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        BaseException exception = assertThrows(MemberException.class, () -> clubService.findMyClubs(abstractMemberId));
        assertThat(exception.getErrorResult()).as("회원이이 없는 경우 MEMBER_NOT_FOUND 예외를 던진다").isEqualTo(MEMBER_NOT_FOUND);

    }


    /**
     * updateClubInfo
     */
    @Test
    @DisplayName("관리자는 자신의 클럽 정보를 수정할 수 있다.")
    public void updateClubInfoByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));
        
        //when
        ClubDto.Update updateDto = new ClubDto.Update(clubId, "updatedClubInfo");
        ClubDto.Response responseDto = clubService.updateClubInfo(memberId, updateDto);
        
        //then
        assertThat(responseDto).as("결과가 존재해야 함").isNotNull();
        assertThat(responseDto.getInfo()).as("클럽 정보가 요청한대로 변경되어야 함").isEqualTo("updatedClubInfo");
    }

    @Test
    @DisplayName("다른 클럽의 정보를 변경하려하면 DIFFERENT_CLUB_EXCEPTION")
    public void updateOtherClubInfoThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));

        //when
        //then
        ClubDto.Update updateDto = new ClubDto.Update(2L, "updatedClubInfo");
        BaseException exception = assertThrows(ClubException.class, () -> clubService.updateClubInfo(memberId, updateDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 정보를 수정하는 경우 DIFFERENT_CLUB_EXCEPTION 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    @DisplayName("일반 회원이 클럽정보를 수정하려 하면 UPDATE_AUTHORIZATION_DENIED")
    public void updateClubInfoByUserThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));

        assert clubMember.checkRoleIs(ClubRole.USER);

        //when
        //then
        ClubDto.Update updateDto = new ClubDto.Update(clubId, "updatedClubInfo");
        BaseException exception = assertThrows(ClubException.class, () -> clubService.updateClubInfo(memberId, updateDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없는 경우 UPDATE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(UPDATE_AUTHORIZATION_DENIED);

    }

    @Test
    @DisplayName("매니저가 클럽정보를 수정하려 하면 UPDATE_AUTHORIZATION_DENIED")
    public void updateClubInfoByManagerThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        clubMember.setManager();
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));

        assert clubMember.checkRoleIs(ClubRole.MANAGER);

        //when
        //then
        ClubDto.Update updateDto = new ClubDto.Update(clubId, "updatedClubInfo");
        BaseException exception = assertThrows(ClubException.class, () -> clubService.updateClubInfo(memberId, updateDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없는 경우 UPDATE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(UPDATE_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("정보 수정 시 클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void updateClubInfoByNotClubMemberThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubDto.Update updateDto = new ClubDto.Update(1L, "updatedClubInfo");
        BaseException exception = assertThrows(ClubMemberException.class, () -> clubService.updateClubInfo(clubMemberId, updateDto));
        assertThat(exception.getErrorResult()).as("클럽회원 데이터가 없는 경우 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * setManagerAuthority
     */
    @Test
    @DisplayName("일반 회원이 매니저의 권한을 설정하려 하면 UPDATE_AUTHORIZATION_DENIED")
    public void setManagerAuthorityByUserThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        assert clubMember.checkRoleIs(ClubRole.USER);

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MEMBER_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.setManagerAuthority(clubMemberId, updateDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없는 경우 UPDATE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(UPDATE_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("매니저가 매니저의 권한을 설정하려 하면 UPDATE_AUTHORIZATION_DENIED")
    public void setManagerAuthorityByManagerThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        clubMember.setManager();
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        assert clubMember.checkRoleIs(ClubRole.MANAGER);

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MEMBER_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.setManagerAuthority(clubMemberId, updateDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없는 경우 UPDATE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(UPDATE_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("관리자는 매니저에게 클럽회원 관리 권한을 설정할 수 있다.")
    public void setManageMemberAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MEMBER_ALL.name());
        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 클럽 회원 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.MEMBER_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저에게 스케줄 관리 권한을 설정할 수 있다.")
    public void setManageScheduleAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.SCHEDULE_ALL.name());
        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 스케줄 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.SCHEDULE_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저에게 게시판 관리 권한을 설정할 수 있다.")
    public void setManagePostAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.POST_ALL.name());
        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);
        assertThat(authorityTypes).as("매니저는 게시판 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.POST_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저에게 메세지 관리 권한을 설정할 수 있다.")
    public void setManageMessageAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MESSAGE_ALL.name());
        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 메세지 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.MESSAGE_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저에게 복수개의 권한을 설정할 수 있다.")
    public void setManagerAuthoritiesByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto =
                new ClubAuthorityDto.Update(
                        clubId,
                        ClubAuthorityType.SCHEDULE_ALL.name(),
                        ClubAuthorityType.MESSAGE_ALL.name());

        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.SCHEDULE_ALL, ClubAuthorityType.MESSAGE_ALL);
        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저의 권한을 변경할 수 있다.")
    public void setManagerAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Update updateDto =
                new ClubAuthorityDto.Update(
                        clubId,
                        ClubAuthorityType.SCHEDULE_ALL.name(),
                        ClubAuthorityType.MESSAGE_ALL.name());
        
        clubService.setManagerAuthority(clubMemberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리 권한을 가져야 한다.")
                .contains(ClubAuthorityType.SCHEDULE_ALL, ClubAuthorityType.MESSAGE_ALL);
        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL);
    }

    @Test
    @DisplayName("다른 클럽의 매니저 권한을 변경하려하면 DIFFERENT_CLUB_EXCEPTION")
    public void setOtherClubManagerAuthorityThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(2L, ClubAuthorityType.SCHEDULE_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.setManagerAuthority(memberId, updateDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 정보를 수정하려 하면 DIFFERENT_CLUB_EXCEPTION 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("권한 설정 시 클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void setClubManagerAuthorityByNotClubMemberThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(1L, ClubAuthorityType.MESSAGE_ALL.name());
        BaseException exception = assertThrows(ClubMemberException.class, () -> clubService.setManagerAuthority(clubMemberId, updateDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * findClubManagerAuthorities
     */
    @Test
    @DisplayName("관리자는 자신이 속한 클럽의 매니저 권한을 확인할 수 있다.")
    public void findManagerAuthoritiesByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Response responseDto =
                clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));
        
        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getAuthorities()).as("반환된 결과의 매니저 권한은 실제 클럽의 매니저 권한과 일치해아 한다.")
                .containsExactlyInAnyOrder(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());
    }

    @Test
    @DisplayName("매니저는 자신이 속한 클럽의 매니저 권한을 확인할 수 있다.")
    public void findManagerAuthoritiesByManager() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        clubMember.setManager();
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Response responseDto =
                clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getAuthorities()).as("반환된 결과의 매니저 권한은 실제 클럽의 매니저 권한과 일치해아 한다.")
                .containsExactlyInAnyOrder(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());
    }

    @Test
    @DisplayName("일반회원이 자신이 속한 클럽의 매니저 권한을 확인하려 하면 READ_AUTHORIZATION_DENIED.")
    public void findManagerAuthoritiesByUserThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        assert clubMember.checkRoleIs(ClubRole.USER);

        //when
        //then
        BaseException exception = assertThrows(ClubException.class,
                () -> clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId)));
        assertThat(exception.getErrorResult()).as("읽기 권한이 없으면 READ_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(READ_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("다른 클럽의 매니저 권한을 조회하려하면 DIFFERENT_CLUB_EXCEPTION")
    public void findOtherClubManagerAuthoritiesThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", "clubInfo");

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.of(clubMember));

        //when
        //then
        BaseException exception = assertThrows(ClubException.class,
                () -> clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(2L)));
        assertThat(exception.getErrorResult()).as("다른 클럽의 정보를 조회하면 DIFFERENT_CLUB_EXCEPTION 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("권한 조회 시 클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND.")
    public void findManagerAuthoritiesByNotClubMemberThenException() throws Exception {
        //given
        Long clubMemberId = 2L;
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        BaseException exception = assertThrows(ClubMemberException.class,
                () -> clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(1L)));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * inviteClub
     */
    @Test
    @DisplayName("관리자는 다른 회원을 초대하기 위한 초대코드를 만들 수 있다.")
    public void inviteClubByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        //when
        InviteDto inviteDto = clubService.inviteClub(adminId);

        //then
        assertThat(inviteDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(inviteDto.getClubId()).as("초대하는 클럽의 id가 일치해야 한다").isEqualTo(clubId);
        assertThat(inviteDto.getInviteCode()).as("초대코드가 존재해야 한다").isNotEmpty();
    }

    @Test
    @DisplayName("회원 관리 권한을 가진 매니저는 다른 회원을 초대하기 위한 초대코드를 만들 수 있다.")
    public void inviteClubByMangerHasMEMBER_ALL() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMember(club, member);
        manager.setManager();
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        //when
        InviteDto inviteDto = clubService.inviteClub(managerId);

        //then
        assertThat(inviteDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(inviteDto.getClubId()).as("초대하는 클럽의 id가 일치해야 한다").isEqualTo(clubId);
        assertThat(inviteDto.getInviteCode()).as("초대코드가 존재해야 한다").isNotEmpty();
    }

    @Test
    @DisplayName("회원 관리 권한이 없는 매니저가 초대코드를 만드려고 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void inviteClubByMangerNotHasMEMBER_ALLThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long managerId = 1L;
        ClubMember manager = createClubMember(club, member);
        manager.setManager();
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        assert !manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);
        //when
        //then
        assertThatThrownBy(() -> clubService.inviteClub(managerId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 초대코드를 만드려고 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void inviteClubByMangerUserThenException() throws Exception {
        //given
        Long clubId = 1L;

        Long clubMemberId = 1L;
        Club club = createClub(clubId, "club", null);
        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        //then
        assertThatThrownBy(() -> clubService.inviteClub(clubMemberId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("초대코드 생성 시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void inviteClubByNotClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        BaseException exception = assertThrows(ClubMemberException.class, () -> clubService.inviteClub(adminId));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * joinClub
     */
    @Test
    @DisplayName("초대코드가 있는 회원은 클럽에 가입할 수 있다.")
    public void joinClub() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));

        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        given(clubMemberRepository.save(any(ClubMember.class))).willAnswer(invocation -> invocation.getArgument(0));

        String inviteCode = InviteCodeGenerator.generateInviteCode("club"); // seed = clubName

        //when
        InviteDto inviteDto = new InviteDto(clubId, inviteCode);
        ClubMemberDto.Response responseDto = clubService.joinClub(memberId, inviteDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();

        ClubMember clubMember = captureClubMemberFromMockRepository();
        assertThat(clubMember).as("클럽 회원으로 등록되어야 한다").isNotNull();
        assertThat(clubMember.getMember()).as("클럽 회원은 member와 매핑되어야 한다").isEqualTo(member);
        assertThat(clubMember.getClub()).as("클럽 회원은 club과 매핑되어야 한다").isEqualTo(club);
        assertThat(clubMember.getRole()).as("클럽 회원은 최초에 USER 역할을 가진다").isEqualTo(ClubRole.USER);
        assertThat(clubMember.isConfirmed()).as("클럽 회원은 승인 대기 상태여야 한다").isFalse();
    }

    @Test
    @DisplayName("초대코드가 일치하지 않으면 WRONG_INVITE_CODE")
    public void joinClubWithInvalidInviteCodeThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        String inviteCode = InviteCodeGenerator.generateInviteCode("invalidSeed");

        //when
        InviteDto inviteDto = new InviteDto(clubId, inviteCode);
        BaseException exception = assertThrows(ClubException.class, () -> clubService.joinClub(memberId, inviteDto));
        assertThat(exception.getErrorResult()).as("초대코드가 유효하지 않으면 WRONG_INVITE_CODE 예외를 던진다")
                .isEqualTo(WRONG_INVITE_CODE);
    }

    @Test
    @DisplayName("가입하려는 클럽이 없으면 CLUB_NOT_FOUND")
    public void joinNotClubThenException() throws Exception {
        //given
        Long clubId = 1L;
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        InviteDto inviteDto = new InviteDto(clubId, "inviteCode");
        BaseException exception = assertThrows(ClubException.class, () -> clubService.joinClub(memberId, inviteDto));
        assertThat(exception.getErrorResult()).as("클럽 정보가 없으면 CLUB_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUB_NOT_FOUND);
    }

    @Test
    @DisplayName("클럽 가입시 회원 데이터가 없으면 MEMBER_NOT_FOUND")
    public void joinClubWithNotMemberThenException() throws Exception {
        //given
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(null));

        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        String inviteCode = InviteCodeGenerator.generateInviteCode("club"); // seed = clubName

        //when
        //then
        InviteDto inviteDto = new InviteDto(clubId, inviteCode);
        BaseException exception = assertThrows(MemberException.class, () -> clubService.joinClub(memberId, inviteDto));
        assertThat(exception.getErrorResult()).as("회원 정보가 없으면 MEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(MEMBER_NOT_FOUND);
    }




    /**
     * Argument capture method
     */
    private Club captureClubFromMockRepository() {
        ArgumentCaptor<Club> captor = ArgumentCaptor.forClass(Club.class);
        then(clubRepository).should(atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private ClubMember captureClubMemberFromMockRepository() {
        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        then(clubMemberRepository).should(atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    /**
     * Club util method
     */
    private static Club createClub(Long clubId, String clubName, String info) {
        Club club = Club.builder()
                .clubName(clubName)
                .info(info)
                .build();
        ReflectionTestUtils.setField(club, "id", clubId);
        
        return club;
    }

    private List<Club> preparedClubs(int size) {
        List<Club> clubs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Club club = createClub((long) i, "club" + i, null);
            clubs.add(club);
        }
        return clubs;
    }

    private void setMyClubs(List<Club> clubs, int from, int to) {
        List<Club> myClubs = clubs.subList(from, to);
        myClubs.forEach(this::createClubMemberAsAdmin);
    }

    private static List<ClubAuthorityType> getClubAuthorityTypes(Club club) {
        return club.getManagerAuthorities().stream()
                .map(ClubAuthority::getClubAuthorityType)
                .toList();
    }


    /**
     * ClubMember util method
     */
    private static ClubMember createClubMember(Club club, Member member) {
        ClubMember clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .build();

        clubMember.confirm();
        return clubMember;
    }

    private ClubMember createClubMemberAsAdmin(Club club) {
        ClubMember clubMember = createClubMember(club, member);
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }
}
