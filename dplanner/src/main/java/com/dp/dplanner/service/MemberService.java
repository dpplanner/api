package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.adapter.dto.ClubDto;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.adapter.dto.FCMDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    @Transactional
    public void updateFCMToken(Long memberId, FCMDto.Request requestDto) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(ErrorResult.MEMBER_NOT_FOUND));
        member.updateFCMToken(requestDto.getRefreshFcmToken());
    }

    @Transactional
    public void changeRecentClub(Long memberId, ClubDto.Request request) {
        Optional<ClubMember> clubMemberOptional = clubMemberRepository.findByClubIdAndMemberId(request.getClubId(), memberId);

        if (clubMemberOptional.isEmpty()) {
            throw new ServiceException(ErrorResult.CLUBMEMBER_NOT_FOUND);
        }else {
            Member member = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(ErrorResult.MEMBER_NOT_FOUND));
            Club club = clubRepository.findById(request.getClubId()).orElseThrow(() -> new ServiceException(ErrorResult.CLUB_NOT_FOUND));
            member.updateRecentClub(club);
        }
    }
}
