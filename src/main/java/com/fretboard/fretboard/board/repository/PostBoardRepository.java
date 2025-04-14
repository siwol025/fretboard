package com.fretboard.fretboard.board.repository;

import com.fretboard.fretboard.board.domain.PostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBoardRepository extends JpaRepository<PostBoard, Long> {
}
