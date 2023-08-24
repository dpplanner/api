package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.ResourceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.SCHEDULE_ALL;
import static com.dp.dplanner.dto.ResourceDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    @RequiredAuthority(SCHEDULE_ALL)
    public Response createResource(Long clubMemberId, Create createDto) {

        Club club = getClub(createDto.getClubId());
        checkIfSameClub(clubMemberId, club.getId());

        Resource resource = resourceRepository.save(createDto.toEntity(club));

        return Response.of(resource);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public Response updateResource(Long clubMemberId, Update updateDto) {

        Resource resource = getResource(updateDto.getId());
        checkIfSameClub(clubMemberId, resource.getClub().getId());

        resource.update(updateDto.getName(), updateDto.getInfo());

        return Response.of(resource);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void deleteResource(Long clubMemberId, Long resourceId) {

        Resource resource = getResource(resourceId);
        checkIfSameClub(clubMemberId, resource.getClub().getId());

        resourceRepository.delete(resource);
    }

    public Response getResourceById(Long clubMemberId, Long resourceId) {

        Resource resource = getResource(resourceId);
        checkIfSameClub(clubMemberId,resource.getClub().getId());

        return Response.of(resource);
    }

    public List<Response> getResourceByClubId(Long clubMemberId, Long clubId) {

        checkIfSameClub(clubMemberId, clubId);
        List<Resource> resourceList = resourceRepository.findByClubId(clubId);

        return Response.ofList(resourceList);
    }

    private  void checkIfSameClub(Long clubMemberId, Long clubId) {
        ClubMember clubMember = getClubMember(clubMemberId);
        if (!clubMember.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }
    }

    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
    }

    private Club getClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));
    }

    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(() -> new ResourceException(RESOURCE_NOT_FOUND));
    }
}
