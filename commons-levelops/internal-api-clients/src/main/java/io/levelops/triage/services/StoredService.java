package io.levelops.triage.services;

import io.levelops.commons.databases.models.database.StoredFilter;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;

import java.io.IOException;
import java.util.List;

public interface StoredService {

    String upsertStoredFilter(final String company, final String type,
                                     final StoredFilter filter, final String name) throws IOException;

    StoredFilter getStoredFilter(final String company, final String type,
                                        final String name) throws IOException;

    DeleteResponse deleteStoredFilter(final String company, final String type,
                                             final String triageFilterName) throws IOException;

    BulkDeleteResponse bulkDeleteStoredFilters(final String company, final String type,
                                                      final List<String> filterNames) throws IOException;

    DbListResponse<StoredFilter> listStoredFilters(final String company, final String type,
                                                          final DefaultListRequest listRequest) throws IOException;
}
