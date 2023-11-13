package io.levelops.controlplane.trigger.strategies;

public interface CursorStrategy<C extends CursorStrategy.Cursor, M extends CursorStrategy.CursorMetadata> {

    interface Cursor {
    }

    interface CursorMetadata {
    }

    C getNextCursor(M currentCursorMetadata);

}
