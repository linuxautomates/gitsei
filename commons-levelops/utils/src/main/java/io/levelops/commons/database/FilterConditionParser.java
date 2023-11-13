package io.levelops.commons.database;

import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class FilterConditionParser {

    public void parseCondition(final Map<String, Object> userSelectors, final Map<String, Object> params){
        // strict match
        //   string
        //   numbers
        //   dates
        //      ranges
        //      greater than
        //      age
        //      IN
        //      1:1
        //      timestamp
        //      epoch seconds
        //      format
        //   json
        // partial match
        //   string
        //   json
        
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String parseCondition(final String fieldName, final String paramPrefix, final Object condition, final Map<String, Object> params){
        if(condition instanceof String){
            return parseCondition(fieldName, paramPrefix, (String) condition, params);
        }
        if(condition instanceof Number){
            return parseCondition(fieldName, paramPrefix, (Number) condition, params);
        }
        if(condition instanceof Collection){
            return parseCondition(fieldName, paramPrefix, (Collection) condition, params);
        }
        if(condition instanceof Map){
            return parseCondition(fieldName, paramPrefix, (Map<String, Object>) condition, params);
        }
        return "";
    }
    
    public static String parseCondition(final String fieldName, final String paramPrefix, final Number condition, final Map<String, Object> params){
        var paramName = paramPrefix + sanitizeParamName(fieldName);
        params.put(paramName, condition);
        if(condition instanceof Double){
            return MessageFormat.format("{0}::float = :{1}::float", fieldName, paramName);
        }
        return MessageFormat.format("{0}::bigint = :{1}::bigint", fieldName, paramName);
    }
    
    public static String parseCondition(final String fieldName, final String paramPrefix, final String condition, final Map<String, Object> params){
        var paramName = paramPrefix + sanitizeParamName(fieldName);
        params.put(paramName, condition);
        return MessageFormat.format("{0} = :{1}", fieldName, paramName);
    }
    
    public static String parseCondition(final String fieldName, final String paramPrefix, final Collection<? extends Object> conditions, final Map<String, Object> params){
        var paramName = paramPrefix + sanitizeParamName(fieldName);
        params.put(paramName, conditions instanceof List ? conditions : conditions.stream().collect(Collectors.toList()));
        return MessageFormat.format("{0} IN (:{1})", fieldName, paramName);
    }
    

    /**
     * // $gt $gte $lt $lte $age
     * 
     * @param fieldName
     * @param conditions
     */
    public static String parseCondition(final String fieldName, final String paramPrefix, final Map<String, Object> conditions, final Map<String, Object> params){
        var paramName = paramPrefix + sanitizeParamName(fieldName);
        var st = new HashSet<String>();
        for(var entry: conditions.entrySet()) {
            var tmpName = paramName;
            var value = entry.getValue();
            switch(entry.getKey()){
                case "$gt":
                    tmpName += "_gt";
                    st.add(fieldName + " > :" + tmpName);
                    break;
                case "$gte":
                    tmpName += "_gte";
                    st.add(fieldName + " >= :" + tmpName);
                    break;
                case "$lt":
                    tmpName += "_lt";
                    st.add(fieldName + " < :" + tmpName);
                    break;
                case "$lte":
                    tmpName += "_lte";
                    st.add(fieldName + " <= :" + tmpName);
                    break;
                case "$age":
                    st.add("(" + fieldName +")::bigint"+ " >= (EXTRACT(EPOCH FROM now()) - :" + tmpName + ")");
                    value = Integer.valueOf(value.toString());
                    break;
                case "$begin":
                case "$begins":
                    tmpName += "_begins";
                    st.add(fieldName + " SIMILAR TO :" + tmpName);
                    value = value + "%";
                    break;
                case "$end":
                case "$ends":
                    tmpName += "_ends";
                    st.add(fieldName + " SIMILAR TO :" + tmpName);
                    value = "%" + value;
                    break;
                case "$contain":
                case "$contains":
                    tmpName += "_contains";
                    st.add(fieldName + " SIMILAR TO :" + tmpName);
                    value = "%" + value + "%";
                    break;
                default:
                    // pass it to other parsers and append results
                    log.warn("unsupported key: {}", entry.getKey());
                    continue;
            }
            params.put(tmpName, value);
        };
        return CollectionUtils.isNotEmpty(st) ? "(" + String.join(" AND ", st) + ")" : "";
    }

    @Nonnull
    public static String sanitizeParamName(@Nonnull String paramName) {
        Validate.notBlank(paramName, "paramName cannot be null or empty");
        return paramName
                .replaceAll("-", "_")
                .replaceAll(">", "")
                .replaceAll("@", "")
                .replaceAll("\\.", "_")
                .replaceAll(":", "");
    }
}
