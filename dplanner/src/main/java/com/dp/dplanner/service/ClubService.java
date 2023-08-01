package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubDTO;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubService {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    public ClubDTO createClub(Long memberId, String clubName, String clubInfo) throws NoSuchElementException {

        Member member = memberRepository.findById(memberId).orElseThrow(NoSuchElementException::new);

        Club club = Club.builder().clubName(clubName).info(clubInfo).build();
        clubRepository.save(club);

        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        clubMember.setAdmin();
        clubMember.confirm();
        clubMemberRepository.save(clubMember);

        return ClubDTO.of(club);
    }
}
