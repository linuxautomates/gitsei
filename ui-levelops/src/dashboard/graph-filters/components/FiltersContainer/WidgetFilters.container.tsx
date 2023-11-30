import { RestWidget } from "classes/RestDashboards";
import DashboardGraphsFiltersContainer from "dashboard/graph-filters/containers/dashboard-graph-filters.container";
import React from "react";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import AddWidgetFilterContainer from "../AddWidgetFilter/AddWidgetFilter.container";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
interface Props {
  report: any;
  integrationIds: Array<string>;
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterRemoved: (key: string) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  partialFiltersErrors: any;
  maxRecords: number;
  widgetId: string;
  onWeightChange: (value: any, type: any) => void;
  onMetadataChange: (value: any, type: any, reportType?: String) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  onMaxRecordsSelection?: (value: any) => void;
  partialFilterError?: any;
  widgetWeights: any;
  weightError: string;
  graphType?: string;
  metaData?: any;
  dashboardMetaData?: any;
  onChildFilterRemove: (filterpayload: any, returnUpdatedQuery?: boolean | undefined) => any;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  onModifiedFilterValueChange?: (payload: any) => void;
  onSingleStatTypeFilterChange: (value: string, removeKey: string) => void;
  handleBulkAddToMetadata?: (metadataForMerge?: any, filterPayload?: any) => void;
  isMultiTimeSeriesReport?: boolean;
  isParentTabData?: boolean;
  advancedTabState?: {
    value: boolean;
    callback: any;
  };
  currentFilterKeyValueChange?: string;
}

const WidgetFiltersContainer: React.FC<Props> = props => {
  const {
    report,
    integrationIds,
    partialFiltersErrors,
    filters,
    maxRecords,
    graphType,
    weightError,
    widgetId,
    widgetWeights,
    metaData,
    dashboardMetaData,
    onExcludeFilterChange,
    onFilterRemoved,
    onFilterValueChange,
    onChildFilterRemove,
    handlePartialValueChange,
    handleTimeRangeFilterValueChange,
    handleTimeRangeTypeChange,
    onMetadataChange,
    onWeightChange,
    onAggregationAcrossSelection,
    onMaxRecordsSelection,
    handleLastSprintChange,
    onModifiedFilterValueChange,
    onSingleStatTypeFilterChange,
    handleBulkAddToMetadata,
    isMultiTimeSeriesReport,
    isParentTabData,
    advancedTabState,
    currentFilterKeyValueChange
  } = props;

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const queryParamDashboardOUId = queryParamOU ? [queryParamOU] : [];

  if (widget?.hasFilterConfigs) {
    return (
      <AddWidgetFilterContainer
        metadata={metaData}
        report={report}
        integrationIds={integrationIds}
        filters={filters}
        onExcludeFilterChange={onExcludeFilterChange}
        onFilterValueChange={onFilterValueChange}
        onWeightChange={onWeightChange}
        widgetWeights={widgetWeights}
        weightError={weightError}
        handlePartialValueChange={handlePartialValueChange}
        handleTimeRangeTypeChange={handleTimeRangeTypeChange}
        onChildFilterRemove={onChildFilterRemove}
        onFilterRemoved={onFilterRemoved}
        handleTimeRangeFilterValueChange={handleTimeRangeFilterValueChange}
        dropdownButtonClass={"filter-select-button"}
        partialFiltersErrors={partialFiltersErrors}
        handleMetadataChange={onMetadataChange}
        dashboardMetaData={dashboardMetaData}
        onModifiedFilterValueChange={onModifiedFilterValueChange}
        handleLastSprintChange={handleLastSprintChange}
        handleBulkAddToMetadata={handleBulkAddToMetadata}
        onAggregationAcrossSelection={onAggregationAcrossSelection}
        handleSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
        queryParamDashboardOUId={queryParamDashboardOUId}
        isMultiTimeSeriesReport={isMultiTimeSeriesReport}
        isParentTabData={isParentTabData}
        advancedTabState={advancedTabState}
        currentFilterKeyValueChange={currentFilterKeyValueChange}
      />
    );
  }

  return (
    <DashboardGraphsFiltersContainer
      widgetId={widgetId}
      reportType={report}
      filters={filters}
      integrationIds={integrationIds}
      onFilterValueChange={onFilterValueChange}
      onMetadataChange={onMetadataChange}
      onWeightChange={onWeightChange}
      graphType={graphType}
      widgetWeights={widgetWeights}
      weightError={weightError}
      maxRecords={maxRecords}
      onAggregationAcrossSelection={onAggregationAcrossSelection}
      onMaxRecordsSelection={onMaxRecordsSelection}
      onExcludeChange={onExcludeFilterChange}
      onTimeRangeTypeChange={handleTimeRangeTypeChange}
      onTimeFilterValueChange={handleTimeRangeFilterValueChange}
      onPartialChange={handlePartialValueChange}
      partialFilterError={partialFiltersErrors}
      metaData={metaData}
      dashboardMetaData={dashboardMetaData}
      handleLastSprintChange={handleLastSprintChange}
      onSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
      onModifieldFilterValueChange={onModifiedFilterValueChange}
      queryParamDashboardOUId={queryParamDashboardOUId}
    />
  );
};

export default React.memo(WidgetFiltersContainer);
