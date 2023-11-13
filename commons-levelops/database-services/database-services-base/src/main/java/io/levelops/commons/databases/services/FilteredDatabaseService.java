package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.stream.Stream;

public abstract class FilteredDatabaseService<V, F> extends DatabaseService<V> {

    protected static final int DEFAULT_PAGE_SIZE = 25;
    protected final NamedParameterJdbcTemplate template;
    protected final int pageSize;

    protected FilteredDatabaseService(DataSource dataSource, @Nullable Integer pageSize) {
        super(dataSource);
        this.pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    protected FilteredDatabaseService(DataSource dataSource) {
        this(dataSource, null);
    }

    @Override
    public DbListResponse<V> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null);
    }

    public Stream<V> streamWithCustomPageSize(String company, F filter, int customPageSize) {
        return PaginationUtils.stream(0, 1, page -> filter(page, customPageSize, company, filter).getRecords());
    }

    public Stream<V> stream(String company, F filter) {
        return PaginationUtils.stream(0, 1, page -> filter(page, pageSize, company, filter).getRecords());
    }

    public abstract DbListResponse<V> filter(Integer pageNumber, Integer pageSize, String company, @Nullable F filter);

}
