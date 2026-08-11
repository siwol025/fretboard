package com.fretboard.fretboard.global.datasource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 무거운 집계·검색·리포팅 읽기를 표시한다. 이 애노테이션이 붙은 메서드 실행 동안
 * {@link WorkloadContextHolder}가 {@link ReadWorkload#ANALYTICS}로 세팅되어,
 * 해당 읽기가 SERVING(피드/상세) 경로와 분리된 analytics 노드로 라우팅된다.
 * stale이 허용되므로 read-your-writes 마킹은 건너뛴다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Analytics {
}
