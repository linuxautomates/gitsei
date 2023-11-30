import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import DoraStatChart from "../DoraStatChart";
import DoraStageChart from "./containers/DoraStageChart";
import "./DoraStageStatWrapper.style.scss";
import { get } from "lodash";
import { DoraStageData, DoraStatData } from "./types";
import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";
import { convertToDay } from "custom-hooks/helpers/leadTime.helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { DoraLeadTimeStageChartProps } from "shared-resources/charts/chart-types";
import { WidgetDrilldownHandlerContext, WidgetFilterContext } from "dashboard/pages/context";

export interface DoraCombinedStatStageChartProps extends Partial<DoraLeadTimeStageChartProps> {
  chartTitle: string;
  apiData: {
    stats: DoraStatData;
    stages: DoraStageData[];
  };
  onDoraClick: (data: any, filters?: any) => void;
  widgetMetaData?: basicMappingType<string>;
  statDescInterval?: string;
  dateRangeValue?: { $gt: number; $lt: number };
  reportType?: string;
}

const DoraCombinedStatStageChart: React.FC<DoraCombinedStatStageChartProps> = (
  props: DoraCombinedStatStageChartProps
) => {
  
  const { widgetMetaData, statDescInterval, onDoraClick, dateRangeValue, data, id, reportType } = props;
  const metrics = get(widgetMetaData, "metrics", "mean");
  const stats = get(props, ["apiData", "stats"], {});
  const stages = get(props, ["apiData", "stages"], []);
  const [clicked, setClicked] = useState<boolean>(false);
  const [activePhase, setActivePhase] = useState<string | undefined>(undefined);
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { isDrilldownOpen, drilldownWidgetId } = useContext(WidgetDrilldownHandlerContext);
  const doraStatProps: any = useMemo(() => {
    const total = get(stats, "total", 0);
    const metricKey = `lead_time_band_${metrics}`;
    const bandValue = get(stats, [metricKey], "");
    const valueInDays = Math.round(convertToDay(get(stats, [`lead_time_${metrics}`], 0), AcceptanceTimeUnit.SECONDS));
    const unit = valueInDays > 1 ? "Days" : "Day";
    return {
      value: valueInDays,
      unit: unit,
      band: bandValue,
      count: total,
      descInterval: statDescInterval,
      showDoraGrading: true,
      descStringValue: "Changes",
      reportType: reportType
    };
  }, [stats, metrics, statDescInterval]);

  const statClickHandler = () => {
    setClicked(true);
    setActivePhase(undefined);
    onDoraClick({
      time_range: {
        $lt: dateRangeValue?.$lt,
        $gt: dateRangeValue?.$gt
      },
      activeColumn: "total",
      ...(contextFilters as any)?.[id as string]
    });
  };
  const handlePhaseClick = useCallback(
    (stageName: any) => {
      setClicked(false);
      setActivePhase(stageName);
      onDoraClick &&
        onDoraClick({
          activeColumn: stageName,
          time_range: dateRangeValue,
          histogram_stage_name: stageName,
          ...(contextFilters as any)?.[id as string]
        });
    },
    [data, onDoraClick, dateRangeValue, contextFilters, id]
  );

  useEffect(() => {
    if (!isDrilldownOpen || drilldownWidgetId !== id) {
      setActivePhase(undefined);
      setClicked(false);
    }
  }, [isDrilldownOpen, drilldownWidgetId]);

  return (
    <div className="dora-stat-stage-chart">
      <div className="left-component">
        <DoraStatChart {...doraStatProps} onClick={statClickHandler} clicked={clicked} setClicked={setClicked} />
      </div>
      <div className="right-component">
        <DoraStageChart
          {...(props as any)}
          isShowHeaderText={true}
          data={stages}
          metrics={metrics}
          onDoraClick={handlePhaseClick}
          activePhase={activePhase}
        />
      </div>
    </div>
  );
};

export default DoraCombinedStatStageChart;
