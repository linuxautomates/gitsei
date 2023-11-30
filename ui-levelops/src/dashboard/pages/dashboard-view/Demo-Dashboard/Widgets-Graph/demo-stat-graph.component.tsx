import React from "react";
import { Icon, Statistic } from "antd";
import cx from "classnames";
import { getNumberAbbreviation } from "shared-resources/charts/helper";
import { DemoStatChartProps } from "./Widget-Grapg-Types/demo-stat-chart.types";
import { get } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { SHOW_SINGLE_STAT_EXTRA_INFO } from "model/report/scm/baseSCMReports.constant";
import { SCM_DORA_REPORTS } from "dashboard/constants/applications/names";
import DemoStatsChartExtra from "./DemoStatChartExtra";

const DemoStatsChartComponent: React.FC<DemoStatChartProps> = (props: DemoStatChartProps) => {
  const { data, reportType } = props;
  const { stat, statTrend, unit, unitSymbol } = data;
  let statColor = statTrend ? (statTrend > 0 ? "#37b57e" : "#e2392b") : "inherit";
  const statIcon = statTrend ? (statTrend > 0 ? "caret-up" : "caret-down") : "";
  let statExtraInfo = get(widgetConstants, [reportType as any, SHOW_SINGLE_STAT_EXTRA_INFO], undefined);

  const DESC_FOR_LEAD_TIME = "stories shipped in last 3 months";
  const DESC_FOR_TIME_TO_RECOVER = "bugs fixed in last 3 months";
  const DESC_FOR_DEPLOYMENT_FREQUENCY = "deployments in last 3 months";
  const DESC_FOR_FAILURE_RATE = "changes in last 3 months";

  if (typeof statExtraInfo === "function") {
    statExtraInfo = statExtraInfo({ ...props.data });
  }
  if (reportType === SCM_DORA_REPORTS.LEAD_TIME_FOR_CHNAGE) {
    statExtraInfo = <DemoStatsChartExtra desc={DESC_FOR_LEAD_TIME} {...props.data} />;
  }
  if (reportType === SCM_DORA_REPORTS.TIME_TO_RECOVER) {
    statExtraInfo = <DemoStatsChartExtra desc={DESC_FOR_TIME_TO_RECOVER} {...props.data} />;
  }
  if (reportType === SCM_DORA_REPORTS.DEPLOYMENT_FREQUENCY) {
    statExtraInfo = <DemoStatsChartExtra desc={DESC_FOR_DEPLOYMENT_FREQUENCY} {...props.data} />;
  }
  if (reportType === SCM_DORA_REPORTS.FAILURE_RATE) {
    statExtraInfo = <DemoStatsChartExtra desc={DESC_FOR_FAILURE_RATE} {...props.data} />;
  }
  return (
    <div
      className={cx("stats-chart-component", {
        "stats-extra-info-wrapper": statExtraInfo
      })}>
      <Statistic
        title={""}
        value={stat}
        valueRender={value => (
          <div className="statistical-value">
            {value}
            {unitSymbol}
          </div>
        )}
        suffix={
          <>
            {statTrend != 0 ? (
              <span
                style={{
                  color: statColor,
                  display: "flex",
                  alignItems: "center",
                  flexWrap: "wrap",
                  marginLeft: "1.5rem"
                }}>
                <Icon type={statIcon} />
                {statTrend && getNumberAbbreviation(statTrend)}
              </span>
            ) : null}
            {unit}
          </>
        }
      />
      {statExtraInfo ? statExtraInfo : null}
    </div>
  );
};

export default DemoStatsChartComponent;
