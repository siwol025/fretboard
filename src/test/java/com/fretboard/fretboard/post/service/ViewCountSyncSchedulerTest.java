package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountSyncSchedulerTest {

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private ViewCountSyncScheduler viewCountSyncScheduler;

    @Test
    @DisplayName("syncViewCountsToDatabase — Redis 조회수를 DB에 일괄 동기화하고 Redis를 초기화한다")
    void syncViewCountsToDatabase_Redis조회수를_DB에_일괄동기화하고_초기화한다() {
        given(viewCountService.getAllViewCounts())
                .willReturn(Map.of("1", "100", "2", "50"));

        viewCountSyncScheduler.syncViewCountsToDatabase();

        verify(postRepository).updateViewCount(1L, 100L);
        verify(postRepository).updateViewCount(2L, 50L);
        verify(viewCountService).clearAllViewCounts();
    }
}
