package com.dp.dplanner.service;

import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.ResourceType;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.adapter.dto.ResourceDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    @RequiredAuthority(authority = RESOURCE_ALL)
    @Transactional
    public Response createResource(Long clubMemberId, Create createDto) {
        Club club = getClub(createDto.getClubId());
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, club.getId());

        Resource resource = resourceRepository.save(createDto.toEntity(club));
        return Response.of(resource);
    }

    @RequiredAuthority(authority = RESOURCE_ALL)
    @Transactional
    public Response updateResource(Long clubMemberId, Update updateDto) {
        Resource resource = getResource(updateDto.getId());
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        resource.update(updateDto.getName(), updateDto.getInfo(), updateDto.isReturnMessageRequired(), updateDto.getNotice(), ResourceType.valueOf(updateDto.getResourceType()), updateDto.getBookableSpan());
        return Response.of(resource);
    }

    @RequiredAuthority(authority = RESOURCE_ALL)
    @Transactional
    public void deleteResource(Long clubMemberId, Long resourceId) {
        Resource resource = getResource(resourceId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        resourceRepository.delete(resource);
    }

    public Response getResourceById(Long clubMemberId, Long resourceId) {
        Resource resource = getResource(resourceId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember,resource.getClub().getId());

        return Response.of(resource);
    }

    public List<Response> getResourceByClubId(Long clubMemberId, Long clubId) {
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, clubId);
        List<Resource> resourceList = resourceRepository.findByClubId(clubId);

        return Response.ofList(resourceList);
    }



    /**
     * utility methods
     */
    private static void checkIsSameClub(ClubMember clubMember, Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
    }
    private Club getClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));
    }
    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(() -> new ServiceException(RESOURCE_NOT_FOUND));
    }
}
