package yuuine.xxrag.vector.util;

import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.*;
import java.util.stream.Collectors;

public final class RrfFusion {

    private RrfFusion() {
    }

    public static <T> List<SearchHit<T>> fuse(
            List<SearchHit<T>> textHits,
            List<SearchHit<T>> vectorHits,
            int rrfK,
            double textWeight,
            double vectorWeight
    ) {
        Map<String, RrfEntry<T>> map = new HashMap<>();

        apply(map, textHits, rrfK, textWeight);
        apply(map, vectorHits, rrfK, vectorWeight);

        return map.values().stream()
                .sorted(Comparator.comparingDouble(RrfEntry<T>::score).reversed())
                .map(RrfEntry::hit)
                .collect(Collectors.toList());
    }

    private static <T> void apply(
            Map<String, RrfEntry<T>> map,
            List<SearchHit<T>> hits,
            int rrfK,
            double weight
    ) {
        for (int i = 0; i < hits.size(); i++) {
            SearchHit<T> hit = hits.get(i);
            int rank = i + 1;

            double score = weight / (rrfK + rank);

            map.computeIfAbsent(hit.getId(), id -> new RrfEntry<>(hit))
                    .add(score);
        }
    }

    private static class RrfEntry<T> {
        private final SearchHit<T> hit;
        private double score;

        RrfEntry(SearchHit<T> hit) {
            this.hit = hit;
        }

        void add(double s) {
            this.score += s;
        }

        double score() {
            return score;
        }

        SearchHit<T> hit() {
            return hit;
        }
    }
}
