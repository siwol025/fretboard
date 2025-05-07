package com.fretboard.fretboard.board.repository;

import com.fretboard.fretboard.board.domain.PostBoard;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBoardRepository extends JpaRepository<PostBoard, Long> {
    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT pb FROM PostBoard pb WHERE pb.board.id = :boardId")
    Page<PostBoard> findPostBoardsByBoardId(Long boardId, Pageable pageable);

    @Query(
            value = """
                    SELECT pb.*
                    FROM (
                        SELECT pb.*, ROW_NUMBER() OVER (PARTITION BY board_id ORDER BY p.created_at DESC) AS rn
                        FROM post_board pb
                        JOIN post p ON pb.post_id = p.id
                    ) pb
                    WHERE pb.rn <= 5
                    ORDER BY pb.board_id, pb.rn
                    """,
            nativeQuery = true
    )
    List<PostBoard> findRecentPostsPerBoards();
}
