import React, { useEffect, useRef, useState } from "react";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { v1 as uuid } from "uuid";

import { WorkitemList } from "workitems/containers";
import { fetchTriageResultJobs } from "reduxConfigs/actions/restapi";
import { getTriageJobResultsList } from "reduxConfigs/selectors/triageSelector";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import AntDrawerWrapperComponent from "../../../../shared-resources/components/ant-drawer-wrapper/ant-drawer-wrapper.component";
import Loader from "../../../../components/Loader/Loader";
import "./issue-drawer.styles.scss";

export interface IssuesDrawerProps {
  visible: boolean;
  onClose: () => void;
  filters: any;
}

export const IssuesDrawerComponent: React.FC<IssuesDrawerProps> = (props: IssuesDrawerProps) => {
  const uniqueKey = useRef<string>(uuid());

  const [jobIds, setJobIds] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const dispatch = useDispatch();
  const triageJobResultState = useSelector(getTriageJobResultsList);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("fetchJobResults", "list", "0"));
      dispatch(restapiClear("jenkins_pipeline_triage_runs", "list", "0"));
    };
  }, []);

  useEffect(() => {
    const loading = get(triageJobResultState, ["0", "loading"], true);
    if (!loading) {
      setJobIds(get(triageJobResultState, ["0", "data"], []));
      setLoading(false);
    }
  }, [triageJobResultState]);

  useEffect(() => {
    setJobIds([]);

    if (props && props.filters) {
      const {
        filters: { name, date, status }
      } = props;

      dispatch(
        fetchTriageResultJobs({
          page: 0,
          page_size: 100,
          sort: [],
          filter: {
            job_statuses: [status],
            job_names: [name],
            start_time: {
              $gt: date[0],
              $lt: date[1]
            },
            partial: {}
          },
          across: ""
        })
      );
    }
  }, [props.filters]);

  const renderTable = () => {
    if (loading) {
      return <Loader />;
    }
    return (
      <WorkitemList
        compact
        moreFilters={{ cicd_job_run_ids: jobIds }}
        hasFilters={false}
        hasSearch={false}
        uuid={uniqueKey.current}
      />
    );
  };

  return (
    <AntDrawerWrapperComponent
      visible={props.visible}
      onClose={props.onClose}
      icon={"work"}
      name={props?.filters?.name}
      name_editable={false}
      className="triage-grid-view-issue-drawer"
      width="700px">
      {renderTable()}
    </AntDrawerWrapperComponent>
  );
};

export default React.memo(IssuesDrawerComponent);
