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
    @DisplayName("Member 저장")
    public void save() throws Exception {
        //given
        Member member = Member.builder().build();
        //when
        Member savedMember = memberRepository.save(member);
        //then
        assertThat(savedMember.getId()).isNotNull();
    }

    @Test
    @DisplayName("memberId로 조회")
    public void findById() throws Exception {
        //given
        Member member = Member.builder().build();
        Member savedMember = memberRepository.save(member);
        //when
        Member findMember = memberRepository.findById(savedMember.getId()).orElse(null);
        //then
        assertThat(findMember.getId()).isEqualTo(savedMember.getId());
    }
}