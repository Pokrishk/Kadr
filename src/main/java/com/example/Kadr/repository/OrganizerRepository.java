package com.example.Kadr.repository;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    boolean existsByUser(User user);
    boolean existsByUser_Id(Long userId);
    Optional<Organizer> findByUser(User user);

    @EntityGraph(attributePaths = {"user"})
    Page<Organizer> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Organizer> findByOrganizationNameContainingIgnoreCase(String q, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Page<Organizer> findByUser_Role_Id(Long roleId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Page<Organizer> findByUser_Role_IdAndOrganizationNameContainingIgnoreCase(
            Long roleId, String q, Pageable pageable
    );
    @EntityGraph(attributePaths = {"user", "user.role"})
    List<Organizer> findAllByUser_Role_Id(Long roleId);

    Optional<Organizer> findByUser_UsernameIgnoreCase(String username);
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("select o from Organizer o join o.user u join u.role r where upper(r.title) <> upper(:organizerTitle)")
    Page<Organizer> findPendingRequests(@Param("organizerTitle") String organizerTitle, Pageable pageable);

    @Query("select count(o) from Organizer o join o.user u join u.role r where upper(r.title) <> upper(:organizerTitle)")
    long countPendingRequests(@Param("organizerTitle") String organizerTitle);
}
