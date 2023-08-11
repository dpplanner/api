package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.SCHEDULE_ALL;
import static com.dp.dplanner.dto.ResourceDto.*;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    @RequiredAuthority(SCHEDULE_ALL)
    public Response createResource(long clubMemberId, Create createDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);

        checkIfSameClub(clubMember, createDto.getClubId());

        Club club = clubRepository.findById(createDto.getClubId()).orElseThrow(RuntimeException::new);

        Resource resource = resourceRepository.save(createDto.toEntity(club));


        return Response.of(resource);
    }
    @RequiredAuthority(SCHEDULE_ALL)
    public Response updateResource(Long clubMemberId, Update updateDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);

        Resource resource = resourceRepository.findById(updateDto.getId()).orElseThrow(RuntimeException::new);
        resource.update(updateDto.getName(), updateDto.getInfo());

        return Response.of(resource);
    }
    @RequiredAuthority(SCHEDULE_ALL)
    public void deleteResource(Long clubMemberId, Long resourceId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);


        Resource resource = resourceRepository.findById(resourceId).orElseThrow(RuntimeException::new);

        checkIfResourceBelongToClub(clubMember, resource);

        resourceRepository.delete(resource);



    }

    public Response getResourceById(Long clubMemberId, Long resourceId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        Resource resource = resourceRepository.findById(resourceId).orElseThrow(RuntimeException::new);

        checkIfResourceBelongToClub(clubMember, resource);

        return Response.of(resource);

    }

    public List<Response> getResourceByClubId(Long clubMemberId, Long clubId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);

        checkIfSameClub(clubMember, clubId);

        List<Resource> resourceList = resourceRepository.findByClubId(clubId);

        return Response.ofList(resourceList);
    }

    private  void checkIfSameClub(ClubMember clubMember, Long clubId) {
        if (!clubMember.getClub().getId().equals(clubId)) {
            throw new RuntimeException();
        }
    }


    private void checkIfResourceBelongToClub(ClubMember clubMember, Resource resource) {
        if (!resource.getClub().getId().equals(clubMember.getClub().getId())) {
            throw new RuntimeException();
        }
    }
}
