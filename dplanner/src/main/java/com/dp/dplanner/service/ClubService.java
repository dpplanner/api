package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
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
import java.util.NoSuchElementException;

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


    @Transactional
    public ClubDto.Response createClub(Long memberId, ClubDto.Create createDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        Club club = createDto.toEntity();
        clubRepository.save(club);

        ClubMember clubMember = ClubMember.createAdmin(member, club);
        clubMemberRepository.save(clubMember);

        return ClubDto.Response.of(club);
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
    public void setManagerAuthority(Long clubMemberId, ClubAuthorityDto.Update updateDto) {

        List<ClubAuthorityType> authorities = updateDto.toClubAuthorityTypeList();

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, updateDto.getClubId());

        clubAuthorityRepository.deleteAllByClub(clubMember.getClub());

        List<ClubAuthority> clubAuthorities = ClubAuthority.createAuthorities(clubMember.getClub(), authorities);
        clubAuthorityRepository.saveAll(clubAuthorities);
    }

    public ClubAuthorityDto.Response findClubManagerAuthorities(Long clubMemberId, ClubAuthorityDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkReadable(clubMember, requestDto.getClubId());

        return ClubAuthorityDto.Response.of(clubMember.getClub());
    }

    @RequiredAuthority(MEMBER_ALL)
    public InviteDto inviteClub(Long managerId) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        Club club = manager.getClub();
        String seed = club.getClubName();

        String inviteCode = generateInviteCode(seed);

        return new InviteDto(club.getId(), inviteCode);
    }


    @Transactional
    public ClubMemberDto.Response joinClub(Long memberId, InviteDto inviteDto) {

        Club club = clubRepository.findById(inviteDto.getClubId())
                .orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));
        String seed = club.getClubName();

        if (verify(seed, inviteDto.getInviteCode())) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() ->new MemberException(MEMBER_NOT_FOUND));

            ClubMember clubMember = ClubMember.createClubMember(member, club);
            clubMemberRepository.save(clubMember);

            return ClubMemberDto.Response.of(clubMember);
        } else {
            throw new ClubException(WRONG_INVITE_CODE);
        }
    }


    /**
     * utility methods
     */

    private static void checkUpdatable(ClubMember clubMember, Long clubId) {
        checkSameClub(clubMember, clubId);

        if (clubMember.checkRoleIsNot(ADMIN)) {
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
