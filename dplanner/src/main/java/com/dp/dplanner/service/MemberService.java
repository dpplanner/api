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
import com.dp.dplanner.service.upload.FileCleanup;
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
    private final FileCleanup fileCleanup;

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

    @Transactional
    public void leave(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(ErrorResult.MEMBER_NOT_FOUND));
        // 탈퇴 시 클럽 회원도 함께 삭제(@SQLDelete)되며 url 이 null 로 지워지므로, 그 전에 프로필 사진 파일 삭제를 예약한다
        fileCleanup.deleteAfterCommit(member.getClubMembers().stream().map(ClubMember::getUrl).toList());
        memberRepository.delete(member);
    }
}
