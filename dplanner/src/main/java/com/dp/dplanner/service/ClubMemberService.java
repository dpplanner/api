package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClubMemberService {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public ClubMemberDto.Response create(Long memberId, ClubMemberDto.Create createDto) {

        ClubMember exsitingClubMember =
                clubMemberRepository.findByClubIdAndMemberId(createDto.getClubId(), memberId)
                        .orElse(null);

        if (exsitingClubMember != null) {
            throw new ClubMemberException(CLUBMEMBER_ALREADY_EXISTS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        Club club = clubRepository.findById(createDto.getClubId())
                .orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));

        ClubMember clubMember = createDto.toEntity(member, club);
        ClubMember savedMember = clubMemberRepository.save(clubMember);

        return ClubMemberDto.Response.of(savedMember);
    }

    public ClubMemberDto.Response findById(Long clubMemberId, ClubMemberDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        ClubMember requestClubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isSameClub(requestClubMember)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        if (!requestClubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        return ClubMemberDto.Response.of(requestClubMember);
    }

    public List<ClubMemberDto.Response> findMyClubMembers(Long clubMemberId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        Club club = clubMember.getClub();

        List<ClubMember> clubMembers;
        if (clubMember.hasAuthority(MEMBER_ALL)) {
            clubMembers = clubMemberRepository.findAllByClub(club);
        } else {
            clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);
        }

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    @RequiredAuthority(MEMBER_ALL)
    public List<ClubMemberDto.Response> findMyClubMembers(Long managerId, boolean confirmed) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        List<ClubMember> clubMembers;
        if (confirmed) {
            clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(manager.getClub());
        } else {
            clubMembers = clubMemberRepository.findAllUnconfirmedClubMemberByClub(manager.getClub());
        }

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    @Transactional
    public ClubMemberDto.Response update(Long clubMemberId, ClubMemberDto.Update updateDto) {

        if (!clubMemberId.equals(updateDto.getId())) {
            throw new ClubMemberException(UPDATE_AUTHORIZATION_DENIED);
        }

        ClubMember clubMember = clubMemberRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        clubMember.update(updateDto.getName(), updateDto.getInfo());
        return ClubMemberDto.Response.of(clubMember);
    }

    @Transactional
    public void changeClubMemberRole(Long adminId, ClubMemberDto.Update updateDto) {

        if (adminId.equals(updateDto.getId())) {
            throw new ClubMemberException(UPDATE_AUTHORIZATION_DENIED);
        }

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (admin.checkRoleIs(ADMIN)) { //TODO aop로 분리?

            Long clubMemberId = updateDto.getId();
            ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                    .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

            if (!clubMember.isSameClub(admin)) {
                throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
            }

            if (!clubMember.isConfirmed()) {
                throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
            }

            clubMember.changeRole(ClubRole.valueOf(updateDto.getRole()));
        } else {
            throw new ClubMemberException(UPDATE_AUTHORIZATION_DENIED);
        }
    }

    @Transactional
    @RequiredAuthority(MEMBER_ALL)
    public void confirmAll(Long managerId, List<ClubMemberDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        List<Long> unconfirmedClubMemberIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> unconfirmedClubMembers = clubMemberRepository.findAllById(unconfirmedClubMemberIds);

        unconfirmedClubMembers.forEach(clubMember -> {
            if (!clubMember.isSameClub(manager)) {
                throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        unconfirmedClubMembers.forEach(ClubMember::confirm);
    }

    @Transactional
    public void leaveClub(Long clubMemberId) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (clubMember.checkRoleIs(ADMIN)) {
            throw new ClubMemberException(DELETE_AUTHORIZATION_DENIED);
        }

        clubMemberRepository.delete(clubMember);
    }

    @Transactional
    @RequiredAuthority(MEMBER_ALL)
    public void kickOut(Long managerId, ClubMemberDto.Request requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        ClubMember clubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (invalidKickOutRequest(manager, clubMember)) {
            throw new ClubMemberException(DELETE_AUTHORIZATION_DENIED);
        }

        clubMemberRepository.delete(clubMember);

    }

    @Transactional
    @RequiredAuthority(MEMBER_ALL)
    public List<ClubMemberDto.Response> kickOutAll(Long managerId, List<ClubMemberDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        List<Long> requestIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> clubMembers = clubMemberRepository.findAllById(requestIds);

        List<ClubMember> deletedClubMembers = clubMembers.stream()
                .filter(clubMember -> !invalidKickOutRequest(manager, clubMember))
                .toList();

        clubMemberRepository.deleteAll(deletedClubMembers);

        clubMembers.removeAll(deletedClubMembers);

        return ClubMemberDto.Response.ofList(clubMembers);
    }


    /**
     * AOP method
     */
    public boolean hasAuthority(Long clubMemberId, ClubAuthorityType authority) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        return clubMember.hasAuthority(authority);
    }


    /**
     * utility methods
     */
    private static boolean invalidKickOutRequest(ClubMember manager, ClubMember clubMember) {

        if (clubMember.equals(manager)) {
            return true;
        }

        if (!manager.isSameClub(clubMember)) {
            return true;
        }

        if ((isMemberManager(manager) && clubMember.checkRoleIs(ADMIN))) {
            return true;
        }

        return false;
    }

    private static boolean isMemberManager(ClubMember clubMember) {
        return clubMember.checkRoleIs(MANAGER) && clubMember.hasAuthority(MEMBER_ALL);
    }
}
