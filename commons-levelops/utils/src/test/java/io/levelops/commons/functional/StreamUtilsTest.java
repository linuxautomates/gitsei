package io.levelops.commons.functional;

import com.google.common.collect.Iterators;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamUtilsTest {

    @Test
    public void forEachPage() {
        StreamUtils.forEachPage(IntStream.range(0, 100).boxed(), 10, page -> {
            System.out.println(page);
            assertThat(page).hasSize(10);
        });

        StreamUtils.forEachPage(IntStream.range(0, 4).boxed(), 100, page -> {
            System.out.println(page);
            assertThat(page).hasSize(4);
        });
    }

    @Test
    public void forEachPageTakingWhile() {
        // inclusive
        List<List<Integer>> pages = new ArrayList<>();
        StreamUtils.forEachPageTakingWhile(IntStream.range(0, 100).boxed(), 10, page -> {
            pages.add(page);
            System.out.println(page);
        }, i -> i < 42, true);
        assertThat(pages).hasSize(5);
        assertThat(pages.get(4)).containsExactly(40, 41, 42);

        // exclusive
        List<List<Integer>> pages3 = new ArrayList<>();
        StreamUtils.forEachPageTakingWhile(IntStream.range(0, 100).boxed(), 10, page -> {
            pages3.add(page);
            System.out.println(page);
        }, i -> i < 42, false);
        assertThat(pages3).hasSize(5);
        assertThat(pages3.get(4)).containsExactly(40, 41);

        StreamUtils.forEachPageTakingWhile(IntStream.range(0, 4).boxed(), 100, page -> {
            System.out.println(page);
            assertThat(page).hasSize(4);
        }, i -> i < 42, true);
    }

    @Test
    public void partition() {
        List<List<Integer>> partitions = StreamUtils.partition(Stream.of(1, 2, 3, 4, 5), 2)
                .collect(Collectors.toList());
        assertThat(partitions).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
    }

    @Test
    public void partitionReadCount() {
        MutableInt readCount = new MutableInt(0);
        Stream<Integer> longStream = Stream.of(1, 2, 3)
                .flatMap(a -> Stream.of(a * 10 + 1, a * 10 + 2, a * 10 + 3))
                .flatMap(b -> Stream.of(b * 10 + 1, b * 10 + 2, b * 10 + 3))
                .peek(x -> readCount.increment())
                .peek(System.out::println);

        StreamUtils.partition(longStream, 4).forEach(x -> {
            System.out.println(">>> " + x + " readCount=" + readCount.intValue());
            assertThat(readCount.intValue()).isIn(3, 4); // last batch is 3
            readCount.setValue(0); // reset
        } );
    }

    @Test
    public void partitionReadCountUsingIterator() {
        MutableInt readCount = new MutableInt(0);
        Stream<Integer> longStream = Stream.of(1, 2, 3)
                .flatMap(a -> Stream.of(a * 10 + 1, a * 10 + 2, a * 10 + 3))
                .flatMap(b -> Stream.of(b * 10 + 1, b * 10 + 2, b * 10 + 3))
                .peek(x -> readCount.increment())
                .peek(System.out::println);

        Iterators.partition(longStream.iterator(), 4).forEachRemaining(x -> {
            System.out.println(">>> " + x + " readCount=" + readCount.intValue());
            assertThat(readCount.intValue()).isNotEqualTo(4); // counter-intuitive!
            readCount.setValue(0); // reset
        } );
    }
}