package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;


    public List<ClubMemberDto.Response> findMyClubMembers(Long clubId, Long memberId) throws NoSuchElementException{

        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(NoSuchElementException::new);
        Club club = clubMember.getClub();

        List<ClubMember> clubMembers;
        if (clubMember.checkRoleIs(ADMIN) || isMemberManager(clubMember)) {
            clubMembers = clubMemberRepository.findAllByClub(club);
        } else {
            clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);
        }

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    private static boolean isMemberManager(ClubMember clubMember) {
        return clubMember.checkRoleIs(MANAGER) &&
                clubMember.getClub().hasAuthority(MEMBER_ALL);
    }
}
