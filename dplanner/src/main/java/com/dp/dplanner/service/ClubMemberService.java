package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.exception.ClubAuthorityException;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.ClubAuthorityRepository;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Log4j2
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClubMemberService {

    private final MessageService messageService;
    private final UploadService uploadService;
    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorityRepository clubAuthorityRepository;

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


        List<Long> managerIds = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(club.getId(), MEMBER_ALL).stream().map(ClubMember::getId).collect(Collectors.toList());
        messageService.createPrivateMessage(managerIds, Message.clubJoinMessage());

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

    public List<ClubMemberDto.Response> findMyClubMembers(Long clubMemberId,Long clubId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        Club club = clubMember.getClub();

        List<ClubMember> clubMembers;
        clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);

/*        if (clubMember.hasAuthority(MEMBER_ALL)) {
            clubMembers = clubMemberRepository.findAllByClub(club);
        } else {
            clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);
        }*/

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    @RequiredAuthority(authority = MEMBER_ALL)
    public List<ClubMemberDto.Response> findMyClubMembers(Long managerId, Long clubId, boolean confirmed) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!manager.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

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
    @RequiredAuthority(role = ADMIN)
    public ClubMemberDto.Response updateClubMemberClubAuthority(Long adminId, Long clubId, ClubMemberDto.Update updateDto) {


        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!admin.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        Long clubMemberId = updateDto.getId();
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isSameClub(admin)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        if (updateDto.getClubAuthorityId() != null) {
            ClubAuthority clubAuthority = clubAuthorityRepository.findById(updateDto.getClubAuthorityId())
                    .orElseThrow(() -> new ClubAuthorityException(CLUB_AUTHORITY_NOT_FOUND));
            if (!admin.isSameClub(clubAuthority.getClub().getId())) {
                throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
            }
            clubMember.updateClubAuthority(clubAuthority);
        }

        if (updateDto.getRole() != null) {
            if (updateDto.getRole().equals(USER.name())) {
                clubMember.updateClubAuthority(null);
            }
            clubMember.changeRole(ClubRole.valueOf(updateDto.getRole()));
        }

        return ClubMemberDto.Response.of(clubMember);
    }

    @Transactional
    @RequiredAuthority(authority = MEMBER_ALL)
    public void confirmAll(Long managerId, List<ClubMemberDto.Request> requestDto) {
        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        List<Long> unconfirmedClubMemberIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> unconfirmedClubMembers = clubMemberRepository.findAllById(unconfirmedClubMemberIds);

        //TODO unconfirmedClubMembers 빈 리스트 일 때 응답 구분
        unconfirmedClubMembers.forEach(clubMember -> {
            if (!clubMember.isSameClub(manager)) {
                throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        unconfirmedClubMembers.forEach(ClubMember::confirm);
    }

    @Transactional
    public void leaveClub(Long clubMemberId, ClubMemberDto.Request requestDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isSameClub(requestDto.getId())) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        if (clubMember.checkRoleIs(ADMIN)) {
            throw new ClubMemberException(DELETE_AUTHORIZATION_DENIED);
        }

        clubMemberRepository.delete(clubMember);
    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public void kickOut(Long managerId, Long clubId, ClubMemberDto.Request requestDto) {

        ClubMember admin = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!admin.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        ClubMember clubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (invalidKickOutRequest(admin, clubMember)) {
            throw new ClubMemberException(DELETE_AUTHORIZATION_DENIED);
        }

        clubMemberRepository.delete(clubMember);
    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public List<ClubMemberDto.Response> kickOutAll(Long adminId, Long clubId, List<ClubMemberDto.Request> requestDto) {

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!admin.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }

        List<Long> requestIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> clubMembers = clubMemberRepository.findAllById(requestIds);

        List<ClubMember> deletedClubMembers = clubMembers.stream()
                .filter(clubMember -> !invalidKickOutRequest(admin, clubMember))
                .toList();

        clubMemberRepository.deleteAll(deletedClubMembers);

        return ClubMemberDto.Response.ofList(deletedClubMembers);
    }

    @Transactional
    public ClubMemberDto.Response changeClubMemberProfileImage(Long clubMemberId, MultipartFile image) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        String url = uploadService.uploadFile(image);
        clubMember.updateProfileUrl(url);

        return ClubMemberDto.Response.of(clubMember);
    }


    /**
     * AOP method
     */
    public boolean hasAuthority(Long clubMemberId, ClubAuthorityType authority) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        return clubMember.hasAuthority(authority);
    }

    public boolean hasRole(Long clubMemberId, ClubRole role) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        return clubMember.hasRole(role);
    }



    /**
     * utility methods
     */
    private static boolean invalidKickOutRequest(ClubMember manager, ClubMember clubMember) {

        // 본인이 자기 자신 KickOut 하는 경우
        if (clubMember.equals(manager)) {
            return true;
        }
        // 다른 클럽인 경우
        if (!manager.isSameClub(clubMember)) {
            return true;
        }
        // 매니저가 ADMIN kickOut 하는 경우
        if ((isMemberManager(manager) && clubMember.checkRoleIs(ADMIN))) {
            return true;
        }

        return false;
    }

    private static boolean isMemberManager(ClubMember clubMember) {
        return clubMember.checkRoleIs(MANAGER) && clubMember.hasAuthority(MEMBER_ALL);
    }
}
