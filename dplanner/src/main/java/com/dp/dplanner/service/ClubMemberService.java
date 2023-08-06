package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.repository.ClubMemberRepository;
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

    private final ClubMemberRepository clubMemberRepository;


    public ClubMemberDto.Response findById(Long clubMemberId) throws NoSuchElementException{
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isConfirmed()) {
            throw new NoSuchElementException();
        }

        return ClubMemberDto.Response.of(clubMember);
    }

    public List<ClubMemberDto.Response> findMyClubMembers(Long clubMemberId) throws NoSuchElementException{

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);
        Club club = clubMember.getClub();

        List<ClubMember> clubMembers;
        if (clubMember.checkRoleIs(ADMIN) || isMemberManager(clubMember)) {
            clubMembers = clubMemberRepository.findAllByClub(club);
        } else {
            clubMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);
        }

        return ClubMemberDto.Response.ofList(clubMembers);
    }

    public ClubMemberDto.Response update(Long clubMemberId, ClubMemberDto.Update updateDto)
        throws IllegalStateException, NoSuchElementException{

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
            throws IllegalStateException, NoSuchElementException{

        if (adminId.equals(updateDto.getId())) {
            throw new IllegalStateException();
        }

        ClubMember admin = clubMemberRepository.findById(adminId)
                .orElseThrow(NoSuchElementException::new);

        if (admin.checkRoleIs(ADMIN)) {

            Long clubMemberId = updateDto.getId();
            ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                    .orElseThrow(NoSuchElementException::new);

            if (!clubMember.isConfirmed() || !isSameClubMember(admin, clubMember)) {
                throw new IllegalStateException();
            }

            clubMember.changeRole(ClubRole.valueOf(updateDto.getRole()));
        } else {
            throw new IllegalStateException();
        }
    }

    public void leaveClub(Long clubMemberId) throws IllegalStateException, NoSuchElementException{
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        if (clubMember.checkRoleIs(ADMIN)) {
            throw new IllegalStateException();
        }

        clubMemberRepository.delete(clubMember);
    }

    public void kickOut(Long managerId, ClubMemberDto.Delete deleteDto)
            throws IllegalStateException, NoSuchElementException{

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        if (manager.checkRoleIs(ADMIN) || isMemberManager(manager)) {
            ClubMember clubMember = clubMemberRepository.findById(deleteDto.getId())
                    .orElseThrow(NoSuchElementException::new);

            if (invalidKickOutRequest(manager, clubMember)) {
                throw new IllegalStateException();
            }

            clubMemberRepository.delete(clubMember);
        } else {
            throw new IllegalStateException();
        }

    }

    /**
     * utility methods
     */
    private static boolean isSameClubMember(ClubMember memberManager, ClubMember clubMember) {
        return clubMember.getClub().equals(memberManager.getClub());
    }

    private static boolean isMemberManager(ClubMember clubMember) {
        return clubMember.checkRoleIs(MANAGER) &&
                clubMember.getClub().hasAuthority(MEMBER_ALL);
    }

    private static boolean invalidKickOutRequest(ClubMember manager, ClubMember clubMember) {

        if (clubMember.equals(manager)) {
            return true;
        }

        if (!isSameClubMember(manager, clubMember)) {
            return true;
        }

        if ((isMemberManager(manager) && clubMember.checkRoleIs(ADMIN))) {
            return true;
        }

        return false;
    }
}
