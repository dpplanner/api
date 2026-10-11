package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

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

    private Member withdrawn(String email, LocalDateTime deletedAt) {
        Member member = memberRepository.save(Member.builder().email(email).name("이름").refreshToken("refresh").build());
        member.updateFCMToken("fcm");
        testEntityManager.flush();
        memberRepository.delete(member);
        testEntityManager.flush();
        testEntityManager.getEntityManager()
                .createNativeQuery("update member set deleted_at = ? where id = ?")
                .setParameter(1, deletedAt)
                .setParameter(2, member.getId())
                .executeUpdate();
        return member;
    }

    private Member reload(Member member) {
        testEntityManager.clear();
        return testEntityManager.find(Member.class, member.getId());
    }

    @Test
    @DisplayName("탈퇴 시 is_deleted·deleted_at 기록, 로그인·푸시 토큰 즉시 삭제, 이메일·이름은 유지")
    public void leave_marksDeletedAt() {
        Member member = memberRepository.save(Member.builder().email("a@example.com").name("이름").refreshToken("refresh").build());
        member.updateFCMToken("fcm");
        testEntityManager.flush();

        memberRepository.delete(member);
        testEntityManager.flush();

        Member found = reload(member);
        assertThat(found.getIsDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isNotNull();
        assertThat(found.getRefreshToken()).isNull();
        assertThat(found.getFcmToken()).isNull();
        assertThat(found.getEmail()).isEqualTo("a@example.com");
        assertThat(memberRepository.findByEmail("a@example.com")).isEmpty();
    }

    @Test
    @DisplayName("탈퇴 30일 지난 회원과 deleted_at 없는 옛 탈퇴 회원만 익명화, 30일 안 된 회원·정상 회원은 그대로")
    public void anonymizeWithdrawnBefore() {
        LocalDateTime now = LocalDateTime.now();
        Member old = withdrawn("old@example.com", now.minusDays(31));
        Member legacy = withdrawn("legacy@example.com", null);
        Member recent = withdrawn("recent@example.com", now.minusDays(29));
        Member active = memberRepository.save(Member.builder().email("active@example.com").name("활동").build());
        testEntityManager.flush();

        int count = memberRepository.anonymizeWithdrawnBefore(now.minusDays(30));

        assertThat(count).isEqualTo(2);
        assertThat(reload(old).getEmail()).isEqualTo("deleted_" + old.getId());
        assertThat(reload(old).getName()).isEqualTo("(탈퇴한 회원)");
        assertThat(reload(legacy).getEmail()).isEqualTo("deleted_" + legacy.getId());
        assertThat(reload(recent).getEmail()).isEqualTo("recent@example.com");
        assertThat(reload(active).getEmail()).isEqualTo("active@example.com");
        assertThat(reload(active).getName()).isEqualTo("활동");

        // 다시 돌려도 이미 익명화된 회원은 건드리지 않는다
        assertThat(memberRepository.anonymizeWithdrawnBefore(now.minusDays(30))).isZero();
    }
}
