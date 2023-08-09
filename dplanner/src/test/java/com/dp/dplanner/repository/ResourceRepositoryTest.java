package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class ResourceRepositoryTest {


    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    Club club;
    private Resource createResource() {
        return Resource.builder()
                .name("test")
                .info("test")
                .club(club)
                .build();
    }
    @BeforeEach
    public void setUp() {
        club = Club.builder().build();
        testEntityManager.persist(club);
    }

    @Test
    public void ResourceRepository_createResource_ReturnResource() {

        Resource resource = createResource();

        Resource savedResource = resourceRepository.save(resource);

        assertThat(savedResource).isNotNull();
        assertThat(savedResource.getId()).isGreaterThan(0);
        assertThat(savedResource.getClub()).isEqualTo(club);

    }

    @Test
    public void ResourceRepository_findResourceById_ReturnResource(){

        Resource resource = createResource();
        resourceRepository.save(resource);

        Resource foundResource = resourceRepository.findById(resource.getId()).get();

        assertThat(foundResource).isNotNull();
        assertThat(foundResource).isEqualTo(resource);
        assertThat(foundResource.getClub()).isEqualTo(club);
    }

    @Test
    public void ResourceRepository_deleteResource() {

        Resource resource = createResource();
        resourceRepository.save(resource);

        resourceRepository.delete(resource);

        Optional<Resource> deletedResource = resourceRepository.findById(resource.getId());
        assertThat(deletedResource).isEmpty();

    }


    @Test
    public void ResourceRepository_findByPostId_ReturnResourceList(){
        resourceRepository.save(createResource());
        resourceRepository.save(createResource());

        List<Resource> resourceList = resourceRepository.findByClubId(club.getId());

        assertThat(resourceList).isNotNull();
        assertThat(resourceList.size()).isEqualTo(2);
        assertThat(resourceList).extracting(Resource::getClub).extracting(Club::getId).containsOnly(club.getId());
    }
}
