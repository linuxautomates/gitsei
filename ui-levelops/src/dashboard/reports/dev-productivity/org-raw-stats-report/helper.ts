import { get } from "lodash";
import { defaultColumns, getOrgRawStatColumns, getRawStatsColumnsList, rawStatsColumns } from "../rawStatsTable.config";

export const transformData = (data: any) => {
  const { apiData } = data;
  return { data: apiData || [] };
};

export const getChartProps = (widget: any, interval?: string) => {
  const { selected_columns } = widget;
  return {
    columns: getOrgRawStatColumns(selected_columns, interval, "org-raw-stat"),
    defaultColumns: defaultColumns
  };
};

export const getFilters = (props: any) => {
  const { tempWidgetInterval } = props;
  return {
    filter: {
      ou_ref_ids: props.dashboardOuIds,
      interval: tempWidgetInterval ? tempWidgetInterval : props.filters.interval
    }
  };
};
