package com.fretboard.fretboard.global;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Override
    public void run(String... args) {
        if (boardRepository.count() == 0) {
            boardRepository.save(Board.builder()
                            .title("자유게시판")
                            .description("자유로운 주제의 대화")
                            .slug("free")
                            .boardType(BoardType.WRITABLE)
                            .build());
            boardRepository.save(Board.builder()
                            .title("공지사항")
                            .description("중요 공지 및 업데이트")
                            .slug("notice")
                            .boardType(BoardType.NON_WRITABLE)
                            .build());
            boardRepository.save(Board.builder()
                            .title("질문 & 답변")
                            .description("궁금한 점을 물어보세요")
                            .slug("qna")
                            .boardType(BoardType.WRITABLE)
                            .build());
        }
        Board board = boardRepository.findById(3L)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));

        if (memberRepository.count() ==0) {
            memberRepository.save(Member.builder()
                            .nickname("김개발")
                            .password("xcvoqbrosdvkzxcvnaelwelfajxzcvsdjo")
                            .username("xcvoqbrosdvkzxcvnaelwelfajxzcvsdjo")
                            .role(Role.USER)
                            .build());
            memberRepository.save(Member.builder()
                            .nickname("이디자인")
                            .password("zxbnoaseojrbjkgaelkzsdlkewkbfavsdlf")
                            .username("zxbnoaseojrbjkgaelkzsdlkewkbfavsdlf")
                            .role(Role.USER)
                            .build());
            memberRepository.save(Member.builder()
                            .nickname("박프론트")
                            .password("asdlflnbzxpwelnkwteqpzxcbovdsalnkqwelkhkhls")
                            .username("asdlflnbzxpwelnkwteqpzxcbovdsalnkqwelkhkhls")
                            .role(Role.USER)
                            .build());
        }
        Member member = memberRepository.findById(3L)
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
        if (postRepository. count() == 0) {
            for (int i = 1; i <= 1000; i++) {
                Post post = Post.builder()
                        .title("테스트 게시글 " + i)
                        .content("이것은 테스트 게시글 내용입니다. 번호: " + i)
                        .member(member)
                        .build();
                post.setBoard(board);
                Post savedPost = postRepository.save(post);
                for (int j = 1; j <= 100; j++) {
                    Comment comment = Comment.parent("테스트 댓글", member, savedPost);
                    savedPost.addComment(comment);
                    commentRepository.save(comment);
                }

            }
        }
    }
}
