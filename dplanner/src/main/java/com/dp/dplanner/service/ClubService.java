package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.exception.ClubAuthorityException;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static com.dp.dplanner.util.InviteCodeGenerator.*;

@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClubService {
    private final MemberRepository memberRepository;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorityRepository clubAuthorityRepository;
    private final ClubMemberService clubMemberService;


    @Transactional
    public ClubDto.Response createClub(Long memberId, ClubDto.Create createDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        Club club = createDto.toEntity();
        clubRepository.save(club);

        member.updateRecentClub(club);

        ClubMember clubMember = ClubMember.createAdmin(member, club);
        clubMemberRepository.save(clubMember);

        return ClubDto.Response.of(club);
    }

    public List<ClubDto.Response> findClubs(Map<String, String> param) {

        List<Club> clubs = clubRepository.findAll();
        return ClubDto.Response.ofList(clubs);
    }

    public ClubDto.Response findClubById(Long clubId) {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));

        return ClubDto.Response.of(club);
    }

    public List<ClubDto.Response> findMyClubs(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        List<Club> clubs = member.getClubMembers().stream()
                .map(ClubMember::getClub)
                .toList();

        return ClubDto.Response.ofList(clubs);
    }

    @Transactional
    public ClubDto.Response updateClubInfo(Long clubMemberId, ClubDto.Update updateDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, updateDto.getClubId());

        Club updatedClub = clubMember.getClub().updateInfo(updateDto.getInfo());
        return ClubDto.Response.of(updatedClub);
    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public ClubAuthorityDto.Response createClubAuthority(Long clubMemberId, ClubAuthorityDto.Create createDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, createDto.getClubId());

        ClubAuthority createdClubAuthority = createDto.toEntity(clubMember.getClub());
        clubAuthorityRepository.save(createdClubAuthority);

        return ClubAuthorityDto.Response.of(clubMember.getClub().getId(), createdClubAuthority);


    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public ClubAuthorityDto.Response updateClubAuthority(Long clubMemberId, ClubAuthorityDto.Update updateDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, updateDto.getClubId());

        ClubAuthority authority = clubAuthorityRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ClubAuthorityException(CLUB_AUTHORITY_NOT_FOUND));

        authority.update(updateDto.toClubAuthorityTypeList(), updateDto.getName(), updateDto.getDescription());

        return ClubAuthorityDto.Response.of(clubMember.getClub().getId(), authority);

    }

    @RequiredAuthority(role = ADMIN)
    public List<ClubAuthorityDto.Response> findClubManagerAuthorities(Long clubMemberId, ClubAuthorityDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkReadable(clubMember, requestDto.getClubId());

        List<ClubAuthority> clubAuthorities = clubAuthorityRepository.findAllByClub(clubMember.getClub());

        return ClubAuthorityDto.Response.ofList(clubMember.getClub().getId(),clubAuthorities);
    }

    @RequiredAuthority(authority = MEMBER_ALL)
    public InviteDto inviteClub(Long managerId,Long clubId) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        if (!manager.isSameClub(clubId)) {
            throw new ClubException(DIFFERENT_CLUB_EXCEPTION);
        }

        Club club = manager.getClub();
        String seed = club.getClubName();

        String inviteCode = generateInviteCode(seed);

        return new InviteDto(club.getId(), inviteCode);
    }

    //TODO 클럽 가입 방법 정리(클럽 회원 생성 기능을 clubMemberService에 위임하고 싶은데 name, info등의 정보는 어떻게 처리할지?)

    @Transactional
    public ClubMemberDto.Response joinClub(Long memberId, InviteDto inviteDto) {

        Club club = clubRepository.findById(inviteDto.getClubId())
                .orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));
        String seed = club.getClubName();

        if (verify(seed, inviteDto.getInviteCode())) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() ->new MemberException(MEMBER_NOT_FOUND));

            ClubMemberDto.Create createDto = ClubMemberDto.Create.builder()
                    .clubId(inviteDto.getClubId())
                    .name(member.getName())
                    .build();
            member.updateRecentClub(club);

            return clubMemberService.create(memberId, createDto);
        } else {
            throw new ClubException(WRONG_INVITE_CODE);
        }
    }


    /**
     * utility methods
     */

    private static void checkUpdatable(ClubMember clubMember, Long clubId) {
        checkSameClub(clubMember, clubId);

        if (!clubMember.checkRoleIs(ADMIN)) {
            throw new ClubException(UPDATE_AUTHORIZATION_DENIED);
        }
    }

    private static void checkReadable(ClubMember clubMember, Long clubId) {
        checkSameClub(clubMember, clubId);

        if (clubMember.checkRoleIs(USER)) {
            throw new ClubException(READ_AUTHORIZATION_DENIED);
        }
    }

    private static void checkSameClub(ClubMember clubMember, Long clubId) {
        if (!clubMember.isSameClub(clubId)) {
            throw new ClubException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
}
