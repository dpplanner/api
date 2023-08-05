package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static com.dp.dplanner.domain.club.ClubRole.*;

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

        if (!isValidRequest(clubMember, updateDto.getClubId())) {
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

        //TODO clubMemberId를 dto에 담아서 받을까?
        List<ClubAuthorityType> authorities = updateDto.toClubAuthorityTypeList();

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!isValidRequest(clubMember, updateDto.getClubId())) {
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

        if (!isValidRequest(clubMember, requestDto.getClubId())) {
            throw new IllegalStateException();
        }

        if (clubMember.checkRoleIs(USER)) {
            throw new IllegalStateException();
        }

        return ClubAuthorityDto.Response.of(clubMember.getClub());
    }


    /**
     * utility methods
     */
    private static ClubMember joinClubAsAdmin(Member member, Club club) {
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }

    private static boolean isValidRequest(ClubMember clubMember, Long requestClubId) {
        return requestClubId.equals(clubMember.getClub().getId());
    }

}
