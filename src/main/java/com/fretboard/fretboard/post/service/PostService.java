package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.board.repository.PostBoardRepository;
import com.fretboard.fretboard.board.service.BoardService;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.image.infrastructure.AwsS3Provider;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.repository.MemberRepository;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.EditPostRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
import com.fretboard.fretboard.post.dto.request.NewPostRequest;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final PostBoardRepository postBoardRepository;
    private final BoardService boardService;
    private final MemberRepository memberRepository;
    private final AwsS3Provider awsS3Provider;

    @Transactional
    public Long addPost(final NewPostRequest request, final MemberAuth memberAuth) {
        Member member = getMember(memberAuth);

        Post post = request.toPost(member);
        post.setContent(convertTempImageUrlsToPermanent(post.getContent()));

        Post savedPost = postRepository.save(post);
        boardService.savePostBoard(savedPost, request.boardId());
        return savedPost.getId();
    }

    @Transactional
    public void updatePost(final Long id, final EditPostRequest request, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        Member loginMember = getMember(memberAuth);
        validateIsAuthor(post.getMember(), loginMember);

        post.setTitle(request.title());
        post.setContent(convertTempImageUrlsToPermanent(request.content()));
    }

    @Transactional
    public void deletePost(final Long id, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        Member loginMember = getMember(memberAuth);
        validateIsAuthor(post.getMember(), loginMember);

        postRepository.delete(post);
    }

    public PostDetailResponse findPost(final Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostDetailResponse.of(post);
    }

    public PostListResponse findPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<PostBoard> postBoardPage = postBoardRepository.findPostBoardsByBoardId(boardId, pageable);
        return PostListResponse.of(postBoardPage);
    }

    public List<PostSummaryResponse> findPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.stream()
                .map(PostSummaryResponse::of)
                .toList();
    }

    private Member getMember(final MemberAuth memberAuth) {
        return memberRepository.findById(memberAuth.memberId())
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
    }

    private String convertTempImageUrlsToPermanent(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Elements images = doc.select("img");

        for (Element img : images) {
            String imgUrl = img.attr("src");
            String permanentUrl = awsS3Provider.copyImageToPermanentStorage(imgUrl);
            img.attr("src", permanentUrl);
        }

        return doc.body().html();
    }

    private void validateIsAuthor(Member author, Member loginMember) {
        if (!author.equals(loginMember)) {
            throw new FretBoardException(ExceptionType.NOT_AUTHOR);
        }
    }
}
