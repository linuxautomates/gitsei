import * as React from "react";
import { tableColumns } from "./table-config";
import { ExpandedHitsComponent } from "./expandedHits.component";
import ServerPaginatedTable from "../../../shared-resources/containers/server-paginated-table/server-paginated-table.container";
import "./stagesTable.style.scss";
import { useState } from "react";

interface StagesPaginatedProps {
  jobRunId?: string;
  title?: React.Component;
  moreFilters: any;
  jobIds?: Array<string>;
  dateRange?: any;
  results?: Array<string>;
  selectedStageId?: string;
  topLevelFilters?: any;
  showSelectedStage?: boolean;
}

const StagesPaginatedComponent: React.FC<StagesPaginatedProps> = props => {
  const [defaultExpandIndex, setDefaultExpandIndex] = useState<number | undefined>(undefined);
  const filters = {
    job_run_id: props.jobRunId,
    ...(props.moreFilters || {}),
    ...(props.topLevelFilters || {})
  };
  return (
    <ServerPaginatedTable
      pageName={"triageResultsDetails"}
      uri={"jenkins_pipeline_job_stages"}
      method={"list"}
      columns={tableColumns}
      hasSearch={false}
      moreFilters={filters}
      expandedRowRender={(record: any) => <ExpandedHitsComponent hits={record.hits || []} stageId={record.id || ""} />}
      componentTitle={props.title}
      rowClassName={(record: any, index: number) => {
        if (record.id === props.selectedStageId && defaultExpandIndex !== index) {
          setDefaultExpandIndex(index);
        }
        return "";
      }}
      defaultExpandedRowKeys={props.showSelectedStage ? defaultExpandIndex : ""}
    />
  );
};

export default StagesPaginatedComponent;
