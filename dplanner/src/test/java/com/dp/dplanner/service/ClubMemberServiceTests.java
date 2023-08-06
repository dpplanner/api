package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
public class ClubMemberServiceTests {

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

    @Test
    @DisplayName("사용자는 같은 클럽에 속한 회원들의 정보를 조회할 수 있다.")
    public void findMyClubMembers() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3);
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
    @DisplayName("다른 클럽의 회원을 조회하려 하면 NoSuchElementException")
    @Disabled("clubMemberId 사용으로 전환하면서 이런 경우가 생기지 않음")
    public void findOtherClubMemberThenException() throws Exception {
//        //given
//        Long otherClubId = 2L;
//        given(clubMemberRepository.findByClubIdAndMemberId(otherClubId, memberId)).willReturn(Optional.ofNullable(null));
//
//        //when
//        //then
//        assertThatThrownBy(() -> clubMemberService.findMyClubMembers(otherClubId, memberId))
//                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("클럽 회원 데이터가 없으면 NoSuchElementException -- 정상적으로는 불가능한 케이스")
    public void findClubMemberThenException() throws Exception {
        //given
        Long wrongClubMemberId = 2L;
        given(clubMemberRepository.findById(wrongClubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubMemberService.findMyClubMembers(wrongClubMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }

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

    @Test
    @DisplayName("사용자는 클럽 회원의 정보를 조회할 수 있다.")
    public void findClubMemberById() throws Exception {
        //given
        Long clubMemberId = 1L;
        ClubMember clubMember = createConfirmedClubMember(club, "member");
        ReflectionTestUtils.setField(clubMember, "id", clubMemberId);
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubMemberDto.Response responseDto = clubMemberService.findById(clubMemberId);

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
        assertThatThrownBy(() -> clubMemberService.findById(notConfirmedClubMemberId))
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
        assertThatThrownBy(() -> clubMemberService.findById(wrongClubMemberId))
                .isInstanceOf(NoSuchElementException.class);
    }

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
        ClubMember deletedClubMember = captureClubMemberFromMockRepository();
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
        ClubMember deletedAdmin = captureClubMemberFromMockRepository();
        assertThat(deletedAdmin).as("퇴출된 관리자는 실제 관리자와 일치해야 한다").isEqualTo(anotherAdmin);

        //when
        ClubMemberDto.Delete deleteDto2 = new ClubMemberDto.Delete(managerId);
        clubMemberService.kickOut(adminId, deleteDto2);
        //then
        ClubMember deletedManager = captureClubMemberFromMockRepository();
        assertThat(deletedManager).as("퇴출된 매니저는 실제 매니저와 일치해야 한다").isEqualTo(manager);

        //when
        ClubMemberDto.Delete deleteDto3 = new ClubMemberDto.Delete(clubMemberId);
        clubMemberService.kickOut(adminId, deleteDto3);
        //then
        ClubMember deletedClubMember = captureClubMemberFromMockRepository();
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
        ClubMember deletedManager = captureClubMemberFromMockRepository();
        assertThat(deletedManager).as("퇴출된 매니저는 실제 매니저와 일치해야 한다").isEqualTo(anotherManager);

        //when
        ClubMemberDto.Delete deleteDto2 = new ClubMemberDto.Delete(clubMemberId);
        clubMemberService.kickOut(managerId, deleteDto2);
        //then
        ClubMember deletedClubMember = captureClubMemberFromMockRepository();
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
     * Argument capture method
     */
    private ClubMember captureClubMemberFromMockRepository() {
        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        then(clubMemberRepository).should(atLeastOnce()).delete(captor.capture());
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
    private static List<ClubMember> createConfirmedClubMembers(Club club, int n) {
        List<ClubMember> clubMembers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ClubMember anotherClubMember = createConfirmedClubMember(club, "clubMember" + i);
            clubMembers.add(anotherClubMember);
        }
        return clubMembers;
    }

    private static ClubMember createConfirmedClubMember(Club club, String name) {
        ClubMember clubMember = createClubMember(club, name);
        clubMember.confirm();
        return clubMember;
    }

    private static ClubMember createClubMember(Club club, String name) {
        return ClubMember.builder()
                .club(club)
                .member(createMember())
                .name(name)
                .build();
    }

    private List<ClubMember> prepareClubMembersIncludeUnConfirmedMember(String unConfirmedMemberName) {
        List<ClubMember> clubMembers = createConfirmedClubMembers(club, 3);

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
