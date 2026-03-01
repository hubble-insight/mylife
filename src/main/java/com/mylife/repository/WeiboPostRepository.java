package com.mylife.repository;

import com.mylife.model.WeiboPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeiboPostRepository extends JpaRepository<WeiboPost, String> {

    List<WeiboPost> findTop20ByOrderByCreatedAtDesc();

    List<WeiboPost> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

    long count();
}
