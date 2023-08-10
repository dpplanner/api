package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;
import static com.dp.dplanner.util.InviteCodeGenerator.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubService {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorityRepository clubAuthorityRepository;


    public ClubDto.Response createClub(Long memberId, ClubDto.Create createDto) throws NoSuchElementException {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);

        Club club = createDto.toEntity();
        clubRepository.save(club);

        ClubMember clubMember = joinClubAsAdmin(member, club);
        clubMemberRepository.save(clubMember);

        return ClubDto.Response.of(club);
    }

    public ClubDto.Response findClubById(Long clubId) throws NoSuchElementException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(NoSuchElementException::new);

        return ClubDto.Response.of(club);
    }

    public List<ClubDto.Response> findMyClubs(Long memberId) throws NoSuchElementException {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);

        List<Club> clubs = member.getClubMembers().stream()
                .map(ClubMember::getClub)
                .toList();

        return ClubDto.Response.ofList(clubs);
    }

    public ClubDto.Response updateClubInfo(Long clubMemberId, ClubDto.Update updateDto)
            throws NoSuchElementException, IllegalStateException{

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isSameClub(updateDto.getClubId())) {
            throw new IllegalStateException();
        }

        if (clubMember.checkRoleIsNot(ADMIN)) {
            throw new IllegalStateException();
        }

        Club updatedClub = clubMember.getClub().updateInfo(updateDto.getInfo());
        return ClubDto.Response.of(updatedClub);
    }

    public void setManagerAuthority(Long clubMemberId, ClubAuthorityDto.Update updateDto)
            throws IllegalStateException, NoSuchElementException {

        List<ClubAuthorityType> authorities = updateDto.toClubAuthorityTypeList();

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isSameClub(updateDto.getClubId())) {
            throw new IllegalStateException();
        }

        if (clubMember.checkRoleIsNot(ADMIN)) {
            throw new IllegalStateException();
        }

        clubAuthorityRepository.deleteAllByClub(clubMember.getClub());

        List<ClubAuthority> clubAuthorities = ClubAuthority.createAuthorities(clubMember.getClub(), authorities);
        clubAuthorityRepository.saveAll(clubAuthorities);
    }

    public ClubAuthorityDto.Response findClubManagerAuthorities(Long clubMemberId, ClubAuthorityDto.Request requestDto)
            throws IllegalStateException, NoSuchElementException {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isSameClub(requestDto.getClubId())) {
            throw new IllegalStateException();
        }

        if (clubMember.checkRoleIs(USER)) {
            throw new IllegalStateException();
        }

        return ClubAuthorityDto.Response.of(clubMember.getClub());
    }

    @RequiredAuthority(MEMBER_ALL)
    public InviteDto inviteClub(Long managerId)
            throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        Club club = manager.getClub();
        String seed = club.getClubName();

        String inviteCode = generateInviteCode(seed);

        return new InviteDto(club.getId(), inviteCode);
    }


    public ClubMemberDto.Response joinClub(Long memberId, InviteDto inviteDto)
            throws IllegalStateException, NoSuchElementException {

        Club club = clubRepository.findById(inviteDto.getClubId())
                .orElseThrow(NoSuchElementException::new);
        String seed = club.getClubName();

        if (verify(seed, inviteDto.getInviteCode())) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(NoSuchElementException::new);

            ClubMember clubMember = createClubMember(member, club);
            clubMemberRepository.save(clubMember);

            return ClubMemberDto.Response.of(clubMember);
        } else {
            throw new IllegalStateException();
        }
    }



    /**
     * utility methods
     */
    private static ClubMember joinClubAsAdmin(Member member, Club club) {
        ClubMember clubMember = createClubMember(member, club);
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }

    private static ClubMember createClubMember(Member member, Club club) {
        return ClubMember.builder().club(club).member(member).build();
    }

}
