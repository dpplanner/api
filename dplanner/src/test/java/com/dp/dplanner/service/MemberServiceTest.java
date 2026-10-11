package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.service.upload.FileCleanup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ClubRepository clubRepository;
    @Mock
    FileCleanup fileCleanup;
    @InjectMocks
    MemberService memberService;

    @Test
    @DisplayName("회원 탈퇴 시 모든 클럽의 프로필 사진 파일 삭제를 예약하고 회원을 삭제한다")
    void leaveThenDeleteProfileImages() {
        Member member = Member.builder().email("a@example.com").name("a").build();
        ClubMember withImage = ClubMember.createClubMember(member, Club.builder().clubName("c1").build());
        withImage.updateProfileUrl("url-1");
        ClubMember.createClubMember(member, Club.builder().clubName("c2").build()); // 사진 없음
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.leave(1L);

        then(fileCleanup).should().deleteAfterCommit(Arrays.asList("url-1", null));
        then(memberRepository).should().delete(member);
    }
}
