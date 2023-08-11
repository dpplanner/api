package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
public class ClubMemberServiceTests {

    @Mock
    MemberRepository memberRepository;
    @Mock
    ClubRepository clubRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;

    @InjectMocks
    ClubMemberService clubMemberService;

    static Long clubId;
    static Club club;

    @BeforeEach
    void setUp() {
        //레포지토리에 미리 저장된 club
        clubId = 1L;
        club = createClub("club");
        ReflectionTestUtils.setField(club, "id", clubId);
    }

    /**
     * create
     */
    @Test
    @DisplayName("사용자는 클럽 가입시 클럽회원으로 등록된다")
    public void createClubMember() throws Exception {
        //given
        Long memberId = 1L;
        Member member = createMember();
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));
        given(clubMemberRepository.save(any(ClubMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        //when
        ClubMemberDto.Create createDto =
                new ClubMemberDto.Create(clubId, "newClubMember", "newClubMemberInfo");
        ClubMemberDto.Response responseDto = clubMemberService.create(memberId, createDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getName()).as("반환된 이름은 요청한 이름과 일치해야 한다")
                .isEqualTo("newClubMember");
        assertThat(responseDto.getInfo()).as("반환된 부가정보는 요청한 부가정보와 일치해야 한다")
                .isEqualTo("newClubMemberInfo");
        assertThat(responseDto.getRole()).as("회원의 역할은 USER여야 한다")
                .isEqualTo(ClubRole.USER.name());

        ClubMember savedClubMember = captureFromMockRepositoryWhenCreate();
        assertThat(savedClubMember).as("클럽회원이 생성되어야 한다").isNotNull();
        assertThat(savedClubMember.getMember()).as("생성된 클럽 회원은 member와 매핑되어야 한다").isEqualTo(member);
        assertThat(savedClubMember.getClub()).as("생성된 클럽 회원은 club과 매핑되어야 한다").isEqualTo(club);
        assertThat(savedClubMember.getRole()).as("생성된 클럽 회원의 역할은 USER여야 한다").isEqualTo(ClubRole.USER);
        assertThat(savedClubMember.isConfirmed()).as("생성된 클럽회원은 승인대기상태여야 한다").isFalse();

    }

    @Test
    @DisplayName("가입하려는 클럽이 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void createClubMemberWithNotClubThenException() throws Exception {
        //given
        Long memberId = 1L;
        Member member = createMember();
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(member));
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Create createDto =
                new ClubMemberDto.Create(clubId, "newClubMember", "newClubMemberInfo");
        assertThatThrownBy(() -> clubMemberService.create(memberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("서비스에 등록되지 않은 회원일 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void createClubMemberWithNotMemberThenException() throws Exception {
        //given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Create createDto =
                new ClubMemberDto.Create(clubId, "newClubMember", "newClubMemberInfo");
        assertThatThrownBy(() -> clubMemberService.create(memberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("이미 클럽에 가입된 회원일 경우 IllegalStateException")
    public void createExistingClubMemberThenException() throws Exception {
        //given
        Long memberId = 1L;
        ClubMember existingClubMember = createClubMember(club, "existingClubMember");
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).
                willReturn(Optional.ofNullable(existingClubMember));

        //when
        //then
        ClubMemberDto.Create createDto =
                new ClubMemberDto.Create(clubId, "newClubMember", "newClubMemberInfo");
        assertThatThrownBy(() -> clubMemberService.create(memberId, createDto))
                .isInstanceOf(IllegalStateException.class);
    }


    /**
     * findById
     */
    @Test
    @DisplayName("사용자는 클럽 회원의 정보를 조회할 수 있다.")
    public void findClubMemberById() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        ReflectionTestUtils.setField(clubMember, "id", clubMemberId);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(clubMemberId);
        ClubMemberDto.Response responseDto = clubMemberService.findById(requestDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 함").isNotNull();
        assertThat(responseDto.getId()).as("조회한 clubMemberId가 일치해야 한다.").isEqualTo(clubMemberId);
        assertThat(responseDto.getName()).as("조회한 이름이 실제 회원의 이름과 일치해야 한다.").isEqualTo("member");
    }

    @Test
    @DisplayName("승인되지 않은 회원의 정보를 조회하려 하면 NoSuchElementException")
    public void findNotConfirmedClubMemberThenException() throws Exception {
        //given
        Long notConfirmedClubMemberId = 1L;
        ClubMember notConfirmedClubMember = createClubMember(club, "notConfirmedClubMember");
        given(clubMemberRepository.findById(notConfirmedClubMemberId)).willReturn(Optional.ofNullable(notConfirmedClubMember));

        assert !notConfirmedClubMember.isConfirmed();

        //when
        //then
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(notConfirmedClubMemberId);
        assertThatThrownBy(() -> clubMemberService.findById(requestDto))
                .isInstanceOf(NoSuchElementException.class);

    }

    @Test
    @DisplayName("클럽 회원 조회시 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void findNotClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 1L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(wrongClubMemberId);
        assertThatThrownBy(() -> clubMemberService.findById(requestDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * findMyClubMembers
     */
    @Test
    @DisplayName("사용자는 같은 클럽에 속한 회원들의 정보를 조회할 수 있다.")
    public void findMyClubMembers() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3, "clubMember");
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]
        given(clubMemberRepository.findAllConfirmedClubMemberByClub(club)).willReturn(clubMembers);

        ClubMember unConfirmedMember = createClubMember(club, "unConfirmedMember");
        ClubMember otherClubMember = createClubMember(createClub("otherClub"), "otherClubMember");

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubMemberId);

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
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsAdmin("member");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = prepareClubMembersIncludeUnConfirmedMember("unConfirmedMember");
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]

        given(clubMemberRepository.findAllByClub(club)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubMemberId);

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

        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsManager("manager");

        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = prepareClubMembersIncludeUnConfirmedMember("unConfirmedMember");
        clubMembers.add(clubMember); //clubMembers = [member, clubMember0, clubMember1, clubMember2]

        given(clubMemberRepository.findAllByClub(club)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Response> findClubMembers = clubMemberService.findMyClubMembers(clubMemberId);

        //then
        assertThat(findClubMembers).as("결과가 존재해야 함").isNotNull();

        List<String> findMemberNames = findClubMembers.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(findMemberNames).as("결과에 승인되지 않은 회원이 포함되어야 함")
                .contains("unConfirmedMember");
    }

    @Test
    @DisplayName("승인되지 않은 회원이 클럽 회원을 조회하려 하면 IllegalStateException")
    public void findMyClubMembersByNotConfirmedMemberThenException() throws Exception {
        //given
        Long notConfirmedId = 1L;
        ClubMember notConfirmed = createClubMember(club, "notConfirmed");
        given(clubMemberRepository.findById(notConfirmedId)).willReturn(Optional.ofNullable(notConfirmed));

        assert !notConfirmed.isConfirmed();

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findMyClubMembers(notConfirmedId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("클럽 회원 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void findNotMyClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 2L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findMyClubMembers(wrongClubMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * findUnconfirmedClubMembers
     */
    @Test
    @DisplayName("관리자는 승인되지 않은 회원들을 리스트로 조회할 수 있다.")
    public void findUnconfirmedClubMembersByAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        List<ClubMember> confirmedClubMembers = createConfirmedClubMembers(club, 3, "clubMember");
        List<ClubMember> unconfirmedClubMembers = createUnconfirmedClubMembers(club, 3, "unconfirmed");

        given(clubMemberRepository.findAllUnconfirmedClubMemberByClub(club))
                .willReturn(unconfirmedClubMembers);

        //when
        List<ClubMemberDto.Response> responseDto = clubMemberService.findUnconfirmedClubMembers(adminId);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();

        List<String> responseNames = responseDto.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(responseNames).as("승인되지 않은 회원들을 포함해야 한다")
                .contains("unconfirmed0", "unconfirmed1", "unconfirmed2");
        assertThat(responseNames).as("승인된 회원들을 포함하지 않아야 한다")
                .doesNotContain("clubMember0", "clubMember1", "clubMember2");
    }

    @Test
    @DisplayName("회원 관리 권한을 가진 매니저는 승인되지 않은 회원을 리스트로 조회할 수 있다.")
    public void findUnconfirmedClubMembersByManagerHasMEMBER_ALL() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<ClubMember> confirmedClubMembers = createConfirmedClubMembers(club, 3, "clubMember");
        List<ClubMember> unconfirmedClubMembers = createUnconfirmedClubMembers(club, 3, "unconfirmed");

        given(clubMemberRepository.findAllUnconfirmedClubMemberByClub(club))
                .willReturn(unconfirmedClubMembers);

        //when
        List<ClubMemberDto.Response> responseDto = clubMemberService.findUnconfirmedClubMembers(managerId);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();

        List<String> responseNames = responseDto.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(responseNames).as("승인되지 않은 회원들을 포함해야 한다")
                .contains("unconfirmed0", "unconfirmed1", "unconfirmed2");
        assertThat(responseNames).as("승인된 회원들을 포함하지 않아야 한다")
                .doesNotContain("clubMember0", "clubMember1", "clubMember2");
    }

    @Test
    @DisplayName("회원 관리 권한이 없는 매니저가 승인되지 않은 회원들을 조회하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void findUnconfirmedClubMembersByManagerNotHasMEMBER_ALLThenException() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        assert !manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);
        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findUnconfirmedClubMembers(managerId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 승인되지 않은 회원들을 조회하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void findUnconfirmedClubMembersByUserThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findUnconfirmedClubMembers(clubMemberId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("승인되지 않은 회원 조회시 본인의 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void findUnconfirmedClubMembersByNotClubMemberThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findUnconfirmedClubMembers(clubMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * update
     */
    @Test
    @DisplayName("사용자는 클럽회원의 이름과 부가정보를 수정할 수 있다.")
    public void updateClubMember() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubMemberDto.Update updateDto = createUpdateDto(clubMemberId, "updatedMember", "updatedInfo");
        ClubMemberDto.Response responseDto = clubMemberService.update(clubMemberId, updateDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getName()).as("이름이 변경되어야 한다").isEqualTo("updatedMember");
        assertThat(responseDto.getInfo()).as("부가정보가 변경되어야 한다").isEqualTo("updatedInfo");
    }

    @Test
    @DisplayName("다른 클럽회원의 정보를 수정하려하면 IllegalStateException")
    public void updateOtherClubMemberThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDto(2L, "updatedMember", "updatedInfo");
        assertThatThrownBy(() -> clubMemberService.update(clubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("승인되지 않은 회원의 정보를 수정하려 하면 IllegalStateException")
    public void updateNotConfirmedClubMemberThenException() throws Exception {
        //given
        Long notConfirmedClubMemberId = 1L;
        ClubMember notConfirmedClubMember = createClubMember(club, "notConfirmedClubMember");
        given(clubMemberRepository.findById(notConfirmedClubMemberId)).willReturn(Optional.ofNullable(notConfirmedClubMember));

        assert !notConfirmedClubMember.isConfirmed();

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDto(notConfirmedClubMemberId, "updatedMember", "updatedInfo");
        assertThatThrownBy(() -> clubMemberService.update(notConfirmedClubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("클럽회원 정보 수정시 클럽 회원의 데이터가 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void updateNotClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 1L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDto(wrongClubMemberId, "updatedMember", "updatedInfo");
        assertThatThrownBy(() -> clubMemberService.update(wrongClubMemberId, updateDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * changeClubMemberRole
     */
    @Test
    @DisplayName("관리자는 일반 회원을 매니저나 관리자로 설정할 수 있다.")
    public void changeUserToManagerAndAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long toBeManagerId = 2L;
        ClubMember toBeManager = createConfirmedClubMember(club, "toBeManager");
        given(clubMemberRepository.findById(toBeManagerId)).willReturn(Optional.ofNullable(toBeManager));

        Long toBeAdminId = 3L;
        ClubMember toBeAdmin = createConfirmedClubMember(club, "toBeAdmin");
        given(clubMemberRepository.findById(toBeAdminId)).willReturn(Optional.ofNullable(toBeAdmin));

        assert toBeManager.checkRoleIs(ClubRole.USER) && toBeAdmin.checkRoleIs(ClubRole.USER);

        //when
        ClubMemberDto.Update updateDto1 = createUpdateDtoToChangeRole(toBeManagerId, ClubRole.MANAGER);
        clubMemberService.changeClubMemberRole(adminId, updateDto1);

        ClubMemberDto.Update updateDto2 = createUpdateDtoToChangeRole(toBeAdminId, ClubRole.ADMIN);
        clubMemberService.changeClubMemberRole(adminId, updateDto2);

        //then
        assertThat(toBeManager.getRole()).as("클럽 회원의 역할은 매니저여야 함").isEqualTo(ClubRole.MANAGER);
        assertThat(toBeAdmin.getRole()).as("클럽 회원의 역할은 관리자여야 함").isEqualTo(ClubRole.ADMIN);
    }

    @Test
    @DisplayName("관리자는 매니저를 일반 회원이나 관리자로 설정할 수 있다.")
    public void changeManagerToUserAndAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long toBeUserId = 2L;
        ClubMember toBeUser = createClubMemberAsManager("toBeUser");
        given(clubMemberRepository.findById(toBeUserId)).willReturn(Optional.ofNullable(toBeUser));

        Long toBeAdminId = 3L;
        ClubMember toBeAdmin = createClubMemberAsManager("toBeAdmin");
        given(clubMemberRepository.findById(toBeAdminId)).willReturn(Optional.ofNullable(toBeAdmin));

        assert toBeUser.checkRoleIs(ClubRole.MANAGER) && toBeAdmin.checkRoleIs(ClubRole.MANAGER);

        //when
        ClubMemberDto.Update updateDto1 = createUpdateDtoToChangeRole(toBeUserId, ClubRole.USER);
        clubMemberService.changeClubMemberRole(adminId, updateDto1);

        ClubMemberDto.Update updateDto2 = createUpdateDtoToChangeRole(toBeAdminId, ClubRole.ADMIN);
        clubMemberService.changeClubMemberRole(adminId, updateDto2);

        //then
        assertThat(toBeUser.getRole()).as("클럽 회원의 역할은 일반회원이어야 함").isEqualTo(ClubRole.USER);
        assertThat(toBeAdmin.getRole()).as("클럽 회원의 역할은 관리자여야 함").isEqualTo(ClubRole.ADMIN);
    }

    @Test
    @DisplayName("관리자는 다른 관리자를 일반 회원이나 매니저로 설정할 수 있다.")
    public void changeAdminToUserAndManager() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long toBeUserId = 2L;
        ClubMember toBeUser = createClubMemberAsAdmin("toBeUser");
        given(clubMemberRepository.findById(toBeUserId)).willReturn(Optional.ofNullable(toBeUser));

        Long toBeManagerId = 3L;
        ClubMember toBeManager = createClubMemberAsAdmin("toBeManager");
        given(clubMemberRepository.findById(toBeManagerId)).willReturn(Optional.ofNullable(toBeManager));

        assert toBeUser.checkRoleIs(ClubRole.ADMIN) && toBeManager.checkRoleIs(ClubRole.ADMIN);

        //when
        ClubMemberDto.Update updateDto1 = createUpdateDtoToChangeRole(toBeUserId, ClubRole.USER);
        clubMemberService.changeClubMemberRole(adminId, updateDto1);

        ClubMemberDto.Update updateDto2 = createUpdateDtoToChangeRole(toBeManagerId, ClubRole.MANAGER);
        clubMemberService.changeClubMemberRole(adminId, updateDto2);

        //then
        assertThat(toBeUser.getRole()).as("클럽 회원의 역할은 일반회원이어야 함").isEqualTo(ClubRole.USER);
        assertThat(toBeManager.getRole()).as("클럽 회원의 역할은 관리자여야 함").isEqualTo(ClubRole.MANAGER);
    }

    @Test
    @DisplayName("매니저가 임의의 클럽 회원의 역할을 변경하려 하면 IllegalStateException")
    public void changeClubMemberRoleByManagerThenException() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        Long anotherClubMemberId = 2L;
        ClubMember anotherClubMember = createConfirmedClubMember(club, "anotherClubMember");

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(anotherClubMemberId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(managerId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 임의의 클럽 회원의 역할을 변경하려 하면 IllegalStateException")
    public void changeClubMemberRoleByUserThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        Long anotherClubMemberId = 2L;
        ClubMember anotherClubMember = createConfirmedClubMember(club, "anotherClubMember");

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(anotherClubMemberId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(clubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("승인되지 않은 회원의 역할을 변경하는 경우 IllegalStateException")
    public void changeNotConfirmedClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long notConfirmedId = 2L;
        ClubMember notConfirmed = createClubMember(club, "notConfirmed");
        given(clubMemberRepository.findById(notConfirmedId)).willReturn(Optional.ofNullable(notConfirmed));

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(notConfirmedId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(adminId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 클럽 회원의 역할을 변경하는 경우 IllegalStateException")
    public void changeOtherClubMemberRoleThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long otherClubId = 2L;
        Club otherClub = createClubWithId(2L, "otherClub");

        Long otherClubMemberId = 2L;
        ClubMember otherClubMember = createConfirmedClubMember(otherClub, "otherClubMember");
        given(clubMemberRepository.findById(otherClubMemberId)).willReturn(Optional.ofNullable(otherClubMember));

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(otherClubMemberId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(adminId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("자신의 역할을 변경하는 경우 IllegalStateException")
    public void changeMyRoleThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(adminId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(adminId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("역할을 바꾸려는 회원의 데이터가 존재하지 않을 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void changeNotClubMemberRole() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long wrongClubId = 2L;
        given(clubMemberRepository.findById(wrongClubId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(wrongClubId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(adminId, updateDto))
                .isInstanceOf(NoSuchElementException.class);

    }

    @Test
    @DisplayName("회원 역할 변경 시 관리자의 데이터가 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void changeClubMemberRoleByAdminWithNoData() throws Exception {
        //given
        Long adminId = 1L;
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(null));

        Long clubMemberId = 2L;

        //when
        //then
        ClubMemberDto.Update updateDto = createUpdateDtoToChangeRole(clubMemberId, ClubRole.MANAGER);
        assertThatThrownBy(() -> clubMemberService.changeClubMemberRole(adminId, updateDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * confirmAll
     */
    @Test
    @DisplayName("관리자는 승인 대기중인 회원들을 승인할 수 있다.")
    public void confirmAllByAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));
        List<ClubMember> unconfirmedClubMembers = createUnconfirmedClubMembers(club, 3, "unconfirmed");
        given(clubMemberRepository.findAllById(unconfirmedClubMemberIds)).willReturn(unconfirmedClubMembers);

        //when
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        clubMemberService.confirmAll(adminId, requestDto);

        //then
        unconfirmedClubMembers.forEach(clubMember ->
                assertThat(clubMember.isConfirmed()).as("클럽 회원이 승인되어야 한다").isTrue()
        );
    }

    @Test
    @DisplayName("회원 관리 권한이 있는 매니저는 승인 대기중인 회원들을 승인할 수 있다.")
    public void confirmAllByManagerHasMEMBER_ALL() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));
        List<ClubMember> unconfirmedClubMembers = createUnconfirmedClubMembers(club, 3, "unconfirmed");
        given(clubMemberRepository.findAllById(unconfirmedClubMemberIds)).willReturn(unconfirmedClubMembers);

        //when
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        clubMemberService.confirmAll(managerId, requestDto);

        //then
        unconfirmedClubMembers.forEach(clubMember ->
                assertThat(clubMember.isConfirmed()).as("클럽 회원이 승인되어야 한다").isTrue()
        );
    }

    @Test
    @DisplayName("회원 관리 권한이 없는 매니저가 승인 대기중인 회원을 승인하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void confirmAllByManagerNotHasMEMBER_ALLThenException() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));

        assert !manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        assertThatThrownBy(() -> clubMemberService.confirmAll(managerId, requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 승인 대기중인 회원을 승인하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void confirmAllByUserThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));

        //when
        //then
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        assertThatThrownBy(() -> clubMemberService.confirmAll(clubMemberId, requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 클럽의 회원을 승인하려하면 IllegalStateException -- 정상적으로는 불가능한 케이스")
    public void confirmAllByOtherClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));
        List<ClubMember> unconfirmedClubMembers = createUnconfirmedClubMembers(createClub("otherClub"), 3, "unconfirmed");
        given(clubMemberRepository.findAllById(unconfirmedClubMemberIds)).willReturn(unconfirmedClubMembers);

        //when
        //then
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        assertThatThrownBy(() -> clubMemberService.confirmAll(adminId, requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("회원 승인시 본인의 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void confirmAllByNotMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(null));

        List<Long> unconfirmedClubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));

        //when
        //then
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(unconfirmedClubMemberIds);
        assertThatThrownBy(() -> clubMemberService.confirmAll(adminId, requestDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * leaveClub
     */
    @Test
    @DisplayName("사용자가 클럽에서 탈퇴할 때 클럽회원 데이터가 삭제된다.")
    public void leaveClub() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        clubMemberService.leaveClub(clubMemberId);

        //then
        ClubMember deletedClubMember = captureFromMockRepositoryWhenDelete();
        assertThat(deletedClubMember).as("삭제된 회원은 실제 회원과 일치해야 한다.").isEqualTo(clubMember);
    }

    @Test
    @DisplayName("관리자가 스스로 클럽에서 탈퇴하려 하면 IllegalStateException")
    public void leaveClubByAdminThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.leaveClub(adminId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("클럽 탈퇴시 클럽 회원 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void leaveClubByNotClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 1L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.leaveClub(wrongClubMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * kickOut
     */
    @Test
    @DisplayName("관리자는 임의의 클럽 회원을 퇴출할 수 있다.")
    public void kickOutByAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long anotherAdminId = 2L;
        ClubMember anotherAdmin = createClubMemberAsAdmin("anotherAdmin");
        given(clubMemberRepository.findById(anotherAdminId)).willReturn(Optional.ofNullable(anotherAdmin));

        Long managerId = 3L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        Long clubMemberId = 4L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        assert anotherAdmin.checkRoleIs(ClubRole.ADMIN) &&
                manager.checkRoleIs(ClubRole.MANAGER) &&
                clubMember.checkRoleIs(ClubRole.USER);


        //when
        ClubMemberDto.Delete deleteDto1 = new ClubMemberDto.Delete(anotherAdminId);
        clubMemberService.kickOut(adminId, deleteDto1);
        //then
        ClubMember deletedAdmin = captureFromMockRepositoryWhenDelete();
        assertThat(deletedAdmin).as("퇴출된 관리자는 실제 관리자와 일치해야 한다").isEqualTo(anotherAdmin);

        //when
        ClubMemberDto.Delete deleteDto2 = new ClubMemberDto.Delete(managerId);
        clubMemberService.kickOut(adminId, deleteDto2);
        //then
        ClubMember deletedManager = captureFromMockRepositoryWhenDelete();
        assertThat(deletedManager).as("퇴출된 매니저는 실제 매니저와 일치해야 한다").isEqualTo(manager);

        //when
        ClubMemberDto.Delete deleteDto3 = new ClubMemberDto.Delete(clubMemberId);
        clubMemberService.kickOut(adminId, deleteDto3);
        //then
        ClubMember deletedClubMember = captureFromMockRepositoryWhenDelete();
        assertThat(deletedClubMember).as("퇴출된 회원은 실제 회원과 일치해야 한다").isEqualTo(clubMember);
    }

    @Test
    @DisplayName("회원 관리 권한이 있는 매니저는 매니저와 일반 회원을 퇴출할 수 있다.")
    public void kickOutByManagerHasMEMBER_ALL() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        Long anotherManagerId = 2L;
        ClubMember anotherManager = createClubMemberAsManager("anotherManager");
        given(clubMemberRepository.findById(anotherManagerId)).willReturn(Optional.ofNullable(anotherManager));

        Long clubMemberId = 3L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        assert anotherManager.checkRoleIs(ClubRole.MANAGER) &&
                clubMember.checkRoleIs(ClubRole.USER);


        //when
        ClubMemberDto.Delete deleteDto1 = new ClubMemberDto.Delete(anotherManagerId);
        clubMemberService.kickOut(managerId, deleteDto1);
        //then
        ClubMember deletedManager = captureFromMockRepositoryWhenDelete();
        assertThat(deletedManager).as("퇴출된 매니저는 실제 매니저와 일치해야 한다").isEqualTo(anotherManager);

        //when
        ClubMemberDto.Delete deleteDto2 = new ClubMemberDto.Delete(clubMemberId);
        clubMemberService.kickOut(managerId, deleteDto2);
        //then
        ClubMember deletedClubMember = captureFromMockRepositoryWhenDelete();
        assertThat(deletedClubMember).as("퇴출된 회원은 실제 회원과 일치해야 한다").isEqualTo(clubMember);
    }

    @Test
    @DisplayName("회원 관리 권한이 있는 매니저가 관리자를 퇴출하려 하면 IllegalStateException")
    public void kickOutAdminByManagerHasMEMBER_ALLThenException() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        Long adminId = 2L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        assert admin.checkRoleIs(ClubRole.ADMIN);

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(adminId);
        assertThatThrownBy(() -> clubMemberService.kickOut(managerId, deleteDto))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    @DisplayName("회원 관리 권한이 없는 매니저가 클럽 회원을 퇴출하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void kickOutByManagerNotHasMEMBER_ALLThenException() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        Long clubMemberId = 2L;
        ClubMember clubMember = createClubMember(club, "clubMember");

        assert !club.hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(clubMemberId);
        assertThatThrownBy(() -> clubMemberService.kickOut(managerId, deleteDto))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    @DisplayName("일반 회원이 다른 회원을 퇴출하려 하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void kickOutByUserThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        Long anotherClubMemberId = 2L;
        ClubMember anotherClubMember = createClubMember(club, "anotherClubMember");

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(anotherClubMemberId);
        assertThatThrownBy(() -> clubMemberService.kickOut(clubMemberId, deleteDto))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    @DisplayName("다른 클럽의 회원을 퇴출하려 하면 IllegalStateException")
    public void kickOutOtherClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long otherClubMemberId = 2L;
        ClubMember otherClubMember = createClubMember(createClub("otherClub"), "otherClubMember");
        given(clubMemberRepository.findById(otherClubMemberId)).willReturn(Optional.ofNullable(otherClubMember));

        assert !otherClubMember.getClub().equals(admin.getClub());

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(otherClubMemberId);
        assertThatThrownBy(() -> clubMemberService.kickOut(adminId, deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("클럽 회원 퇴출 시 관리자/매니저의 데이터가 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void kickOutNotAdminThenException() throws Exception {
        //given
        Long adminId = 1L;
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(null));

        Long clubMemberId = 2L;

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(clubMemberId);
        assertThatThrownBy(() -> clubMemberService.kickOut(adminId, deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("클럽 회원 퇴출 시 퇴출되는 회원의 데이터가 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void kickOutNotClubMemberThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long clubMemberId = 2L;
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(clubMemberId);
        assertThatThrownBy(() -> clubMemberService.kickOut(adminId, deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("본인을 퇴출시키려는 경우 IllegalStateException")
    public void kickOutSelfThenException() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        //when
        //then
        ClubMemberDto.Delete deleteDto = new ClubMemberDto.Delete(adminId);
        assertThatThrownBy(() -> clubMemberService.kickOut(adminId, deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }


    /**
     * kickOutAll
     */
    @Test
    @DisplayName("관리자는 여러명의 회원을 한번에 퇴출할 수 있다.")
    public void kickOutAllByAdmin() throws Exception {
        //given
        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        List<Long> clubMemberIds = List.of(2L, 3L, 4L);
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3, "clubMember");
        given(clubMemberRepository.findAllById(clubMemberIds)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(clubMemberIds);
        List<ClubMemberDto.Response> responseDtoList = clubMemberService.kickOutAll(adminId, deleteDto);

        //then
        assertThat(responseDtoList).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDtoList.isEmpty()).as("모든 회원이 삭제되면 빈 리스트를 반환한다").isTrue();

        List<ClubMember> deletedClubMembers = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedClubMembers).as("삭제된 회원들은 실제 회원과 일치해야 한다")
                .containsAll(clubMembers);
    }

    @Test
    @DisplayName("회원 관리 권한이 있는 매니저는 여러 명의 회원을 한번에 퇴출할 수 있다.")
    public void kickOutAllByManagerHasMEMBER_ALL() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<Long> clubMemberIds = List.of(2L, 3L, 4L);
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3, "clubMember");
        given(clubMemberRepository.findAllById(clubMemberIds)).willReturn(clubMembers);

        //when
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(clubMemberIds);
        List<ClubMemberDto.Response> responseDtoList = clubMemberService.kickOutAll(managerId, deleteDto);

        //then
        assertThat(responseDtoList).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDtoList.isEmpty()).as("모든 회원이 삭제되면 빈 리스트를 반환한다").isTrue();

        List<ClubMember> deletedClubMembers = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedClubMembers).as("삭제된 회원들은 실제 회원과 일치해야 한다")
                .containsAll(clubMembers);
    }

    @Test
    @DisplayName("회원 관리 권한이 없는 매니저가 다른 회원들을 퇴출하려하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void kickOutAllByMangerNotHasMEMBER_ALLThenException() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<Long> clubMemberIds = List.of(2L, 3L, 4L);

        assert !club.hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(clubMemberIds);
        assertThatThrownBy(() -> clubMemberService.kickOutAll(managerId, deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 다른 회원들을 퇴출하려하면 IllegalStateException")
    @Disabled("RequiredAuthority 어노테이션 사용으로 인해 스프링 통합테스트로 이전")
    public void kickOutAllByUserThenException() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createClubMemberAsManager("clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<Long> clubMemberIds = List.of(2L, 3L, 4L);

        //when
        //then
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(clubMemberIds);
        assertThatThrownBy(() -> clubMemberService.kickOutAll(clubMemberId, deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("퇴출하려는 회원 중 퇴출이 불가능한 회원이 존재하면 이들을 제외한 회원들을 모두 퇴출한다.")
    public void kickOutAllByAdminIncludeSelf() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        List<Long> clubMemberIds = new ArrayList<>(List.of(2L, 3L, 4L));
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3, "clubMember");

        Long adminId = 5L;
        ClubMember admin = createClubMemberAsAdmin("admin");

        Long otherClubMemberId = 6L;
        ClubMember otherClubMember = createClubMember(createClub("otherClub"), "otherClubMember");

        List<Long> savedClubMemberIds = new ArrayList<>(clubMemberIds);
        savedClubMemberIds.addAll(List.of(managerId, adminId, otherClubMemberId));
        List<ClubMember> savedClubMembers = new ArrayList<>(clubMembers);
        savedClubMembers.addAll(List.of(manager, admin, otherClubMember));

        given(clubMemberRepository.findAllById(savedClubMemberIds)).willReturn(savedClubMembers);

        assert club.hasAuthority(ClubAuthorityType.MEMBER_ALL);


        //when
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(savedClubMemberIds);
        List<ClubMemberDto.Response> responseDtoList = clubMemberService.kickOutAll(managerId, deleteDto);

        //then
        assertThat(responseDtoList).as("결과가 존재해야 한다").isNotNull();

        List<String> responseNames = responseDtoList.stream().map(ClubMemberDto.Response::getName).toList();
        assertThat(responseNames).as("삭제되지 않은 회원에는 본인, 관리자, 다른 클럽의 회원이 포함되어야 한다")
                .containsExactlyInAnyOrder("manager", "admin", "otherClubMember");

        List<ClubMember> deletedClubMembers = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedClubMembers).as("삭제된 회원들은 실제 회원과 일치해야 한다")
                .containsAll(clubMembers);
        assertThat(deletedClubMembers).as("본인, 관리자, 다른 클럽의 회원은 삭제되지 않아야 한다")
                .doesNotContain(manager, admin, otherClubMember);
    }

    @Test
    @DisplayName("여러명의 클럽 회원 퇴출 시 관리자/매니저의 데이터가 없는 경우 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void kickOutAllNotAdminThenException() throws Exception {
        //given
        Long adminId = 1L;
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(null));

        Long clubMemberId = 2L;

        //when
        //then
        List<ClubMemberDto.Delete> deleteDto = ClubMemberDto.Delete.ofList(List.of(clubMemberId));
        assertThatThrownBy(() -> clubMemberService.kickOutAll(adminId, deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * hasAuthority
     */
    @Test
    @DisplayName("관리자나 매니저가 해당 권한이 있으면 True")
    public void hasAuthorityThenTrue() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        Long adminId = 1L;
        ClubMember admin = createClubMemberAsAdmin("admin");
        given(clubMemberRepository.findById(adminId)).willReturn(Optional.ofNullable(admin));

        Long managerId = 2L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        //when
        boolean authorizedAdmin = clubMemberService.hasAuthority(adminId, ClubAuthorityType.MEMBER_ALL);
        boolean authorizedManager = clubMemberService.hasAuthority(managerId, ClubAuthorityType.MEMBER_ALL);

        //then
        assertThat(authorizedAdmin).as("관리자는 모든 권한이 있어야 한다").isTrue();
        assertThat(authorizedManager).as("매니저는 클럽에 설정된 권한이 있어야 한다").isTrue();
    }

    @Test
    @DisplayName("매니저가 해당 권한이 없거나 일반회원이면 False")
    public void hasNotAuthorityThenFalse() throws Exception {
        //given
        Long managerId = 1L;
        ClubMember manager = createClubMemberAsManager("manager");
        given(clubMemberRepository.findById(managerId)).willReturn(Optional.ofNullable(manager));

        assert !manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);

        Long clubMemberId = 2L;
        ClubMember clubMember = createConfirmedClubMember(club, "clubMember");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        boolean unauthorizedManager = clubMemberService.hasAuthority(managerId, ClubAuthorityType.MEMBER_ALL);
        boolean unauthorizedClubMember = clubMemberService.hasAuthority(clubMemberId, ClubAuthorityType.MEMBER_ALL);

        //then
        assertThat(unauthorizedManager).as("권한이 부여되지 않은 매니저는 접근 권한이 없어야 한다").isFalse();
        assertThat(unauthorizedClubMember).as("일반 회원은 접근 권한이 없어야 한다").isFalse();
    }

    @Test
    @DisplayName("클럽 회원 데이터가 없는 경우 NoSuchElementException")
    public void hasAuthorityWithNotClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 1L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.hasAuthority(wrongClubMemberId, ClubAuthorityType.MEMBER_ALL))
                .isInstanceOf(NoSuchElementException.class);
    }



    /**
     * Argument capture method
     */
    private ClubMember captureFromMockRepositoryWhenCreate() {
        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        then(clubMemberRepository).should(atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private ClubMember captureFromMockRepositoryWhenDelete() {
        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        then(clubMemberRepository).should(atLeastOnce()).delete(captor.capture());
        return captor.getValue();
    }

    private List<ClubMember> captureFromMockRepositoryWhenDeleteAll() {
        ArgumentCaptor<List<ClubMember>> captor = ArgumentCaptor.forClass(List.class);
        then(clubMemberRepository).should(atLeastOnce()).deleteAll(captor.capture());
        return captor.getValue();
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

    private static Club createClubWithId(Long clubId, String clubName) {
        Club newClub = createClub(clubName);
        ReflectionTestUtils.setField(newClub, "id", clubId);

        return newClub;
    }


    /**
     * ClubMember util method
     */
    private static List<ClubMember> createConfirmedClubMembers(Club club, int n, String name) {
        List<ClubMember> clubMembers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ClubMember anotherClubMember = createConfirmedClubMember(club, name + i);
            clubMembers.add(anotherClubMember);
        }
        return clubMembers;
    }

    private static ClubMember createConfirmedClubMember(Club club, String name) {
        ClubMember clubMember = createClubMember(club, name);
        clubMember.confirm();
        return clubMember;
    }

    private static List<ClubMember> createUnconfirmedClubMembers(Club club, int n, String name) {
        List<ClubMember> unconfirmedClubMembers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ClubMember clubMember = createClubMember(club, name + i);
            unconfirmedClubMembers.add(clubMember);
        }
        return unconfirmedClubMembers;
    }

    private static ClubMember createClubMember(Club club, String name) {
        return ClubMember.builder()
                .club(club)
                .member(createMember())
                .name(name)
                .build();
    }

    private List<ClubMember> prepareClubMembersIncludeUnConfirmedMember(String unConfirmedMemberName) {
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3, "clubMember");

        ClubMember unConfirmedMember = createClubMember(club, unConfirmedMemberName);
        clubMembers.add(unConfirmedMember);

        return clubMembers;
    }

    private static ClubMember createClubMemberAsAdmin(String name) {
        ClubMember admin = createConfirmedClubMember(club, name);
        admin.setAdmin();
        return admin;
    }

    private static ClubMember createClubMemberAsManager(String name) {
        ClubMember manager = createConfirmedClubMember(club, name);
        manager.setManager();
        return manager;
    }


    /**
     * Dto util method
     */
    private static ClubMemberDto.Update createUpdateDtoToChangeRole(Long clubMemberId, ClubRole role) {
        return ClubMemberDto.Update.builder()
                .id(clubMemberId)
                .role(role.name())
                .build();
    }
    private static ClubMemberDto.Update createUpdateDto(Long clubMemberId, String name, String info) {
        return ClubMemberDto.Update.builder()
                .id(clubMemberId)
                .name(name)
                .info(info)
                .build();
    }
}
