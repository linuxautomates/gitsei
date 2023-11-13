package io.levelops.commons.elasticsearch_clients.models;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Builder;
import lombok.Value;
import org.elasticsearch.client.RestClient;

@Value
@Builder(toBuilder = true)
public class ESContext {
    private final RestClient restClient;

    private final ElasticsearchTransport transport;

    private final ElasticsearchClient client;
}
