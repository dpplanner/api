package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


    @Query("""
            select cm.id, m.fcmToken
            from ClubMember cm
            join Member m on cm.member.id = m.id
            where cm.id in :clubMemberIds and m.isDeleted=false and cm.isDeleted=false
            """)
    List<Object[]> getFcmTokensUsingClubMemberIds(@Param("clubMemberIds") List<Long> clubMemberIds);

}
