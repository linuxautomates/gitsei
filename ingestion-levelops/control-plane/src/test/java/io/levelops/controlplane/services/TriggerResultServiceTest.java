package io.levelops.controlplane.services;

import io.levelops.controlplane.models.DbTriggeredJob;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Test;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TriggerResultServiceTest {

    @Test
    public void name() {
        int page = 8;
        int pageSize = 11;
        long skip = (long) page * pageSize;
        MutableLong count = new MutableLong(0);
        IntStream.range(0, 100)
                .skip(skip)
                .limit(pageSize + 1)
                .peek(x -> System.out.println("currentCount: " + count.longValue()))
                .filter(x -> count.getAndIncrement() < pageSize)
                .peek(i -> System.out.println(">>> premap: " + i))
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        return i;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .forEach(System.out::println);
        boolean hasNext = (count.longValue() > pageSize);
        System.out.println("count: " + count);
        System.out.println("hasNext: " + hasNext);
        assertThat(hasNext).isTrue();
    }
}