package io.levelops.integrations.tenable.sources;

import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

class ChunkPageSupplier implements Supplier<ChunkPageData> {
    private final Function<ChunkPageData, ChunkPageData> pageSupplier;
    private ChunkPageData chunkPageData;

    public static Predicate<ChunkPageData> predicate = chunkPageData -> !chunkPageData.done;

    public ChunkPageSupplier(ChunkPageData chunkPageData, Function<ChunkPageData, ChunkPageData> pageSupplier) {
        this.chunkPageData = chunkPageData;
        this.pageSupplier = pageSupplier;
    }

    @Override
    public ChunkPageData get() {
        chunkPageData = pageSupplier.apply(chunkPageData);
        HashSet<Integer> chunksAvailableSet = new HashSet<>(chunkPageData.getChunksAvailable());
        HashSet<Integer> chunksProcessedSet = new HashSet<>(chunkPageData.getChunksProcessed());
        chunksAvailableSet.removeAll(chunksProcessedSet);
        if (chunkPageData.getStatus().equalsIgnoreCase(ChunkPageData.FINISHED) && chunksAvailableSet.size() == 0) {
            chunkPageData.done = true;
        }
        return chunkPageData;
    }
}