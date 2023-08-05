package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
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

    @Test
    @DisplayName("사용자는 클럽을 생성할 수 있음")
    public void createClubSuccess() throws Exception {
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
    @DisplayName("서비스에 가입되지 않은 회원이 클럽을 생성하려고 하면 NoSuchElementException")
    public void createClubByAbstractMemberThenException() throws Exception {
        //given
        Long abstractMemberId = 2L;
        given(memberRepository.findById(abstractMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubDto.Create createDto = new ClubDto.Create("newClub", "newClubInfo");
        assertThatThrownBy(() -> clubService.createClub(abstractMemberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }
    
    @Test
    @DisplayName("사용자는 clubId로 클럽을 조회할 수 있음")
    public void findClubByClubId() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("newClub", "newClubInfo");
        given(clubRepository.findById(clubId)).willReturn(Optional.ofNullable(club));

        //when
        ClubDto.Response responseDto = clubService.findClubById(clubId);
        
        //then
        assertThat(responseDto).as("반환된 DTO가 존재해야 함").isNotNull();
        assertThat(responseDto.getClubName()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽이름)").isEqualTo("newClub");
        assertThat(responseDto.getInfo()).as("DTO의 클럽정보가 찾는 클럽과 일치해야 함(클럽정보)").isEqualTo("newClubInfo");

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
        Club club = createClub("club", "clubInfo");
        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.of(clubMember));
        
        //when
        ClubDto.Update updateDto = new ClubDto.Update(clubId, "updatedClubInfo");
        ClubDto.Response responseDto = clubService.updateClubInfo(memberId, updateDto);
        
        //then
        assertThat(responseDto).as("결과가 존재해야 함").isNotNull();
        assertThat(responseDto.getInfo()).as("클럽 정보가 요청한대로 변경되어야 함").isEqualTo("updatedClubInfo");
    }

    @Test
    @DisplayName("관리자가 아닌 사용자가 클럽정보를 수정하려 하면 IllegalStateException")
    public void updateClubInfoByNotAdminThenException() throws Exception {
        //given
        Long clubId1 = 1L;
        Club club1 = createClub("club1", "clubInfo1");
        ClubMember clubMember1 = createClubMember(club1, member);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId1, memberId)).willReturn(Optional.of(clubMember1));

        Long clubId2 = 2L;
        Club club2 = createClub("club2", "clubInfo2");
        ClubMember clubMember2 = createClubMember(club2, member);
        clubMember2.setManager();
        given(clubMemberRepository.findByClubIdAndMemberId(clubId2, memberId)).willReturn(Optional.of(clubMember2));

        assert clubMember1.checkRoleIsNot(ClubRole.ADMIN) && clubMember2.checkRoleIsNot(ClubRole.ADMIN);

        //when
        //then
        ClubDto.Update updateDto1 = new ClubDto.Update(clubId1, "updatedClubInfo");
        assertThatThrownBy(() -> clubService.updateClubInfo(memberId, updateDto1))
                .isInstanceOf(IllegalStateException.class);

        ClubDto.Update updateDto2 = new ClubDto.Update(clubId2, "updatedClubInfo");
        assertThatThrownBy(() -> clubService.updateClubInfo(memberId, updateDto2))
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
        ClubDto.Update updateDto = new ClubDto.Update(clubId, "updatedClubInfo");
        assertThatThrownBy(() -> clubService.updateClubInfo(clubId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("관리자가 아닌 회원이 매니저의 권한을 설정하려 하면 IllegalStateException")
    public void setManagerAuthorityByNotAdminThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));
        assert clubMember.checkRoleIsNot(ClubRole.ADMIN);

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MEMBER_ALL.name());
        assertThatThrownBy(() -> clubService.setManagerAuthority(memberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("관리자는 매니저에게 클럽회원 관리 권한을 설정할 수 있다.")
    public void setManageMemberAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MEMBER_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

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
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.SCHEDULE_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

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
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.POST_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

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
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MESSAGE_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

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
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        //when
        ClubAuthorityDto.Update updateDto =
                new ClubAuthorityDto.Update(
                        clubId,
                        ClubAuthorityType.SCHEDULE_ALL.name(),
                        ClubAuthorityType.MESSAGE_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리 권한을 가져야 한다.")
                .containsExactly(ClubAuthorityType.SCHEDULE_ALL, ClubAuthorityType.MESSAGE_ALL);
        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL);
    }

    @Test
    @DisplayName("관리자는 매니저의 권한을 변경할 수 있다.")
    public void updateManagerAuthorityByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Update updateDto =
                new ClubAuthorityDto.Update(
                        clubId,
                        ClubAuthorityType.SCHEDULE_ALL.name(),
                        ClubAuthorityType.MESSAGE_ALL.name());
        clubService.setManagerAuthority(memberId, updateDto);

        //then
        List<ClubAuthorityType> authorityTypes = getClubAuthorityTypes(club);

        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리 권한을 가져야 한다.")
                .contains(ClubAuthorityType.SCHEDULE_ALL, ClubAuthorityType.MESSAGE_ALL);
        assertThat(authorityTypes).as("매니저는 스케줄과 메세지 관리를 제외한 다른 권한은 가지지 않아야 한다.")
                .doesNotContain(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL);
    }

    @Test
    @DisplayName("다른 클럽의 매니저 권한을 설정하려 하면 NoSuchElementException")
    public void setOtherClubManagerAuthorityThenException() throws Exception {
        //given
        Long clubId = 2L;
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ClubAuthorityDto.Update updateDto = new ClubAuthorityDto.Update(clubId, ClubAuthorityType.MESSAGE_ALL.name());
        assertThatThrownBy(() -> clubService.setManagerAuthority(memberId, updateDto))
                .isInstanceOf(NoSuchElementException.class);
    }
    
    @Test
    @DisplayName("관리자는 자신이 속한 클럽의 매니저 권한을 확인할 수 있다.")
    public void findManagerAuthoritiesByAdmin() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMemberAsAdmin(club);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Response responseDto =
                clubService.findClubManagerAuthorities(memberId, new ClubAuthorityDto.Request(clubId));
        
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
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMember(club, member);
        clubMember.setManager();
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL, ClubAuthorityType.POST_ALL));

        //when
        ClubAuthorityDto.Response responseDto =
                clubService.findClubManagerAuthorities(memberId, new ClubAuthorityDto.Request(clubId));

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getAuthorities()).as("반환된 결과의 매니저 권한은 실제 클럽의 매니저 권한과 일치해아 한다.")
                .containsExactlyInAnyOrder(ClubAuthorityType.MEMBER_ALL.name(), ClubAuthorityType.POST_ALL.name());
    }

    @Test
    @DisplayName("일반회원이 자신이 속한 클럽의 매니저 권한을 확인하려 하면 IllegalStateException.")
    public void findManagerAuthoritiesByUserThenException() throws Exception {
        //given
        Long clubId = 1L;
        Club club = createClub("club", null);

        ClubMember clubMember = createClubMember(club, member);
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(clubMember));

        assert clubMember.checkRoleIs(ClubRole.USER);

        //when
        //then
        assertThatThrownBy(() -> clubService.findClubManagerAuthorities(memberId, new ClubAuthorityDto.Request(clubId)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("자신이 속하지 않은 클럽의 매니저 권한을 확인하려 하면 NoSuchElementException.")
    public void findOtherClubManagerAuthoritiesThenException() throws Exception {
        //given
        Long clubId = 2L;
        given(clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        assertThatThrownBy(() -> clubService.findClubManagerAuthorities(memberId, new ClubAuthorityDto.Request(clubId)))
                .isInstanceOf(NoSuchElementException.class);
    }







    /**
     * Argument capture method
     */
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

    /**
     * Club util method
     */
    private static Club createClub(String clubName, String info) {
        return Club.builder()
                .clubName(clubName)
                .info(info)
                .build();
    }

    private List<Club> preparedClubs(int size) {
        List<Club> clubs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Club club = createClub("club" + i, null);
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
