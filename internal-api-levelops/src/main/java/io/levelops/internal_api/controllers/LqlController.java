package io.levelops.internal_api.controllers;

import io.levelops.internal_api.models.LqlValidateResponse;
import io.levelops.internal_api.requests.LqlValidateRequest;
import io.levelops.lql.LQL;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;
import io.levelops.web.util.SpringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/internal/v1/lql")
public class LqlController {

    @PostMapping("/validate")
    public DeferredResult<ResponseEntity<Map<String, Object>>> validate(@RequestBody LqlValidateRequest validateRequest) {
        return SpringUtils.deferResponse(() -> {
            Map<String, Object> response = new ConcurrentHashMap<>();
            AtomicBoolean errors = new AtomicBoolean(false);
            if (validateRequest.getLqls() != null) {
                validateRequest.getLqls().parallelStream().forEach(lql -> {
                    try {
                        LqlAst ast = LQL.parse(lql);
                        response.put(lql, LqlValidateResponse.builder()
                                .lql(ast.toInlineString())
                                .ast(ast)
                                .build());
                    } catch (LqlException e) {
                        response.put(lql, Map.of("error", e.getMessage()));
                        errors.set(true);
                    }
                });
            }
            if (!errors.get()) {
                return ResponseEntity.ok().body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        });
    }

}
