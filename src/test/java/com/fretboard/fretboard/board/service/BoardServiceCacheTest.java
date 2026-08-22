package com.fretboard.fretboard.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Task 2 (게시판 캐시) 동작 테스트.
 *
 * 순수 Mockito 단위 테스트({@link BoardServiceTest})로는 @Cacheable/@CacheEvict 프록시가 동작하지 않으므로,
 * 캐싱을 활성화한 최소 Spring 컨텍스트에서 캐시어사이드 의미(히트/미스, 무효화)를 검증한다.
 *
 * <p>가정(GREEN 정렬 필요):
 * <ul>
 *   <li>테스트 CacheManager 빈 이름은 프로덕션 규칙과 맞추기 위해 "postCacheManager"로 등록한다
 *       (PostFeedService.getRecentPosts가 cacheManager = "postCacheManager"를 명시하므로,
 *        BoardService @Cacheable도 동일 cacheManager를 사용할 것으로 가정).</li>
 *   <li>ConcurrentMapCacheManager를 사용하며 "boards"/"board" 캐시를 사전 등록한다(동적 생성에 의존하지 않음).</li>
 *   <li>Redis 없이 동작한다.</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        BoardService.class,
        BoardServiceCacheTest.CacheTestConfig.class
})
class BoardServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager postCacheManager() {
            return new ConcurrentMapCacheManager(CacheKey.BOARDS, CacheKey.BOARD);
        }
    }

    @Autowired
    private BoardService boardService;

    @MockBean
    private BoardRepository boardRepository;

    @MockBean
    private AuthorizationHelper authorizationHelper;

    @Autowired
    private CacheManager cacheManager;

    private final MemberAuth adminAuth = new MemberAuth(1L);

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    private void givenAdmin() {
        Member admin = Member.builder()
                .username("admin")
                .password("password")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();
        given(authorizationHelper.getMember(adminAuth)).willReturn(admin);
    }

    private Board board(String title) {
        return Board.builder()
                .title(title)
                .description("설명")
                .slug("slug")
                .boardType(BoardType.WRITABLE)
                .build();
    }

    @Test
    @DisplayName("게시판 목록 두 번째 조회는 DB에 접근하지 않는다")
    void 게시판목록_두번째조회는_DB미접근() {
        given(boardRepository.findAll()).willReturn(List.of(board("자유게시판")));

        boardService.findBoards();
        boardService.findBoards();

        verify(boardRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("게시판 단건 두 번째 조회는 DB에 접근하지 않는다")
    void 게시판단건_두번째조회는_DB미접근() {
        given(boardRepository.findById(1L)).willReturn(Optional.of(board("자유게시판")));

        boardService.findBoard(1L);
        boardService.findBoard(1L);

        verify(boardRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("게시판 생성 후 목록 캐시가 무효화된다")
    void 게시판생성_후_목록캐시_무효화() {
        givenAdmin();
        given(boardRepository.findAll()).willReturn(List.of(board("자유게시판")));
        given(boardRepository.save(any(Board.class))).willReturn(board("새 게시판"));

        boardService.findBoards();
        boardService.createBoard(
                new BoardRequest("새 게시판", "설명", "new-board", BoardType.WRITABLE), adminAuth);
        boardService.findBoards();

        verify(boardRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("게시판 수정 후 목록과 단건 캐시가 모두 무효화된다")
    void 게시판수정_후_목록과_단건캐시_무효화() {
        givenAdmin();
        given(boardRepository.findAll()).willReturn(List.of(board("자유게시판")));
        given(boardRepository.findById(1L)).willReturn(Optional.of(board("자유게시판")));

        // 두 캐시를 모두 워밍한다.
        boardService.findBoards();
        BoardElementResponse before = boardService.findBoard(1L);
        assertThat(before.title()).isEqualTo("자유게시판");

        // 수정 후에는 DB가 수정된 값을 반환하도록 스텁을 갱신한다.
        boardService.editBoard(1L,
                new BoardRequest("수정된 게시판", "설명", "edited-board", BoardType.WRITABLE), adminAuth);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board("수정된 게시판")));

        // 목록 캐시 무효화: 다음 findBoards()는 DB(findAll)를 다시 조회한다.
        boardService.findBoards();
        verify(boardRepository, times(2)).findAll();

        // 단건 캐시 무효화: 다음 findBoard(1L)는 갱신된 값을 반환한다(캐시 스테일 아님).
        BoardElementResponse after = boardService.findBoard(1L);
        assertThat(after.title()).isEqualTo("수정된 게시판");
    }

    @Test
    @DisplayName("게시판 삭제 후 목록과 단건 캐시가 모두 무효화된다")
    void 게시판삭제_후_목록과_단건캐시_무효화() {
        givenAdmin();
        Board existing = board("자유게시판");
        given(boardRepository.findAll()).willReturn(List.of(existing));
        given(boardRepository.findById(1L)).willReturn(Optional.of(existing));

        // 두 캐시를 모두 워밍한다.
        boardService.findBoards();
        boardService.findBoard(1L);
        assertThat(cacheManager.getCache(CacheKey.BOARD).get(1L)).isNotNull();

        boardService.deleteBoard(1L, adminAuth);
        verify(boardRepository).delete(eq(existing));

        // 단건 캐시 무효화: board(#id) 엔트리가 제거되었다.
        assertThat(cacheManager.getCache(CacheKey.BOARD).get(1L)).isNull();

        // 목록 캐시 무효화: 다음 findBoards()는 DB(findAll)를 다시 조회한다.
        boardService.findBoards();
        verify(boardRepository, times(2)).findAll();
    }
}
