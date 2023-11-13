package io.levelops.lql.filter;

import com.fasterxml.jackson.databind.JsonNode;
import io.levelops.commons.models.ContentType;
import io.levelops.lql.LQL;
import io.levelops.lql.eval.LqlTermEvaluator;
import io.levelops.lql.exceptions.LqlException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public interface LqlFilter {

    /**
     * Returns content type on which this filter can be applied.
     */
    ContentType getContentType();

    /**
     * Returns list of evaluators based on given data.
     * (i.e this method should make the right variable assignments based on the given data)
     */
    List<LqlTermEvaluator> buildEvaluators(String lql, JsonNode data);

    default boolean eval(String lql, JsonNode data) {
        try {
            return LQL.eval(lql, buildEvaluators(lql, data));
        } catch (LqlException e) {
            Logger log = LogManager.getLogger(LqlFilter.class);
            log.warn("Failed to evaluate lql='{}' on contentType={}", lql, getContentType(), e);
            return false;
        }
    }

}
