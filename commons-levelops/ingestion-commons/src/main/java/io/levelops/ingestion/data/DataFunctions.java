package io.levelops.ingestion.data;

import java.util.function.Function;

public class DataFunctions {

    public static <T, U> Function<Data<T>, Data<U>> basicDataTransform(Class<U> outputDataClass, Function<T, U> dataTransform) {
        return dataT -> BasicData.of(outputDataClass, dataTransform.apply(dataT.getPayload()));
    }

}
