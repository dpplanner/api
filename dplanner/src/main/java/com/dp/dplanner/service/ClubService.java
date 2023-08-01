package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.dto.ClubDTO;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubService {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;


    public ClubDTO createClub(Long memberId, String clubName, String clubInfo) throws NoSuchElementException {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);

        Club club = Club.builder().clubName(clubName).info(clubInfo).build();
        clubRepository.save(club);

        ClubMember clubMember = joinClubAsAdmin(member, club);
        clubMemberRepository.save(clubMember);

        return ClubDTO.of(club);
    }

    public ClubDTO findClubById(Long clubId) throws NoSuchElementException {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(NoSuchElementException::new);

        return ClubDTO.of(club);
    }

    public List<ClubDTO> findMyClubs(Long memberId) throws NoSuchElementException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);

        List<Club> clubs = member.getClubMembers().stream()
                .map(ClubMember::getClub)
                .toList();

        return clubs.stream().map(ClubDTO::of).toList();
    }

    public ClubDTO updateClubInfo(Long clubId, Long memberId, String updatedClubInfo) throws IllegalStateException{
        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
                .orElseThrow(IllegalStateException::new);

        if (clubMember.getRole() != ClubRole.ADMIN) {
            throw new IllegalStateException();
        }

        Club updatedClub = clubMember.getClub().updateInfo(updatedClubInfo);
        return ClubDTO.of(updatedClub);
    }

    private static ClubMember joinClubAsAdmin(Member member, Club club) {
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
    }
}
