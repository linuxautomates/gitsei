package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class PagerDutyTransitionalEntity implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("ingestion_data_type")
    private PagerDutyIngestionDataType ingestionDataType;
    private Long updatedAt;
    private Map<String, Object> any = new HashMap<>();
    
    @JsonAnySetter
    public void setAny(final String key, final Object value){
        this.any.put(key, value);
    }
    
    @JsonAnyGetter
    public Map<String, Object> getAny(){
        return this.any;
    }
}