import { useAPIFilter } from "custom-hooks/useAPIFilter";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AddWidgetFilterContainerProps } from "../AddWidgetFilter/AddWidgetFilter.container";

interface ExcludeStatusAPIFilterContainerProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
  activePopKey?: string;
  handleActivePopkey: (key: string | undefined) => void;
}

const ExcludeStatusAPIFilterContainer: React.FC<ExcludeStatusAPIFilterContainerProps> = (
  props: ExcludeStatusAPIFilterContainerProps
) => {
  const { filterProps } = props;
  const { filterMetaData } = filterProps;
  const { reportType } = filterMetaData as ApiDropDownData;

  const statusFilterConfig = useMemo(() => {
    let config: LevelOpsFilter[] | ((args: any) => LevelOpsFilter[]) | undefined = getWidgetConstant(
      reportType,
      REPORT_FILTERS_CONFIG
    );
    if (config) {
      if (typeof config === "function") {
        config = config({});
      }
      const filter = config.find((item: LevelOpsFilter) => item.id === "statuses" || item.id === "workitem_statuses");
      if (filter) {
        return { ...filterMetaData, ...filter.filterMetaData };
      }
    }
  }, [reportType, filterMetaData]);

  const excludeRestData = useAPIFilter(filterMetaData as ApiDropDownData);
  const statusRestData = useAPIFilter(statusFilterConfig as ApiDropDownData);

  const renderFilter = useMemo(() => {
    return React.createElement(props.filterProps.renderComponent, {
      ...props,
      filterProps: {
        ...props.filterProps,
        filterMetaData: {
          ...props.filterProps.filterMetaData,
          apiConfig: {
            ...excludeRestData,
            status_data: statusRestData.data,
            loading: excludeRestData.loading || statusRestData.loading,
            error: excludeRestData.error || statusRestData.error
          }
        }
      }
    });
  }, [props, excludeRestData, statusRestData]);

  return renderFilter;
};

export default ExcludeStatusAPIFilterContainer;
