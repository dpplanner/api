package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends CrudRepository<Member,Long> {

    @Query("SELECT m from Member m where m.id = :memberId and m.isDeleted=false ")
    Optional<Member> findById(@Param("memberId") Long memberId);

    @Query("SELECT m from Member m where m.email = :email and m.isDeleted=false ")
    Optional<Member> findByEmail(@Param("email") String email);
    @Query("SELECT m from Member m where m.refreshToken = :refreshToken and m.isDeleted=false ")
    Optional<Member> findByRefreshToken(@Param("refreshToken") String RefreshToken);

    @Modifying
    @Query("update Member m set m.refreshToken = :refreshToken where m.id = :memberId")
    void updateRefreshToken(@Param("memberId") Long memberId,
                            @Param("refreshToken") String refreshToken);


    /**
     * 탈퇴 후 보관 기간이 지난 회원의 계정 정보(이메일·이름)를 파기한다.
     * deletedAt 이 null 인 회원(컬럼 추가 전 탈퇴)도 보관 기간이 지난 것으로 본다.
     * 이미 익명화된 회원(email = 'deleted_{id}')은 다시 건드리지 않는다.
     */
    @Modifying
    @Query("""
            update Member m
            set m.email = concat('deleted_', cast(m.id as String)),
                m.name = '(탈퇴한 회원)',
                m.refreshToken = null,
                m.fcmToken = null
            where m.isDeleted = true
              and (m.deletedAt is null or m.deletedAt <= :threshold)
              and (m.email is null or m.email <> concat('deleted_', cast(m.id as String)))
            """)
    int anonymizeWithdrawnBefore(@Param("threshold") LocalDateTime threshold);

    @Query("""
            select cm.id, m.fcmToken
            from ClubMember cm
            join Member m on cm.member.id = m.id
            where cm.id in :clubMemberIds and m.isDeleted=false and cm.isDeleted=false
            """)
    List<Object[]> getFcmTokensUsingClubMemberIds(@Param("clubMemberIds") List<Long> clubMemberIds);

}
