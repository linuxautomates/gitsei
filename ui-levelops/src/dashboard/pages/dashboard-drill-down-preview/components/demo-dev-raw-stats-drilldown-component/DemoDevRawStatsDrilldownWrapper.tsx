import React, { useMemo } from "react";
import { ScorecardDashboardTimeRangeOptions } from "dashboard/components/dashboard-view-page-secondary-header/constants";
import { strIsEqual } from "utils/stringUtils";
import DemoFeatureDrillDownComponent from "./DemoFeatureDrillDownComponent";
import DemoLineGraphForDevRawStatsDrilldown from "./DemoLineGraphForDevRawStatsDrilldown";

interface DemoDevRawStatsDrilldownProps {
  drillDownProps: any;
  onDrilldownClose: any;
  columns?: any;
  widgetId?: string;
}

const DemoDevRawStatsDrilldownWrapper: React.FC<DemoDevRawStatsDrilldownProps> = (
  props: DemoDevRawStatsDrilldownProps
) => {
  const { drillDownProps, onDrilldownClose, columns, widgetId } = props;
  const { data, graphData, drilldown_count } = drillDownProps;
  const { columnName, record, interval } = drillDownProps?.devRawStatsAdditionalData;
  const name = record?.full_name;
  const selectedTimeRange = ScorecardDashboardTimeRangeOptions.find(item =>
    strIsEqual(item.key, interval.toLowerCase())
  )?.label;

  const lineChart = useMemo(() => {
    return (
      <DemoLineGraphForDevRawStatsDrilldown
        graphData={graphData}
        widgetId={widgetId}
        columnName={columnName}
        interval={interval}
      />
    );
  }, [widgetId, columnName, interval, graphData]);

  return (
    <DemoFeatureDrillDownComponent
      selectedFeature={columnName}
      columns={columns}
      data={data}
      dashboardTimeRange={selectedTimeRange}
      isDevRawStatsDrilldown={true}
      onDrilldownClose={onDrilldownClose}
      nameForTitle={name}
      extraPropsForGraph={lineChart}
      isDashboardDrilldown={true}
      drilldown_count={drilldown_count}
    />
  );
};

export default DemoDevRawStatsDrilldownWrapper;
