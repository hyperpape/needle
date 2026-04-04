package com.justinblank.strings.RegexAST;

import com.justinblank.strings.CharRange;
import com.justinblank.strings.Factorization;

import java.util.Optional;

public class CharRangeNode extends Node implements Comparable<CharRangeNode> {

    private final CharRange range;

    public CharRange range() {
        return range;
    }

    public CharRangeNode(CharRange range ) {
        this.range = range;
    }

    public CharRangeNode(char start, char end) {
        this(CharRange.of(start, end));
    }

    public int minLength() {
        return 1;
    }

    public Optional<Integer> maxLength() {
        return Optional.of(1);
    }

    @Override
    protected int height() {
        return 0;
    }

    public Factorization bestFactors() {
        return Factorization.fromRange(this.range.getStart(), this.range.getEnd());
    }

    @Override
    public int compareTo(CharRangeNode o) {
        return range.compareTo(o.range);
    }

    public Node reversed() {
        return this;
    }

    @Override
    public boolean nonAscii() {
        if (range.getEnd() <= 127) {
            return false;
        }
        else if (range.getEnd() == '\uFFFF' && range.getStart() < 127) {
            return false;
        }
        return true;
    }

    @Override
    public Node toUTF16Bytes() {
        var start = range.getStart();
        var end = range.getEnd();
        var startLow = start & 0x00FF;
        var startHigh = (start & 0xFF00) >> 8;
        var endLow = end & 0x00FF;
        var endHigh = (end & 0xFF00) >> 8;

        var low = startLow;
        var high = startHigh;

        if (startHigh == endHigh) {
            return singleCharRangeUTF(high, startLow, endLow);
        }
        else {
            Node result = singleCharRangeUTF(startHigh, startLow, 255);
            if (startHigh + 1 < endHigh) {
                Node middle = Concatenation.concatenate(new CharRangeNode((char) (startHigh + 1), (char) (endHigh - 1)), new CharRangeNode((char) 0, (char) 255));
                result = Union.of(result, middle, false);
            }
            result = Union.of(result, singleCharRangeUTF((char) endHigh, (char) 0, (char) endLow), false);
            return result;
        }
    }

    private static Node singleCharRangeUTF(int high, int startLow, int endLow) {
        return Concatenation.concatenate(LiteralNode.fromChar((char) high), new CharRangeNode((char) startLow, (char) endLow));
    }

    @Override
    public String toString() {
        return "CharRangeNode{" +
                "range=" + range +
                '}';
    }
}
