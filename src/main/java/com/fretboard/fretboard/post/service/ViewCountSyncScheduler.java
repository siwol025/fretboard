package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final ViewCountService viewCountService;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncViewCountsToDatabase() {
        Map<Object, Object> counts = viewCountService.getAllViewCounts();

        for (Map.Entry<Object, Object> entry : counts.entrySet()) {
            Long postId = Long.parseLong(entry.getKey().toString());
            Long viewCount = Long.parseLong(entry.getValue().toString());
            postRepository.updateViewCount(postId, viewCount);
        }

        viewCountService.clearAllViewCounts();
    }
}
