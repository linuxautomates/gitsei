package io.levelops.normalization.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.normalization.Normalizer;
import io.levelops.normalization.exceptions.NormalizationException;
import io.levelops.commons.models.ContentType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class NormalizationService {

    private static final String SCAN_PACKAGE = "io.levelops";
    private final ObjectMapper objectMapper;

    private final Map<ContentType, Method> normalizers;

    public NormalizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        normalizers = discover();
    }

    private static Map<ContentType, Method> discover() {
        log.info("Scanning for @Normalizer methods...");
        MutableInt scanned = new MutableInt(0);
        var map = new Reflections(SCAN_PACKAGE, new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Normalizer.class).stream()
                .peek(m -> scanned.increment())
                .filter(NormalizationService::isSignatureValid)
                .collect(Collectors.toMap(
                        m -> {
                            Normalizer annotation = m.getAnnotation(Normalizer.class);
                            return ContentType.fromString(annotation.contentType());
                        },
                        m -> m));
        log.info("Found {} @Normalizer method(s) (scanned={})", map.size(), scanned.intValue());
        return map;
    }

    private static boolean isSignatureValid(Method m) {
        if (!m.isAnnotationPresent(Normalizer.class)) {
            return false;
        }
        if (!Modifier.isStatic(m.getModifiers())) {
            log.warn("Ignoring non-static method '{}' annotated with @Normalizer in {}: {}", m.getName(), m.getDeclaringClass().getSimpleName(), m);
            return false;
        }
        Normalizer annotation = m.getAnnotation(Normalizer.class);
        boolean isSignatureValid = m.getParameterCount() == 2
                && m.getParameters()[0].getType().equals(ObjectMapper.class);
        if (!isSignatureValid) {
            log.warn("Ignoring method annotated with @Normalizer with wrong signature in {}. Was expecting: ? {}(ObjectMapper, ?); but got: {}", m.getDeclaringClass().getSimpleName(), m.getName(), m);
        }
        return isSignatureValid;
    }

    public boolean canBeNormalized(ContentType contentType) {
        return normalizers.containsKey(contentType);
    }

    @SuppressWarnings("unchecked")
    public <T> T normalize(ContentType contentType, JsonNode object) throws NormalizationException {
        Method method = normalizers.get(contentType);
        if (method == null) {
            throw new NormalizationException("Cannot normalize contentType=" + contentType);
        }

        Normalizer annotation = method.getAnnotation(Normalizer.class);
        Class<?> inputClass = method.getParameters()[1].getType();
        Class<?> outputClass = method.getReturnType();
        try {
            Object output = method.invoke(null, objectMapper, objectMapper.convertValue(object, inputClass));
            //noinspection unchecked
            return (T) outputClass.cast(output);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new NormalizationException(e);
        }
    }

    public JsonNode normalizeToJson(ContentType contentType, JsonNode input) throws NormalizationException {
        Object object = normalize(contentType, input);
        return objectMapper.convertValue(object, JsonNode.class);
    }

}
