package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.exception.ClubAuthorityException;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.club.ClubRole.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClubService {
    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorityRepository clubAuthorityRepository;
    private final ClubMemberService clubMemberService;
    private final InviteCodeGenerator inviteCodeGenerator;


    @Transactional
    public ClubDto.Response createClub(Long memberId, ClubDto.Create createDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        Club club = createDto.toEntity();
        clubRepository.save(club);

        member.updateRecentClub(club);

        ClubMember clubMember = ClubMember.createAdmin(member, club);
        clubMemberRepository.save(clubMember);

        ClubDto.Response response = ClubDto.Response.of(club);

        return response;
    }

    public List<ClubDto.Response> findClubs(Map<String, String> param) {

        List<Club> clubs = clubRepository.findAll();
        return ClubDto.Response.ofList(clubs);
    }

    public ClubDto.Response findClubById(Long clubId) {

        ClubDto.ResponseMapping club = clubRepository.findClubDtoByClubId(clubId);
        if (club == null) {
            throw new ClubException(CLUB_NOT_FOUND);
        }
        return ClubDto.Response.of(club);
    }

    public List<ClubDto.Response> findMyClubs(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        List<ClubDto.ResponseMapping> myClubs = clubRepository.findMyClubs(member.getId());
        return myClubs.stream().map(ClubDto.Response::of).collect(Collectors.toList());

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
    @RequiredAuthority(role = ADMIN)
    public ClubAuthorityDto.Response createClubAuthority(Long clubMemberId, ClubAuthorityDto.Create createDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, createDto.getClubId());

        ClubAuthority createdClubAuthority = createDto.toEntity(clubMember.getClub());
        clubAuthorityRepository.save(createdClubAuthority);

        return ClubAuthorityDto.Response.of(clubMember.getClub().getId(), createdClubAuthority);


    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public ClubAuthorityDto.Response updateClubAuthority(Long clubMemberId, ClubAuthorityDto.Update updateDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkUpdatable(clubMember, updateDto.getClubId());

        ClubAuthority authority = clubAuthorityRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ClubAuthorityException(CLUB_AUTHORITY_NOT_FOUND));

        authority.update(updateDto.toClubAuthorityTypeList(), updateDto.getName(), updateDto.getDescription());

        return ClubAuthorityDto.Response.of(clubMember.getClub().getId(), authority);

    }

    @RequiredAuthority(role = ADMIN)
    public List<ClubAuthorityDto.Response> findClubManagerAuthorities(Long clubMemberId, ClubAuthorityDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        checkReadable(clubMember, requestDto.getClubId());

        List<ClubAuthority> clubAuthorities = clubAuthorityRepository.findAllByClub(clubMember.getClub());

        return ClubAuthorityDto.Response.ofList(clubMember.getClub().getId(),clubAuthorities);
    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public InviteDto inviteClub(Long adminId,Long clubId) {

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        if (!admin.isSameClub(clubId)) {
            throw new ClubException(DIFFERENT_CLUB_EXCEPTION);
        }

        Club club = admin.getClub();

        String inviteCode = inviteCodeGenerator.generateInviteCode(club);

        return InviteDto.builder()
                .clubId(club.getId())
                .inviteCode(inviteCode)
                .build();
    }

    public InviteDto verifyInviteCode(String inviteCode) {

        return inviteCodeGenerator.verify(inviteCode);

    }
    @Transactional
    public ClubMemberDto.Response joinClub(Long memberId, ClubMemberDto.Create createDto) {

        Club club = clubRepository.findById(createDto.getClubId())
                .orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->new MemberException(MEMBER_NOT_FOUND));

        member.updateRecentClub(club);

        return clubMemberService.create(memberId, createDto);
    }


    /**
     * utility methods
     */

    private static void checkUpdatable(ClubMember clubMember, Long clubId) {
        checkSameClub(clubMember, clubId);

        if (!clubMember.checkRoleIs(ADMIN)) {
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
