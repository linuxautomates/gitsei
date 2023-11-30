import { jiraBacklogKeyMapping } from "dashboard/graph-filters/components/Constants";
import { get, reduce } from "lodash";

export const customBacklogChartProps = (existingProps: any, metadata: any, filters: any, reportData: any) => {
  const chartType = get(metadata, ["graphType"], "bar_chart");
  const leftYAxis = get(metadata, "leftYAxis", "total_tickets");
  const rightYAxis = get(metadata, "rightYAxis", "median");
  const isStacked = !!filters?.stacks && !!filters?.stacks.length;
  const unit = leftYAxis === "total_tickets" ? "Tickets" : "Story Points";
  switch (chartType) {
    case "bar_chart":
      let newBarProps: any[] = [];
      let metrics: any[] = [];
      if (isStacked) {
        metrics = [rightYAxis];
        const updatedKeys = reduce(
          (reportData as any)?.data || [],
          (acc: any, next: any) => {
            return {
              ...acc,
              ...next
            };
          },
          {}
        );
        delete updatedKeys.count;
        delete updatedKeys.name;
        newBarProps = Object.keys(updatedKeys)
          .filter(
            key => !["toolTip", "id", "median", "p90", "mean", "total_tickets", "total_story_points"].includes(key)
          )
          .map(key => {
            return {
              name: key,
              dataKey: key
            };
          });
      } else {
        metrics = [leftYAxis, rightYAxis];
        const defaultProps = metrics.map((item: any) => {
          const mappedKey = get(jiraBacklogKeyMapping, [item], "");
          return {
            name: mappedKey,
            dataKey: item
          };
        });
        newBarProps = [...defaultProps, ...newBarProps];
      }

      const props = {
        ...existingProps,
        stacked: isStacked,
        barProps: newBarProps,
        bypassTitleTransform: true,
        unit
      };
      return { ...props };
    case "line_chart":
      return {
        ...existingProps,
        chartProps: existingProps.chartProps,
        unit
      };
    default:
      return existingProps;
  }
};

export const getBacklogChartUnits = (filters: any) => {
  const leftYAxis = get(filters, "leftYAxis", "total_tickets");
  let leftYAxisUnit = "Tickets";
  if (leftYAxis === "total_story_points") {
    leftYAxisUnit = "Story Points";
  }
  return [leftYAxisUnit, "Days"];
};
