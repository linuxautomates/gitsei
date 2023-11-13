package io.levelops.lql.eval;

import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;

public interface LqlTermEvaluator {

    boolean accepts(LqlAst node);

    Boolean evaluate(LqlAst node) throws LqlException;

}
