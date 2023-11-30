import React, { useCallback, useEffect, useState } from "react";
import ActivityTable from "shared-resources/charts/activity-table/ActivityTable";
import { AntCard } from "shared-resources/components";
import "./demo-pr-activity.scss";
import DemoPRActivityTitle from "./DemoPRActivityTitle";
import { getPRActivityColumns } from "./../../../../../pages/scorecard/components/PRActivity/prActivity.tableConfig";
import { viewByOptions } from "./../../../../../pages/scorecard/components/PRActivity/helpers";
import { DemoPRActivityChartProps } from "./../../Widgets-Graph/Widget-Grapg-Types/demo-pr-activity-chart.types";

const DemoPRActivityComponent = (props: DemoPRActivityChartProps) => {
  const { dashboardTimeGtValue, dashboardTimeLtValue, isTrellisDemoWidget, data } = props;
  const [selectedTimeRange, setSelectedTimeRange] = useState<{ $gt: string; $lt: string }>({
    $gt: dashboardTimeGtValue || "1656288000",
    $lt: dashboardTimeLtValue || "1656892799"
  });
  const [columns, setColumns] = useState<Array<any>>([]);
  const [viewBy, setViewBy] = useState<string>(viewByOptions[1].key);
  const [prData, setPrData] = useState<any>(
    isTrellisDemoWidget ? data?.PRActivityData : props?.data?.[viewByOptions[0].key]?.[0]
  );

  useEffect(() => {
    setColumns(getPRActivityColumns(selectedTimeRange));
    setPrData(getPRActivityData());
  }, []);
  useEffect(() => {
    setPrData(getPRActivityData());
  }, [viewBy, data]);

  const getPRActivityData = useCallback(() => {
    if (isTrellisDemoWidget) return data?.PRActivityData;

    if (viewBy === viewByOptions[0].key) {
      return data?.repo_id && data?.repo_id.length > 0 ? data?.repo_id?.[0] : {};
    } else if (viewBy === viewByOptions[1].key) {
      return data?.integration_user && data?.integration_user.length > 0 ? data?.integration_user?.[0] : {};
    }
  }, [viewBy, data]);

  const attributes = isTrellisDemoWidget
    ? {}
    : { viewBy: viewBy, setViewBy: setViewBy, displayOnlyTitle: false, isWidget: true };
  return (
    <AntCard
      className={isTrellisDemoWidget ? "pr-activity-container" : "demo-pr-activity-container"}
      title={
        <DemoPRActivityTitle
          title={isTrellisDemoWidget ? "PR Activity" : ""}
          dashboardTimeGtValue={dashboardTimeGtValue || "1656666200"}
          dashboardTimeLtValue={dashboardTimeLtValue || "1659258200"}
          setSelectedTimeRange={setSelectedTimeRange}
          isTrellisDemoWidget={isTrellisDemoWidget}
          {...attributes}
        />
      }>
      {<ActivityTable data={prData} columns={columns} />}
    </AntCard>
  );
};

export default DemoPRActivityComponent;
