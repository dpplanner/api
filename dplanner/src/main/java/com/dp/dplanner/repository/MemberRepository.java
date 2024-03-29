package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByRefreshToken(String RefreshToken);

    @Modifying
    @Query("update Member m set m.refreshToken = :refreshToken where m.id = :memberId")
    void updateRefreshToken(@Param("memberId") Long memberId,
                            @Param("refreshToken") String refreshToken);


    @Query("""
            select m.fcmToken
            from ClubMember cm
            join Member m on cm.member.id = m.id
            where cm.id in :clubMemberIds
            """)
    List<String> getFcmTokensUsingClubMemberIds(@Param("clubMemberIds") List<Long> clubMemberIds);

}
