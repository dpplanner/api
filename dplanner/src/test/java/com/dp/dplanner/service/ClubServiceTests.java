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
import com.dp.dplanner.service.upload.UploadService;
import com.dp.dplanner.util.InviteCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
    @Mock
    ClubMemberService clubMemberService;
    @Mock
    InviteCodeGenerator inviteCodeGenerator;
    @Mock
    UploadService uploadService;

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
     * findClubDtoByClubId
     */
    @Test
    @DisplayName("사용자는 clubId로 클럽을 조회할 수 있음")
    public void findClubByClubId() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "newClub", "newClubInfo");

        ClubDto.ResponseMapping responseMapping = createResponseMapping(clubId, "newClub");
        given(clubRepository.findClubDtoByClubId(clubId)).willReturn(responseMapping);
        //when
        ClubDto.Response responseDto = clubService.findClubById(clubId);

        //then
        assertThat(responseDto).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(responseDto.getClubName()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽이름)").isEqualTo("newClub");

    }

    @Test
    @DisplayName("clubId로 조회시 클럽이 없으면 CLUB_NOT_FOUND")
    public void findClubByWrongIdThenException() throws Exception {
        //given
        Long wrongId = 2L;
        given(clubRepository.findClubDtoByClubId(wrongId)).willReturn(null);
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
        ClubDto.ResponseMapping responseMapping1 = createResponseMapping(2L, "club2");
        ClubDto.ResponseMapping responseMapping2 = createResponseMapping(3L, "club3");
        given(clubRepository.findMyClubs(memberId)).willReturn(List.of(responseMapping1, responseMapping2));

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
     * createClubAuthority
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
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(clubId,"name","description", ClubAuthorityType.MEMBER_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.createClubAuthority(clubMemberId, createDto));
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
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(clubId,"name","description", ClubAuthorityType.MEMBER_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.createClubAuthority(clubMemberId, createDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없는 경우 UPDATE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(UPDATE_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("관리자는 매니저의 클럽 권한을 생성할 수 있다.")
    public void setManageMemberAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.MEMBER_ALL));

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(clubAuthorityRepository.save(any(ClubAuthority.class))).willAnswer(invocation -> invocation.getArgument(0));

        //when
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(clubId,"name","description", ClubAuthorityType.MEMBER_ALL.name());
        ClubAuthorityDto.Response responseDto = clubService.createClubAuthority(clubMemberId, createDto);

        assertThat(responseDto.getAuthorities()).as("매니저는 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.MEMBER_ALL.name());

        assertThat(responseDto.getClubId()).as("저장된 클럽 권한의 클럽 id과 요청한 클럽 권한의 클럽 id 같아야 한다.")
                .isEqualTo(clubId);

    }
//
//    @Test
//    @DisplayName("관리자는 매니저에게 스케줄 관리 권한을 설정할 수 있다.")
//    public void setManageScheduleAuthorityByAdmin() throws Exception {
//        //given
//        Long clubId = 1L;
//        Club club = createClub(clubId, "club", null);
//
//        Long clubMemberId = 1L;
//        ClubMember clubMember = createClubMemberAsAdmin(club);
//        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
//
//        //when
//        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.SCHEDULE_ALL.name());
//        clubService.setManagerAuthority(clubMemberId, updateDto);
//
//        //then
//        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);
//
//        assertThat(authorityTypes).as("매니저는 스케줄 관리 권한을 가져야 한다.")
//                .containsExactly(ClubAuthorityType.SCHEDULE_ALL);
//    }
//
//    @Test
//    @DisplayName("관리자는 매니저에게 게시판 관리 권한을 설정할 수 있다.")
//    public void setManagePostAuthorityByAdmin() throws Exception {
//        //given
//        Long clubId = 1L;
//        Club club = createClub(clubId, "club", null);
//
//        Long clubMemberId = 1L;
//        ClubMember clubMember = createClubMemberAsAdmin(club);
//        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
//
//        //when
//        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.POST_ALL.name());
//        clubService.setManagerAuthority(clubMemberId, updateDto);
//
//        //then
//        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);
//        assertThat(authorityTypes).as("매니저는 게시판 관리 권한을 가져야 한다.")
//                .containsExactly(ClubAuthorityType.POST_ALL);
//    }
//
//    @Test
//    @DisplayName("관리자는 매니저에게 메세지 관리 권한을 설정할 수 있다.")
//    public void setManageMessageAuthorityByAdmin() throws Exception {
//        //given
//        Long clubId = 1L;
//        Club club = createClub(clubId, "club", null);
//
//        Long clubMemberId = 1L;
//        ClubMember clubMember = createClubMemberAsAdmin(club);
//        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
//
//        //when
//        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MESSAGE_ALL.name());
//        clubService.setManagerAuthority(clubMemberId, updateDto);
//
//        //then
//        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);
//
//        assertThat(authorityTypes).as("매니저는 메세지 관리 권한을 가져야 한다.")
//                .containsExactly(ClubAuthorityType.MESSAGE_ALL);
//    }

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
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(clubId, "name", "description", ClubAuthorityType.SCHEDULE_ALL.name());

        ClubAuthorityDto.Response responseDto = clubService.createClubAuthority(clubMemberId, createDto);

        //then

        assertThat(responseDto.getAuthorities()).as("매니저는 스케줄 권한을 가져야 한다.")
                .containsExactlyInAnyOrder(ClubAuthorityType.SCHEDULE_ALL.name());

        assertThat(responseDto.getAuthorities()).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());

    }

    @Test
    @DisplayName("관리자는 매니저의 권한을 변경할 수 있다.")
    public void setManagerAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin(club);

        Long clubAuthorityId = 1L;
        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.POST_ALL));

        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(clubAuthorityRepository.findById(clubAuthorityId)).willReturn(Optional.ofNullable(clubAuthority));
        

        //when
        ClubAuthorityDto.Update updateDto =
                new ClubAuthorityDto.Update(
                        clubAuthorityId,
                        clubId,
                        "updateName",
                        "updateDescription",
                        ClubAuthorityType.SCHEDULE_ALL.name());

        clubService.updateClubAuthority(clubMemberId, updateDto);

        //then
        assertThat(clubAuthority.getClubAuthorityTypes()).as("매니저는 스케줄 권한을 가져야 한다.")
                .contains(ClubAuthorityType.SCHEDULE_ALL);
        assertThat(clubAuthority.getClubAuthorityTypes()).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL);
        assertThat(clubAuthority.getName()).as("요청한 이름으로 변경되어야 한다.")
                .isEqualTo("updateName");
        assertThat(clubAuthority.getDescription()).as("요청한 설명으로 변경되어야 한다.")
                .isEqualTo("updateDescription");
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
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(2L,"name","description", ClubAuthorityType.SCHEDULE_ALL.name());
        BaseException exception = assertThrows(ClubException.class, () -> clubService.createClubAuthority(memberId, createDto));
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
        ClubAuthorityDto.Create createDto = new ClubAuthorityDto.Create(1L,"name","description", ClubAuthorityType.SCHEDULE_ALL.name());
        BaseException exception = assertThrows(ClubMemberException.class, () -> clubService.createClubAuthority(clubMemberId, createDto));
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

        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));
        ClubAuthority clubAuthority2 = createClubAuthority(club, "name2", "description2", List.of(ClubAuthorityType.POST_ALL, ClubAuthorityType.SCHEDULE_ALL));

        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(clubAuthorityRepository.findAllByClub(club)).willReturn(List.of(clubAuthority, clubAuthority2));

        //when
        List<ClubAuthorityDto.Response> responseList = clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        //then
        assertThat(responseList).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseList.size()).isEqualTo(2);
        assertThat(responseList).extracting(ClubAuthorityDto.Response::getClubId).containsOnly(clubId);
        assertThat(responseList.get(0).getAuthorities()).containsExactlyInAnyOrder(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());
        assertThat(responseList.get(1).getAuthorities()).containsExactlyInAnyOrder(ClubAuthorityType.POST_ALL.name(), ClubAuthorityType.SCHEDULE_ALL.name());
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

        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));
        ClubAuthority clubAuthority2 = createClubAuthority(club, "name2", "description2", List.of(ClubAuthorityType.POST_ALL, ClubAuthorityType.SCHEDULE_ALL));

        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(clubAuthorityRepository.findAllByClub(club)).willReturn(List.of(clubAuthority, clubAuthority2));

        //when
        List<ClubAuthorityDto.Response> responseList = clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        //then
        assertThat(responseList).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseList.size()).isEqualTo(2);
        assertThat(responseList).extracting(ClubAuthorityDto.Response::getClubId).containsOnly(clubId);
        assertThat(responseList.get(0).getAuthorities()).containsExactlyInAnyOrder(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());
        assertThat(responseList.get(1).getAuthorities()).containsExactlyInAnyOrder(ClubAuthorityType.POST_ALL.name(), ClubAuthorityType.SCHEDULE_ALL.name());
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
        given(inviteCodeGenerator.generateInviteCode(club)).willReturn("uuid");
        //when
        InviteDto inviteDto = clubService.inviteClub(adminId, clubId);

        //then
        assertThat(inviteDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(inviteDto.getClubId()).isEqualTo(clubId);
        assertThat(inviteDto.getInviteCode()).as("초대코드가 존재해야 한다").isNotEmpty();
    }

    @Test
    @DisplayName("회원 관리 권한을 가진 매니저는 다른 회원을 초대하기 위한 초대코드를 만들 수 있다.")
    public void inviteClubByMangerHasMEMBER_ALL() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMember(club, member);
        manager.setManager();
        manager.updateClubAuthority(clubAuthority);

        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));
        given(inviteCodeGenerator.generateInviteCode(club)).willReturn("uuid");
        //when
        InviteDto inviteDto = clubService.inviteClub(managerId, clubId);

        //then
        assertThat(inviteDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(inviteDto.getClubId()).isEqualTo(clubId);
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

        assert !manager.hasAuthority(ClubAuthorityType.MEMBER_ALL);
        //when
        //then
        assertThatThrownBy(() -> clubService.inviteClub(managerId, clubId))
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
        assertThatThrownBy(() -> clubService.inviteClub(clubMemberId, clubId))
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
        BaseException exception = assertThrows(ClubMemberException.class, () -> clubService.inviteClub(adminId, any(Long.class)));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("초대코드 생성 시 본인의 데이터와 요청한 클럽이 다르면 DIFFERENT_CLUB_EXCEPTION")
    public void inviteClubByNotSameClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        Long clubId = 1L;
        Long differentClubId = 2L;
        Club club = createClub(clubId, "club", null);
        ClubMember clubMember = createClubMember(club, member);

        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(clubMember));

        //when
        //then
        BaseException exception = assertThrows(ClubException.class, () -> clubService.inviteClub(adminId,  differentClubId));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터와 본인의 데이터가 다르면 DIFFERENT_CLUB_EXCEPTION 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
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

        ClubMemberDto.Response response = ClubMemberDto.Response.of(ClubMember.createClubMember(member, club));
        given(clubMemberService.create(eq(memberId), any(ClubMemberDto.Create.class))).willReturn(response);

        ClubMemberDto.Create createDto = ClubMemberDto.Create.builder()
                .clubId(clubId)
                .name("name")
                .info("info")
                .build();

        //when
        ClubMemberDto.Response responseDto = clubService.joinClub(memberId, createDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
//        assertThat(responseDto.getName()).as("닉네임은 이름으로 초기회된다").isEqualTo(member.getName());
        assertThat(member.getRecentClub()).as("클럽에 가입하면 최근 클럽이 갱신된다").isEqualTo(club);
    }

    @Test
    @DisplayName("가입하려는 클럽이 없으면 CLUB_NOT_FOUND")
    public void joinNotClubThenException() throws Exception {
        //given
        Long clubId = 1L;
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Create createDto = ClubMemberDto.Create.builder()
                .clubId(clubId)
                .name("name")
                .info("info")
                .build();

        BaseException exception = assertThrows(ClubException.class, () -> clubService.joinClub(memberId, createDto));
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

        ClubMemberDto.Create createDto = ClubMemberDto.Create.builder()
                .clubId(clubId)
                .name("name")
                .info("info")
                .build();

        //when
        //then
        BaseException exception = assertThrows(MemberException.class, () -> clubService.joinClub(memberId, createDto));
        assertThat(exception.getErrorResult()).as("회원 정보가 없으면 MEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("클럽 대표 이미지 변경")
    public void updateClubRepresentativeImage() throws Exception
    {
        MockMultipartFile multipartFile = new MockMultipartFile("image", "content".getBytes());

        Long clubId = 1L;
        Club club = createClub(clubId, "club", null);
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));
        given(uploadService.uploadFile(any())).willReturn("updatedUrl");

        ClubDto.Response response = clubService.changeClubRepresentativeImage(1L, clubId, multipartFile);

        assertThat(response.getUrl()).isEqualTo("updatedUrl");
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

    private static ClubAuthority createClubAuthority(Club club, String name, String description, List<ClubAuthorityType> clubAuthorityTypes) {
        return ClubAuthority.builder()
                .club(club)
                .clubAuthorityTypes(clubAuthorityTypes)
                .name(name)
                .description(description)
                .build();

    }

    private ClubMember createClubMemberAsAdmin(Club club) {
        ClubMember clubMember = createClubMember(club, member);
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }

    public ClubDto.ResponseMapping createResponseMapping(Long id,String clubName) {
        return new ClubDto.ResponseMapping() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getClubName() {
                return clubName;
            }

            @Override
            public String getInfo() {
                return null;
            }

            @Override
            public String getUrl() {
                return null;
            }

            @Override
            public Long getMemberCount() {
                return 1L;
            }

            @Override
            public Boolean getIsConfirmed() {
                return true;
            }
        };
    }
}
