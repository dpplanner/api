package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.domain.club.ClubRole.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubMemberService {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;


    public ClubMemberDto.Response create(Long memberId, ClubMemberDto.Create createDto)
            throws NoSuchElementException, IllegalStateException {

        ClubMember exsitingClubMember =
                clubMemberRepository.findByClubIdAndMemberId(createDto.getClubId(), memberId)
                        .orElse(null);

        if (exsitingClubMember != null) {
            throw new IllegalStateException();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);

        Club club = clubRepository.findById(createDto.getClubId())
                .orElseThrow(NoSuchElementException::new);

        ClubMember clubMember = createDto.toEntity(member, club);
        ClubMember savedMember = clubMemberRepository.save(clubMember);

        return ClubMemberDto.Response.of(savedMember);
    }

    public ClubMemberDto.Response findById(ClubMemberDto.Request requestDto) throws NoSuchElementException {

        ClubMember clubMember = clubMemberRepository.findById(requestDto.getId())
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isConfirmed()) {
            throw new NoSuchElementException();
        }

        return ClubMemberDto.Response.of(clubMember);
    }

    public List<ClubMemberDto.Response> findMyClubMembers(Long clubMemberId)
            throws IllegalStateException, NoSuchElementException {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isConfirmed()) {
            throw new IllegalStateException();
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
    public List<ClubMemberDto.Response> findUnconfirmedClubMembers(Long managerId)
            throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        List<ClubMember> unconfirmedClubMember = clubMemberRepository
                .findAllUnconfirmedClubMemberByClub(manager.getClub());

        return ClubMemberDto.Response.ofList(unconfirmedClubMember);
    }

    public ClubMemberDto.Response update(Long clubMemberId, ClubMemberDto.Update updateDto)
            throws IllegalStateException, NoSuchElementException {

        if (!clubMemberId.equals(updateDto.getId())) {
            throw new IllegalStateException();
        }

        ClubMember clubMember = clubMemberRepository.findById(updateDto.getId())
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isConfirmed()) {
            throw new IllegalStateException();
        }

        clubMember.update(updateDto.getName(), updateDto.getInfo());
        return ClubMemberDto.Response.of(clubMember);
    }

    public void changeClubMemberRole(Long adminId, ClubMemberDto.Update updateDto)
            throws IllegalStateException, NoSuchElementException {

        if (adminId.equals(updateDto.getId())) {
            throw new IllegalStateException();
        }

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(NoSuchElementException::new);

        if (admin.checkRoleIs(ADMIN)) {

            Long clubMemberId = updateDto.getId();
            ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                    .orElseThrow(NoSuchElementException::new);

            if (!clubMember.isConfirmed() || !clubMember.isSameClub(admin)) {
                throw new IllegalStateException();
            }

            clubMember.changeRole(ClubRole.valueOf(updateDto.getRole()));
        } else {
            throw new IllegalStateException();
        }
    }

    @RequiredAuthority(MEMBER_ALL)
    public void confirmAll(Long managerId, List<ClubMemberDto.Request> requestDto)
            throws IllegalStateException, NoSuchElementException{

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        List<Long> unconfirmedClubMemberIds = requestDto.stream().map(ClubMemberDto.Request::getId).toList();
        List<ClubMember> unconfirmedClubMembers = clubMemberRepository.findAllById(unconfirmedClubMemberIds);

        //TODO 클럽 검증

        unconfirmedClubMembers.forEach(ClubMember::confirm);
    }

    public void leaveClub(Long clubMemberId) throws IllegalStateException, NoSuchElementException {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (clubMember.checkRoleIs(ADMIN)) {
            throw new IllegalStateException();
        }

        clubMemberRepository.delete(clubMember);
    }

    @RequiredAuthority(MEMBER_ALL)
    public void kickOut(Long managerId, ClubMemberDto.Delete deleteDto)
            throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        ClubMember clubMember = clubMemberRepository.findById(deleteDto.getId())
                .orElseThrow(NoSuchElementException::new);

        if (invalidKickOutRequest(manager, clubMember)) {
            throw new IllegalStateException();
        }

        clubMemberRepository.delete(clubMember);

    }

    @RequiredAuthority(MEMBER_ALL)
    public List<ClubMemberDto.Response> kickOutAll(Long managerId, List<ClubMemberDto.Delete> deleteDto)
            throws IllegalStateException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        List<Long> requestIds = deleteDto.stream().map(ClubMemberDto.Delete::getId).toList();
        List<ClubMember> clubMembers = clubMemberRepository.findAllById(requestIds);

        List<ClubMember> deletedClubMembers = clubMembers.stream()
                .filter(clubMember -> !invalidKickOutRequest(manager, clubMember))
                .toList();

        clubMemberRepository.deleteAll(deletedClubMembers);

        clubMembers.removeAll(deletedClubMembers);

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    public boolean hasAuthority(Long clubMemberId, ClubAuthorityType authority)
            throws NoSuchElementException{

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

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
