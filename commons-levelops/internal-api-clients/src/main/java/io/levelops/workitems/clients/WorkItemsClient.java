package io.levelops.workitems.clients;

import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface WorkItemsClient {
    WorkItem create(String company, WorkItem workItem) throws InternalApiClientException;

    WorkItem createSnippetWorkItemMultipart(String company, MultipartFile createSnippetWorkItemRequest, MultipartFile snippetFile) throws InternalApiClientException, IOException;
    WorkItem createSnippetWorkItem(String company, CreateSnippetWorkitemRequestWithText request) throws InternalApiClientException, IOException;

    WorkItem getById(String company, UUID workItemId) throws InternalApiClientException;

    WorkItem getByVanityId(String company, String vanityId) throws InternalApiClientException;

    Id update(String company, String submitter, WorkItem workItem) throws InternalApiClientException;

    Id changeProductId(String company, UUID workItemId, String productId) throws InternalApiClientException;

    Id changeParentId(String company, UUID workItemId, String parentWorkItemId) throws InternalApiClientException;

    Id changeState(String company, UUID workItemId, String newState) throws InternalApiClientException;

    DeleteResponse delete(String company, UUID workItemId) throws InternalApiClientException;

    BulkDeleteResponse bulkDelete(String company, List<UUID> ids) throws InternalApiClientException;

    PaginatedResponse<WorkItem> list(String company, DefaultListRequest search) throws InternalApiClientException;

    DbListResponse<DbAggregationResult> aggregate(String company, WorkItemFilter.Calculation calculation, DefaultListRequest filter) throws InternalApiClientException;
}
