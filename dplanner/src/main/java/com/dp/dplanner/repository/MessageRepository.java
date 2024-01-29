package com.dp.dplanner.repository;

import com.dp.dplanner.domain.message.PrivateMessage;
import com.dp.dplanner.domain.message.PublicMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public class MessageRepository {

    private final EntityManager entityManager;

    public MessageRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PrivateMessage save(PrivateMessage privateMessage) {
        entityManager.persist(privateMessage);
        return privateMessage;
    }

    public PublicMessage save(PublicMessage publicMessage) {
        entityManager.persist(publicMessage);
        return publicMessage;

    }

    public List<PrivateMessage> findAll(Long clubMemberId) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        String jpql =
                "SELECT pm FROM PrivateMessage pm " +
                "WHERE pm.clubMember.id = :clubMemberId " +
                "AND pm.createdDate >= :sixMonthsAgo " +
                "ORDER BY pm.createdDate desc ";

        TypedQuery<PrivateMessage> query = entityManager.createQuery(jpql, PrivateMessage.class);
        query.setParameter("clubMemberId", clubMemberId);
        query.setParameter("sixMonthsAgo", sixMonthsAgo);

        return query.getResultList();
    }



}
