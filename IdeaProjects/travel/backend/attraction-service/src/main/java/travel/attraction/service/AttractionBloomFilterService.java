package travel.attraction.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.mapper.travel_recommendation_mapper.AttractionMapper;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttractionBloomFilterService {

    private final AttractionMapper attractionMapper;

    @Value("${attraction.bloom.expected-insertions:10000}")
    private long expectedInsertions;

    @Value("${attraction.bloom.false-positive-rate:0.01}")
    private double falsePositiveRate;

    private final AtomicReference<BloomFilter<Integer>> bloomFilterRef =
            new AtomicReference<>(BloomFilter.create(Funnels.integerFunnel(), 10000, 0.01));

    @PostConstruct
    public void initialize() {
        refreshBloomFilter();
    }

    @Scheduled(cron = "${attraction.bloom.refresh-cron:0 0/30 * * * ?}")
    public void refreshBloomFilter() {
        List<Integer> attractionIds = attractionMapper.selectList(null)
                .stream()
                .map(Attraction::getId)
                .filter(id -> id != null && id > 0)
                .toList();

        BloomFilter<Integer> newFilter = BloomFilter.create(
                Funnels.integerFunnel(),
                Math.max(expectedInsertions, attractionIds.size() * 2L + 1),
                falsePositiveRate
        );

        attractionIds.forEach(newFilter::put);
        bloomFilterRef.set(newFilter);
        log.info("Refreshed attraction bloom filter, loaded {} ids", attractionIds.size());
    }

    public boolean mightContain(Integer attractionId) {
        return attractionId != null && attractionId > 0 && bloomFilterRef.get().mightContain(attractionId);
    }

    public void put(Integer attractionId) {
        if (attractionId != null && attractionId > 0) {
            bloomFilterRef.get().put(attractionId);
        }
    }
}
