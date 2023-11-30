import React from "react";
import queryString from "query-string";
import "./TriageGridView.scss";
import { StagesPaginated } from "../../components";

import moment from "moment";
interface JobStagesPageProps {
  history: any;
  location: any;
}

export const JobStagesPage: React.FC<JobStagesPageProps> = (props: JobStagesPageProps) => {
  const { job_id, result, date } = queryString.parse(props.location.search);

  let dateRange = {};
  if (date) {
    const numDate = parseInt(date as string);
    const sDay = moment.unix(numDate).startOf("day").unix();
    const eDay = moment.unix(numDate).endOf("day").unix();
    dateRange = {
      $gt: sDay,
      $lt: eDay
    };
  }
  const filter = {
    job_ids: job_id ? [job_id] : [],
    result: result ? [result] : [],
    start_time: dateRange
  };

  return <StagesPaginated moreFilters={filter} />;
};
