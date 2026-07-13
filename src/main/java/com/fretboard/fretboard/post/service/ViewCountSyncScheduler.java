package com.fretboard.fretboard.post.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private static final String UPDATE_VIEW_COUNT_SQL = "UPDATE post SET view_count = ? WHERE id = ?";

    private final ViewCountService viewCountService;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncViewCountsToDatabase() {
        Map<Object, Object> counts = viewCountService.getAllViewCounts();
        if (counts.isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = counts.entrySet().stream()
                .map(entry -> new Object[]{
                        Long.parseLong(entry.getValue().toString()),
                        Long.parseLong(entry.getKey().toString())
                })
                .toList();
        jdbcTemplate.batchUpdate(UPDATE_VIEW_COUNT_SQL, batchArgs);

        Set<String> syncedKeys = counts.keySet().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        viewCountService.deleteViewCountKeys(syncedKeys);
    }
}
