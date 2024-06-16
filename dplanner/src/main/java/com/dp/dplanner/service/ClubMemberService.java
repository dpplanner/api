package com.dp.dplanner.service;

import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.adapter.dto.ClubMemberDto;
import com.dp.dplanner.service.exception.ServiceException;
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
            throw new ServiceException(CLUBMEMBER_ALREADY_EXISTS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));

        Club club = clubRepository.findById(createDto.getClubId())
                .orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));

        ClubMember clubMember = createDto.toEntity(member, club);
        ClubMember savedMember = clubMemberRepository.save(clubMember);

        List<ClubMember> managers = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(club.getId(), MEMBER_ALL);

        messageService.createPrivateMessage(managers,
                Message.clubJoinMessage(
                        Message.MessageContentBuildDto.builder()
                                .clubMemberName(clubMember.getName())
                                .clubName(club.getClubName())
                                .info(String.valueOf(savedMember.getId()))
                                .build()));

        return ClubMemberDto.Response.of(savedMember);
    }

    public ClubMemberDto.Response findById(Long clubMemberId, ClubMemberDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        ClubMember requestClubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        checkIsSameClub(clubMember, requestClubMember.getClub().getId());
        checkIsConfirmed(requestClubMember);

        return ClubMemberDto.Response.of(requestClubMember);
    }

    /**
     * 일반적인 회원 목록 요청 처리
     */
    public List<ClubMemberDto.Response> findMyClubMembers(Long clubMemberId,Long clubId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        checkIsSameClub(clubMember, clubId);
        checkIsConfirmed(clubMember);

        Club club = clubMember.getClub();

        // todo clubAuthority에 권한 배열로 넣어서 조인 안 하고 가져올 수 있도록 변경
        List<ClubMember> clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);
        return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
    }

    @RequiredAuthority(authority = MEMBER_ALL)
    public List<ClubMemberDto.Response> findMyClubMembers(Long managerId, Long clubId, boolean confirmed) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        checkIsSameClub(manager, clubId);

        List<ClubMember> clubMembers = clubMemberRepository.findAllUnconfirmedClubMemberByClub(manager.getClub());

        return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
    }

    @Transactional
    public ClubMemberDto.Response update(Long clubMemberId, ClubMemberDto.Update updateDto) {

        if (!clubMemberId.equals(updateDto.getId())) {
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
        }

        ClubMember clubMember = clubMemberRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        checkIsConfirmed(clubMember);
        clubMember.update(updateDto.getName(), updateDto.getInfo());
        return ClubMemberDto.Response.of(clubMember);
    }


    @Transactional
    public ClubMemberDto.Response changeClubMemberProfileImage(Long clubMemberId, MultipartFile image) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        String url = uploadService.uploadFile(image);
        clubMember.updateProfileUrl(url);

        return ClubMemberDto.Response.of(clubMember);
    }

    @Transactional
    @RequiredAuthority(role = ADMIN)
    public ClubMemberDto.Response updateClubMemberClubAuthority(Long adminId, Long clubId, ClubMemberDto.Update updateDto) {


        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        checkIsSameClub(admin, clubId);

        Long clubMemberId = updateDto.getId();
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        checkIsSameClub(clubMember, clubId);
        checkIsConfirmed(clubMember);

        if (updateDto.getClubAuthorityId() != null) {
            ClubAuthority clubAuthority = clubAuthorityRepository.findById(updateDto.getClubAuthorityId())
                    .orElseThrow(() -> new ServiceException(CLUB_AUTHORITY_NOT_FOUND));
            checkIsSameClub(admin, clubAuthority.getClub().getId());
            clubMember.updateClubAuthority(clubAuthority);
        }

        if (updateDto.getRole() != null) {
            if (updateDto.getRole().equals(USER.name()) || updateDto.getRole().equals(ADMIN.name())) {
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
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        List<Long> unconfirmedClubMemberIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> unconfirmedClubMembers = clubMemberRepository.findAllById(unconfirmedClubMemberIds);
        Club club = clubRepository.findById(manager.getClub().getId())
                .orElseThrow(()-> new ServiceException(CLUB_NOT_FOUND));

        unconfirmedClubMembers.forEach(clubMember -> checkIsSameClub(manager,clubMember.getClub().getId()));
        unconfirmedClubMembers.forEach(ClubMember::confirm);

        messageService.sendFcmNotification(unconfirmedClubMembers,
                Message.clubJoinConfirmMessage(
                        Message.MessageContentBuildDto.builder()
                                .clubName(club.getClubName())
                                .build()),true);
    }

    @Transactional
    @RequiredAuthority(authority = MEMBER_ALL)
    public void rejectAll(Long managerId, List<ClubMemberDto.Request> requestDto) {
        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        List<Long> unconfirmedClubMemberIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> unconfirmedClubMembers = clubMemberRepository.findAllById(unconfirmedClubMemberIds);
        Club club = clubRepository.findById(manager.getClub().getId())
                .orElseThrow(()-> new ServiceException(CLUB_NOT_FOUND));

        unconfirmedClubMembers.forEach(clubMember -> checkIsSameClub(manager,clubMember.getClub().getId()));
        clubMemberRepository.deleteAll(unconfirmedClubMembers);

        messageService.sendFcmNotification(unconfirmedClubMembers,
                Message.clubJoinRejectMessage(
                        Message.MessageContentBuildDto.builder()
                                .clubName(club.getClubName())
                                .build()), false);
    }

    @Transactional
    public void leaveClub(Long clubMemberId, ClubMemberDto.Request requestDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        assert clubMemberId.equals(requestDto.getId());
        checkIsAdmin(clubMember);

        clubMemberRepository.delete(clubMember);
    }

    @Transactional
    @RequiredAuthority(authority = MEMBER_ALL)
    public void kickOut(Long managerId, Long clubId, ClubMemberDto.Request requestDto) {

        ClubMember admin = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        checkIsSameClub(admin, clubId);

        ClubMember clubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        if (invalidKickOutRequest(admin, clubMember)) {
            throw new ServiceException(DELETE_AUTHORIZATION_DENIED);
        }

        clubMemberRepository.delete(clubMember);
    }

    @Transactional
    @RequiredAuthority(authority = MEMBER_ALL)
    public List<ClubMemberDto.Response> kickOutAll(Long adminId, Long clubId, List<ClubMemberDto.Request> requestDto) {

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        checkIsSameClub(admin, clubId);

        List<Long> requestIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> clubMembers = clubMemberRepository.findAllById(requestIds);

        List<ClubMember> deletedClubMembers = clubMembers.stream()
                .filter(clubMember -> !invalidKickOutRequest(admin, clubMember))
                .toList();

        clubMemberRepository.deleteAll(deletedClubMembers);

        return ClubMemberDto.Response.ofList(deletedClubMembers);
    }



    /**
     * utility methods
     */
    private static void checkIsSameClub(ClubMember clubMember, Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
    private static void checkIsConfirmed(ClubMember clubMember) {
        if (!clubMember.getIsConfirmed()) {
            throw new ServiceException(CLUBMEMBER_NOT_CONFIRMED);
        }
    }
    private static void checkIsAdmin(ClubMember clubMember) {
        if (clubMember.checkRoleIs(ADMIN)) {
            throw new ServiceException(DELETE_AUTHORIZATION_DENIED);
        }
    }
    private static boolean invalidKickOutRequest(ClubMember manager, ClubMember clubMember) {

        // 본인이 자기 자신 KickOut 하는 경우
        if (clubMember.equals(manager)) {
            return true;
        }
        // 다른 클럽인 경우
        if (!manager.isSameClub(clubMember.getClub().getId())) {
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
