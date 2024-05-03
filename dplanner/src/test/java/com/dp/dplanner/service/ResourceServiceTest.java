package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.ResourceType;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.ClubRepository;
import com.dp.dplanner.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.adapter.dto.ResourceDto.*;
import static com.dp.dplanner.exception.ErrorResult.DIFFERENT_CLUB_EXCEPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTest {

    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ClubRepository clubRepository;
    @Mock
    ResourceRepository resourceRepository;
    @InjectMocks
    ResourceService resourceService;

    Member member;
    Club club;
    ClubMember clubMember;
    Resource resource;
    Long clubId;
    Long memberId;
    Long clubMemberId;
    Long resourceId;


    @BeforeEach
    public void setUp() {

        memberId = 10L;
        member = Member.builder().build();
        ReflectionTestUtils.setField(member,"id",memberId);

        clubId = 20L;
        club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        clubMemberId = 30L;
        clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .build();
        ReflectionTestUtils.setField(clubMember,"id",clubMemberId);

        resourceId = 1L;
        resource = Resource.builder()
                .club(club)
                .resourceType(ResourceType.PLACE)
                .build();
        ReflectionTestUtils.setField(resource, "id", resourceId);

    }

    @Test
    public void ResourceService_createResource_ReturnResponseDto() {

        Create createDto = Create.builder()
                .name("test")
                .info("test")
                .clubId(clubId)
                .resourceType("PLACE")
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        when(clubRepository.findById(clubId)).thenReturn(Optional.ofNullable(club));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Response response = resourceService.createResource(clubMemberId, createDto);

        assertThat(response).isNotNull();
        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getName()).isEqualTo("test");
        assertThat(response.getInfo()).isEqualTo("test");
    }

    @Test
    @Disabled("어노테이션 사용으로 통합 테스트로 변경")
    public void ResourceService_createResource_ThrowException_NotAdmin() {

        Create createDto = Create.builder()
                .name("test")
                .info("test")
                .clubId(clubId)
                .build();
        clubMember.changeRole(ClubRole.MANAGER);

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        assertThatThrownBy(() -> resourceService.createResource(clubMemberId, createDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void ResourceService_createResource_ThrowException_DifferentClub() {

        Club newClub = Club.builder().build();
        ReflectionTestUtils.setField(newClub, "id", clubId + 1);

        Create createDto = Create.builder()
                .name("test")
                .info("test")
                .clubId(clubId+1)
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(clubRepository.findById(createDto.getClubId())).thenReturn(Optional.ofNullable(newClub));

        BaseException clubMemberException = assertThrows(ServiceException.class, () -> resourceService.createResource(clubMemberId, createDto));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }
    
    @Test
    public void ResourceService_deleteResource_ReturnVoid(){

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));
        resourceService.deleteResource(clubMemberId, resourceId);

    }

    @Test
    @Disabled("어노테이션 사용으로 통합 테스트로 변경")
    public void ResourceService_deleteResource_ThrowException_NotAdmin(){

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        assertThatThrownBy(() -> resourceService.deleteResource(clubMemberId, resourceId)).isInstanceOf(RuntimeException.class);

    }

    @Test
    public void ResourceService_deleteResource_ThrowException_ResourceNotBelongToClub(){

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));
        Club newClub = Club.builder().build();
        ReflectionTestUtils.setField(resource,"club",newClub);

        BaseException clubMemberException = assertThrows(ServiceException.class, ()->resourceService.deleteResource(clubMemberId, resourceId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    public void ResourceService_updateResource_ReturnResponse() {

        Update updateDto = Update.builder()
                .id(resourceId)
                .name("updateName")
                .info("updateInfo")
                .resourceType("PLACE")
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(updateDto.getId())).thenReturn(Optional.ofNullable(resource));

        Response response = resourceService.updateResource(clubMemberId, updateDto);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("updateName");
        assertThat(response.getInfo()).isEqualTo("updateInfo");
    }

    @Test
    public void ResourceService_updateResource_ThrowException_ResourceNotBelongToClub() {

        Club newClub = Club.builder().build();
        ReflectionTestUtils.setField(newClub, "id", clubId + 1);
        Update updateDto = Update.builder()
                .id(resourceId)
                .name("updateName")
                .info("updateInfo")
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(updateDto.getId())).thenReturn(Optional.ofNullable(resource));
        ReflectionTestUtils.setField(resource,"club",newClub);

        BaseException clubMemberException = assertThrows(ServiceException.class, () -> resourceService.updateResource(clubMemberId, updateDto));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    public void ResourceService_getResourceById_ReturnResponse(){

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));

        Response response = resourceService.getResourceById(clubMemberId, resourceId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isGreaterThan(0);
        assertThat(response.getClubId()).isEqualTo(clubId);
    }


    @Test
    public void ResourceService_getResourceById_ThrowException_ResourceNotBelongToClub(){

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));
        Club newClub = Club.builder().build();
        ReflectionTestUtils.setField(resource,"club",newClub);

        BaseException clubMemberException = assertThrows(ServiceException.class, () -> resourceService.getResourceById(clubMemberId, resourceId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    public void ResourceService_getResourcesByClubId_ReturnResponseList(){

        Resource resource1 = Resource.builder()
                .club(club)
                .resourceType(ResourceType.PLACE)
                .build();
        ReflectionTestUtils.setField(resource1, "id", 1L);

        Resource resource2 = Resource.builder()
                .club(club)
                .resourceType(ResourceType.PLACE)
                .build();
        ReflectionTestUtils.setField(resource2, "id", 2L);

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findByClubId(clubId)).thenReturn(Arrays.asList(resource1,resource2));
        List<Response> responseList = resourceService.getResourceByClubId(clubMemberId, clubId);

        assertThat(responseList).isNotNull();
        assertThat(responseList.size()).isEqualTo(2);
        assertThat(responseList).extracting(Response::getClubId).containsOnly(clubId);

    }
}

