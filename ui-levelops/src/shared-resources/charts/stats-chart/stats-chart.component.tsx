import React, { useContext, useEffect, useMemo, useRef } from "react";
import { Badge, Icon, Statistic, Tag } from "antd";
import { get, isEqual } from "lodash";
import cx from "classnames";
import {
  ALL_SINGLE_STAT_REPORTS,
  JIRA_SPRINT_REPORTS,
  LEAD_TIME_REPORTS,
  SCM_REPORTS
} from "dashboard/constants/applications/names";
import { WidgetBGColorContext } from "dashboard/pages/context";
import { StatsChartProps } from "../chart-types";
import { getNumberAbbreviation, getSimplifiedUnit } from "../helper";
import { getIdealRangeBGColor } from "./ideal-range.helper";
import "./stats-chart-component.style.scss";
import widgetConstants from "dashboard/constants/widgetConstants";
import { SHOW_SINGLE_STAT_EXTRA_INFO } from "model/report/scm/baseSCMReports.constant";
import { getUriUnit } from "../engineer-table/helper";
import { spritnMetricKeyTypes } from "dashboard/graph-filters/components/sprintFilters.constant";

const decimalReports: string[] = [
  LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
  JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
  SCM_REPORTS.ISSUES_COUNT_SINGLE_STAT,
  ...ALL_SINGLE_STAT_REPORTS
];
const StatsChartComponent = (props: StatsChartProps) => {
  const {
    stat,
    statTrend,
    unit,
    idealRange,
    reportType = "",
    metric,
    showRoundedValue,
    precision,
    unitSymbol,
    simplifyValue
  } = props;
  let statColor = statTrend ? (statTrend > 0 ? "#37b57e" : "#e2392b") : "inherit";
  const statIcon = statTrend ? (statTrend > 0 ? "caret-up" : "caret-down") : "";
  const { setWidgetBGColor } = useContext(WidgetBGColorContext);

  const idealRangeRef = useRef<{ min: number; max: number }>();
  const statRef = useRef<number>();
  let statExtraInfo = get(widgetConstants, [reportType, SHOW_SINGLE_STAT_EXTRA_INFO], undefined);

  if (typeof statExtraInfo === "function") {
    statExtraInfo = statExtraInfo(props);
  }

  useEffect(() => {
    if (idealRange) {
      if (!isEqual(idealRange, idealRangeRef.current) || !isEqual(statRef.current, stat)) {
        idealRangeRef.current = idealRange;
        statRef.current = stat;
        setWidgetBGColor(props.id as any, getIdealRangeBGColor(idealRange, stat));
      }
    }
  }, [idealRange]);

  const onClick = () => {
    props.onClick && props.onClick("stat");
  };

  const getPrecision = useMemo(() => {
    if (!!precision) {
      return precision;
    }
    if (
      simplifyValue ||
      reportType === LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT ||
      reportType === SCM_REPORTS.ISSUES_COUNT_SINGLE_STAT ||
      (reportType === JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT &&
        ((stat < 1 && stat > 0) || metric === spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT))
    ) {
      return 1;
    }
    if (ALL_SINGLE_STAT_REPORTS.includes(reportType as any)) {
      return 2;
    }
    return 0;
  }, [reportType, stat, metric, precision]);

  const value = getNumberAbbreviation(
    stat,
    showRoundedValue !== undefined ? showRoundedValue : !decimalReports.includes(reportType as any),
    getPrecision,
    simplifyValue
  );
  const precisionValue = parseFloat(value);

  const simplifiedUnit = getSimplifiedUnit(stat, unit, simplifyValue);

  return (
    <div
      className={cx({ "stats-chart-cursor-pointer": props.hasClickEvents }, "stats-chart-component", {
        "stats-extra-info-wrapper": statExtraInfo
      })}
      onClick={onClick}>
      <Statistic
        title={""}
        value={value}
        valueRender={value => (
          <div className="statistical-value" style={{ color: !!idealRange ? statColor : "" }}>
            {value} {unitSymbol}
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
                }}
                onClick={onClick}>
                <Icon type={statIcon} />
                {statTrend && getNumberAbbreviation(statTrend)}
              </span>
            ) : null}
            {simplifiedUnit}
          </>
        }
        precision={Number.isInteger(precisionValue) ? undefined : getPrecision}
      />
      {statExtraInfo ? statExtraInfo : null}
    </div>
  );
};

export default StatsChartComponent;
