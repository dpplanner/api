package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;


    public List<ClubMemberDto.Response> findMyClubMembers(Long clubId, Long memberId) throws NoSuchElementException{
        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(NoSuchElementException::new);
        Club club = clubMember.getClub();
        List<ClubMember> clubMembers = clubMemberRepository.findAllByClub(club);

        return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
    }
}
