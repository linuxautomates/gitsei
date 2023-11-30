import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";
import { Typography } from "antd";
import { v1 as uuid } from "uuid";

import { triageMatchingJobsSelector } from "reduxConfigs/selectors/job.selector";
import { AntCol, AntRow } from "../../../../../shared-resources/components";
import { triageMatchingJobs } from "reduxConfigs/actions/restapi";
import Loader from "../../../../../components/Loader/Loader";
import { Link } from "react-router-dom";
import { getBaseUrl } from "constants/routePaths";

interface TriageJobRunsComponentProps {
  workItem: any;
}

const { Text } = Typography;

const TriageJobRunsComponent: React.FC<TriageJobRunsComponentProps> = (props: TriageJobRunsComponentProps) => {
  const MAX_MATCHING_TO_LIST = 10;
  const [jobDetails, setJobDetails] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const dispatch = useDispatch();

  const matchingJobState = useSelector(triageMatchingJobsSelector);

  useEffect(() => {
    if (loading) {
      const loadingJobs = get(matchingJobState, [props.workItem.id, "loading"], true);
      if (!loadingJobs) {
        let jobDetails = get(matchingJobState, [props.workItem.id, "data"], []);
        setJobDetails(jobDetails.slice(0, MAX_MATCHING_TO_LIST));
        setLoading(false);
      }
    }
  }, [matchingJobState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const { workItem } = props;
    if (workItem.cicd_mappings && workItem.cicd_mappings.length > 0) {
      setLoading(true);
      dispatch(triageMatchingJobs(workItem, workItem.id));
    } else {
      setJobDetails([]);
    }
  }, [props.workItem]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return <Loader />;
  }

  if (jobDetails.length === 0) {
    return null;
  }

  const renderJob = (job: any) => {
    let jobName = job.job_name;
    if (job.cicd_job_run_stage_name) {
      jobName = job.job_name + " / " + job.cicd_job_run_stage_name;
    }
    let url = `${getBaseUrl()}/triage/view?id=${job.id}`;
    if (job.cicd_job_run_stage_id) {
      url += "&stage_id=" + job.cicd_job_run_stage_id;
    }
    return (
      <div key={uuid()}>
        <Text>
          <Link target={"_blank"} to={url} rel="noopener noreferrer">
            {jobName} #{job.job_run_number}
          </Link>
        </Text>
      </div>
    );
  };

  return (
    <AntRow className="my-10">
      <AntCol span={24}>
        <div className="flex direction-column">
          <div className="mb-10">
            <strong>Matching Triage Jobs</strong>
          </div>
          {jobDetails.map((mapping: any) => renderJob(mapping))}
        </div>
      </AntCol>
    </AntRow>
  );
};

export default React.memo(TriageJobRunsComponent);
