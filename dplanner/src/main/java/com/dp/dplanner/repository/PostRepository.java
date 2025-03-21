package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            select p,
                   l.id,
                   (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount,
                   (select count(*) from Comment c where c.post.id = p.id) as commentCount
            from Post p
            join fetch p.clubMember
            left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId
            left join PostBlock pb on p.id = pb.id.post.id and pb.id.clubMember.id = :clubMemberId
            left join ClubMemberBlock cb on p.clubMember.id = cb.id.blockedClubMember.id and cb.id.clubMember.id = :clubMemberId
            where p.club.id = :clubId
              and pb.id.post.id is null
              and cb.id.blockedClubMember.id is null
            order by p.isFixed desc, p.createdDate desc
        """)
    Slice<Object[]> findByClubId(@Param(value = "clubId") Long clubId, @Param(value = "clubMemberId") Long clubMemberId, Pageable pageable);

    @Query("""
            select p,
                   l.id,
                   (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount,
                   (select count(*) from Comment c where c.post.id = p.id) as commentCount
            from Post p
            join fetch p.clubMember
            left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId
            left join PostBlock pb on p.id = pb.id.post.id and pb.id.clubMember.id = :clubMemberId
            where p.club.id = :clubId
              and p.clubMember.id = :clubMemberId
              and pb.id.post.id is null
            order by p.createdDate desc
        """)
    Slice<Object[]> findMyPostsByClubId(@Param(value = "clubMemberId") Long clubMemberId,@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("""
            select p,
                   l.id,
                   (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount,
                   (select count(*) from Comment c where c.post.id = p.id) as commentCount
            from Post p
            join fetch p.clubMember
            join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId
            left join PostBlock pb on p.id = pb.id.post.id and pb.id.clubMember.id = :clubMemberId
            left join ClubMemberBlock cb on p.clubMember.id = cb.id.blockedClubMember.id and cb.id.clubMember.id = :clubMemberId
            where p.club.id = :clubId
              and pb.id.post.id is null
              and cb.id.blockedClubMember.id is null
            order by p.createdDate desc
        """)
    Slice<Object[]> findLikePosts(@Param(value = "clubMemberId") Long clubMemberId,@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("""
            select p
            from Post p
            join fetch p.clubMember
            join fetch p.club
            where p.id = :id
        """)
    Optional<Post> findById(@Param(value = "id")Long id);


    @Query("""
            select p,
                   l.id,
                   (select count(*) from PostMemberLike l2 where l2.post.id = p.id) as likeCount,
                   (select count(*) from Comment c where c.post.id = p.id) as commentCount,
                   (select max(c2.lastModifiedDate) from Comment c2 where c2.post.id = p.id and c2.clubMember.id = :clubMemberId) as lastModifiedDate
            from Post p
            join fetch p.clubMember cm
            left join PostMemberLike l on p.id = l.post.id and l.clubMember.id = :clubMemberId
            left join PostBlock pb on p.id = pb.id.post.id and pb.id.clubMember.id = :clubMemberId
            left join ClubMemberBlock cb on p.clubMember.id = cb.id.blockedClubMember.id and cb.id.clubMember.id = :clubMemberId
            where pb.id.post.id is null
              and cb.id.blockedClubMember.id is null
            order by lastModifiedDate desc
        """)
    // 최근 단 댓글 순서대로 post 정렬
    Slice<Object[]> findMyCommentedPosts(@Param(value = "clubMemberId") Long clubMemberId, Pageable pageable);


}
