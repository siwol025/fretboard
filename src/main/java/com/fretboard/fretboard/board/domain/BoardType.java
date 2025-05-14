package com.fretboard.fretboard.board.domain;

import com.fretboard.fretboard.member.domain.Member;
import java.util.function.Predicate;

public enum BoardType {
    WRITABLE(member -> true),
    NON_WRITABLE(Member::isAdmin);

    private final Predicate<Member> writePolicy;

    BoardType(Predicate<Member> writePolicy) {
        this.writePolicy = writePolicy;
    }

    public boolean canWrite(Member member) {
        return writePolicy.test(member);
    }
}
