package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p, l.id, (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount, (select count(*) from Comment c where c.post.id = p.id) as commentCount " +
            "from Post p " +
            "join fetch p.clubMember " +
            "left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId " +
            "where p.club.id = :clubId " +
            "order by p.isFixed desc, p.createdDate desc")
    Slice<Object[]> findByClubId(@Param(value = "clubId") Long clubId, @Param(value = "clubMemberId") Long clubMemberId, Pageable pageable);

    @Query("select p, l.id, (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount, (select count(*) from Comment c where c.post.id = p.id) as commentCount " +
            "from Post p " +
            "join fetch p.clubMember " +
            "left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId " +
            "where p.club.id = :clubId " +
            "and p.clubMember.id = :clubMemberId "  +
            "order by p.createdDate desc")
    Slice<Object[]> findMyPostsByClubId(@Param(value = "clubMemberId") Long clubMemberId,@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("select p, l.id, (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount, (select count(*) from Comment c where c.post.id = p.id) as commentCount " +
            "from Post p " +
            "join fetch p.clubMember " +
            "join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId " +
            "where p.club.id = :clubId " +
            "order by p.createdDate desc")
    Slice<Object[]> findLikePosts(@Param(value = "clubMemberId") Long clubMemberId,@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("select p " +
            "from Post p " +
            "join fetch p.clubMember " +
            "join fetch p.club " +
            "where p.id = :id")
    Optional<Post> findById(@Param(value = "id")Long id);


    @Query("""
            SELECT p, l.id, (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount,  (select count(*) from Comment c where c.post.id = p.id) as commentCount 
            from Post p
            join fetch p.clubMember cm
            left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId 
            join Comment c on c.post.id = p.id and c.clubMember.id = :clubMemberId
            group by p.id,cm.id,l.id
            order by max(c.lastModifiedDate) desc
            """)
    // 최근 단 댓글 순서대로 post 정렬
    Slice<Object[]> findMyCommentedPosts(@Param(value = "clubMemberId") Long clubMemberId, Pageable pageable);


}
