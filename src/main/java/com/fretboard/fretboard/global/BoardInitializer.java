package com.fretboard.fretboard.global;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.board.repository.PostBoardRepository;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostBoardRepository postBoardRepository;
    private final CommentRepository commentRepository;

    @Override
    public void run(String... args) {
        if (boardRepository.count() == 0) {
            boardRepository.save(Board.builder()
                    .title("자유게시판")
                    .build());
            boardRepository.save(Board.builder()
                    .title("공지사항")
                    .build());
            boardRepository.save(Board.builder()
                    .title("개념글")
                    .build());
        }
        Board board = boardRepository.findById(1L)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        if (postRepository. count() == 0) {
            for (int i = 1; i <= 1000; i++) {
                Post post = Post.builder()
                        .title("테스트 게시글 " + i)
                        .content("이것은 테스트 게시글 내용입니다. 번호: " + i)
                        .build();

                Post savedPost = postRepository.save(post);
                for (int j = 1; j <= 100; j++) {
                    Comment comment = Comment.parent("테스트 댓글", savedPost);
                    savedPost.addComment(comment);
                    commentRepository.save(comment);
                }

                // PostBoard 연관관계 설정
                PostBoard postBoard = PostBoard.of(savedPost, board);
                savedPost.addPostBoard(postBoard);


                postBoardRepository.save(postBoard);
            }
        }
    }
}
