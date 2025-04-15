package com.fretboard.fretboard.global;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.board.repository.PostBoardRepository;
import com.fretboard.fretboard.exception.ExceptionType;
import com.fretboard.fretboard.exception.FretBoardException;
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

        for (int i = 1; i <= 100; i++) {
            Post post = Post.builder()
                    .title("테스트 게시글 " + i)
                    .content("이것은 테스트 게시글 내용입니다. 번호: " + i)
                    .build();

            Post savedPost = postRepository.save(post);

            // PostBoard 연관관계 설정
            PostBoard postBoard = PostBoard.of(savedPost, board);
            savedPost.addPostBoard(postBoard);

            postBoardRepository.save(postBoard);
        }
    }
}
