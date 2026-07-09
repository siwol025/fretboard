package com.fretboard.fretboard.global;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;

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
            boardRepository.save(Board.builder()
                        .title("기술 게시판")
                        .description("개발 및 IT 기술에 관한 이야기")
                        .slug("tech")
                        .boardType(BoardType.WRITABLE)
                        .build());
            boardRepository.save(Board.builder()
                        .title("디자인 게시판")
                        .description("UI/UX 및 그래픽 디자인 토론")
                        .slug("design")
                        .boardType(BoardType.WRITABLE)
                        .build());
            boardRepository.save(Board.builder()
                        .title("커리어 게시판")
                        .description("취업, 이직 및 커리어 개발")
                        .slug("career")
                        .boardType(BoardType.WRITABLE)
                        .build());
        }
    }
}
