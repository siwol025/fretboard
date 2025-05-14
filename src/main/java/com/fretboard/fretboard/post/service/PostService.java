package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.repository.MemberRepository;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.EditPostRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.request.NewPostRequest;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final BoardRepository boardRepository;

    @Transactional
    public Long addPost(final NewPostRequest request, final MemberAuth memberAuth) {
        Member loginMember = getMember(memberAuth);
        Board board = getBoard(request.boardId());
        board.validateWritable(loginMember);
        Post post = request.toPost(loginMember, board);

        String convertedContent = imageService.convertTempImageUrlsToPermanent(post.getContent());
        post.setContent(convertedContent);

        Post savedPost = postRepository.save(post);
        return savedPost.getId();
    }

    @Transactional
    public void updatePost(final Long id, final EditPostRequest request, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        validateIsAuthor(post.getMember(), getMember(memberAuth));

        imageService.cleanUpRemovedImages(post.getContent(), request.content());
        String convertedContent = imageService.convertTempImageUrlsToPermanent(request.content());

        post.setTitle(request.title());
        post.setContent(convertedContent);
    }

    @Transactional
    public void deletePost(final Long id, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        validateIsAuthor(post.getMember(), getMember(memberAuth));
        imageService.deleteImage(post.getContent());

        postRepository.delete(post);
    }

    public PostDetailResponse findPost(final Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostDetailResponse.of(post);
    }

    public PostListResponse findPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<Post> postBoardPage = postRepository.findByBoardId(boardId,pageable);
        return PostListResponse.of(postBoardPage);
    }

    private Member getMember(final MemberAuth memberAuth) {
        return memberRepository.findById(memberAuth.memberId())
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
    }

    private Board getBoard(final Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
    }

    private void validateIsAuthor(Member author, Member loginMember) {
        if (!author.equals(loginMember)) {
            throw new FretBoardException(ExceptionType.NOT_AUTHOR);
        }
    }

    public List<RecentPostsPerBoardResponse> findRecentPostsPerBoard() {
        List<Post> recentPostsPerBoards = postRepository.findRecentPostsPerBoards();
        return RecentPostsPerBoardResponse.of(recentPostsPerBoards);
    }
}
