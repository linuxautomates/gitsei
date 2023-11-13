package io.levelops.etl.job_framework;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EtlProcessorRegistry {
    private final Map<String, EtlProcessor<?>> nameToAggProcessorMap;

    @Autowired
    public EtlProcessorRegistry(List<EtlProcessor<?>> EtlProcessors) {
        nameToAggProcessorMap = new HashMap<>();
        for (EtlProcessor<?> ETLProcessor : EtlProcessors) {
            nameToAggProcessorMap.put(ETLProcessor.getComponentClass(), ETLProcessor);
        }
    }

    public EtlProcessor<?> getAggProcessor(String componentClass) {
        if (nameToAggProcessorMap.containsKey(componentClass)) {
            return nameToAggProcessorMap.get(componentClass);
        }
        throw new NotImplementedException("ETL processor for type " + componentClass + " not implemented");
    }
}
