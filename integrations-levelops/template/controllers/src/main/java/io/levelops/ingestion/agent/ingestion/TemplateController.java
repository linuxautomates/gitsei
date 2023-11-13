package io.levelops.ingestion.agent.ingestion;

import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.template.models.TemplateScanQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public class TemplateController implements DataController<TemplateScanQuery>{
 
    @Autowired
    public TemplateController(){

    }

    @Override
    public TemplateScanQuery parseQuery(Object arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public ControllerIngestionResult ingest(io.levelops.ingestion.models.JobContext jobContext, TemplateScanQuery query) throws IngestException {

    // get orgs 
    // get projects
    // get errors per project
    // get events per project
    // get pivots
    return null;
    }
    
}