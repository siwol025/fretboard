package com.fretboard.fretboard.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountSyncSchedulerTest {

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ViewCountSyncScheduler viewCountSyncScheduler;

    @Test
    @DisplayName("syncViewCountsToDatabase — Redis 조회수를 batchUpdate로 일괄 반영하고 읽은 키만 삭제한다")
    void syncViewCountsToDatabase_batchUpdate로_일괄반영하고_읽은키만_삭제한다() {
        given(viewCountService.getAllViewCounts())
                .willReturn(Map.of("1", "100", "2", "50"));

        viewCountSyncScheduler.syncViewCountsToDatabase();

        verify(jdbcTemplate).batchUpdate(eq("UPDATE post SET view_count = ? WHERE id = ?"), anyList());
        verify(viewCountService).deleteViewCountKeys(Set.of("1", "2"));
    }

    @Test
    @DisplayName("syncViewCountsToDatabase — 조회수가 없으면 batchUpdate를 호출하지 않는다")
    void syncViewCountsToDatabase_조회수없으면_batchUpdate_미호출() {
        given(viewCountService.getAllViewCounts()).willReturn(Map.of());

        viewCountSyncScheduler.syncViewCountsToDatabase();

        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
    }
}
