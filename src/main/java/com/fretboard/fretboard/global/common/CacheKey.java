package com.fretboard.fretboard.global.common;

public class CacheKey {
    // 캐시 매니저 빈 이름. RedisCacheConfig#postCacheManager 빈 이름과 반드시 일치해야 한다.
    public static final String CACHE_MANAGER = "postCacheManager";

    // 기존: 최근 게시글
    public static final String RECENT_POSTS = "recentPosts";
    public static final String RECENT_POSTS_KEY = "'all'";
    public static final long RECENT_POSTS_TTL = 1L;

    // 게시판
    public static final String BOARDS = "boards";
    public static final String BOARDS_KEY = "'all'";
    public static final String BOARD = "board";
    public static final String BOARD_KEY = "#id";
    public static final long BOARDS_TTL_MINUTES = 60L;

    // 게시글 상세 정적 본문
    public static final String POST_DETAIL = "postDetail";
    public static final String POST_DETAIL_KEY = "#postId";
    public static final long POST_DETAIL_TTL_MINUTES = 10L;

    // 댓글
    public static final String COMMENTS = "comments";
    public static final String COMMENTS_KEY = "#postId";
    public static final long COMMENTS_TTL_MINUTES = 10L;

    // 인기글
    public static final String POPULAR_TODAY = "popularToday";
    public static final String POPULAR_TODAY_KEY = "'all'";
    public static final String POPULAR_BY_BOARD = "popularByBoard";
    public static final String POPULAR_BY_BOARD_KEY = "#boardId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize";
    public static final long POPULAR_TTL_SECONDS = 60L;
}
