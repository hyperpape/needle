package com.justinblank.strings;

import java.util.LinkedList;
import java.util.List;

class RangeGroup implements Comparable<RangeGroup> {
    final List<CharRange> ranges = new LinkedList<>();

    public static int maxChar(List<RangeGroup> rangeGroups) {
        int maxChar = 0;
        for (RangeGroup rangeGroup : rangeGroups) {
            for (var range : rangeGroup.ranges) {
                if (range.getEnd() > maxChar) {
                    var end = range.getEnd();
                    if (end == Character.MAX_VALUE && range.getStart() <= 127) {
                        end = 127;
                    }
                    maxChar = end;
                }
            }
        }
        return maxChar;
    }

    @Override
    public int compareTo(RangeGroup o) {
        if (this == o) {
            return 0;
        }
        for (var i = 0; i < ranges.size(); i++) {
            if (i >= o.ranges.size()) {
                return 1;
            }
            int comp = ranges.get(i).compareTo(o.ranges.get(i));
            if (comp != 0) {
                return comp;
            }
        }
        if (ranges.size() < o.ranges.size()) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "RangeGroup{" +
                "ranges=" + ranges +
                '}';
    }
}
