package io.levelops.commons.faceted_search.db.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Log4j2
@Service
public class JiraIssuesDBService {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public JiraIssuesDBService(final DataSource dataSource) {
        //super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }




}
