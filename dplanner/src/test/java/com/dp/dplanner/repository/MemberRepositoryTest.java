package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {


    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    @DisplayName("Member가 정상적으로 저장됨")
    public void save() throws Exception {
        //given
        Member member = Member.builder().name("user").info("userInfo").build();
        //when
        Member savedMember = memberRepository.save(member);
        //then
        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getName()).isEqualTo("user");
        assertThat(savedMember.getInfo()).isEqualTo("userInfo");
    }

    @Test
    @DisplayName("memberId로 클럽 조회")
    public void findById() throws Exception {
        //given
        Member member = Member.builder().name("user").info("userInfo").build();
        Member savedMember = memberRepository.save(member);
        //when
        Member findMember = memberRepository.findById(savedMember.getId()).orElse(null);
        //then
        assertThat(findMember.getId()).isEqualTo(savedMember.getId());
        assertThat(findMember.getName()).isEqualTo("user");
        assertThat(findMember.getInfo()).isEqualTo("userInfo");
    }
}