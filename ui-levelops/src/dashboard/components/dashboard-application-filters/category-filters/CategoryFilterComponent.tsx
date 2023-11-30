import { DashboardFiltersReportType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { AZURE_CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { APIFilter, CustomTimeRangeFilters } from "dashboard/graph-filters/components";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { get } from "lodash";
import React from "react";
import { uiFilterKeys, UiFilters } from "../AddFiltersComponent/UiFilters";
interface CategoryFilterComponentProps {
  fieldTypeList?: { key: string; type: string; name: string }[];
  filterKey: string;
  filters: any;
  supportExcludeFilters: any;
  supportPartialStringFilters: any;
  report: DashboardFiltersReportType;
  activePopKey?: string;
  partialFiltersErrors: any;
  integrationIds: Array<string>;
  getMappedSupportedFilters: (key: any) => {
    [x: number]: any;
  };
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string, isCustom?: boolean) => void;
  setActivePopKey: (key: string) => void;
  removeFilterOptionSelected: (key: any) => void;
  transformCustomData: (filterKey: string) => {
    [key: string]: any;
  };
  metadata: any;
}

const CategoryFilterComponent: React.FC<CategoryFilterComponentProps> = ({
  filterKey,
  filters,
  supportExcludeFilters,
  supportPartialStringFilters,
  report,
  activePopKey,
  partialFiltersErrors,
  integrationIds,
  handleTimeRangeTypeChange,
  getMappedSupportedFilters,
  handlePartialValueChange,
  onFilterValueChange,
  onExcludeFilterChange,
  handleTimeRangeFilterValueChange,
  setActivePopKey,
  removeFilterOptionSelected,
  transformCustomData,
  fieldTypeList,
  metadata
}) => {
  const metaData = get(filters, ["metadata"], {});

  const isDateTypeField = !!fieldTypeList?.find(
    (field: { key: string; type: string }) => field.key === filterKey && CustomTimeBasedTypes.includes(field.type)
  );

  return (
    <>
      {!(filterKey.includes(CUSTOM_FIELD_PREFIX) || filterKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) &&
        !uiFilterKeys.includes(filterKey) && (
          <APIFilter
            filterData={getMappedSupportedFilters(filterKey)}
            filters={filters}
            supportExcludeFilters={supportExcludeFilters}
            supportPartialStringFilters={supportPartialStringFilters}
            handlePartialValueChange={handlePartialValueChange}
            handleFilterValueChange={onFilterValueChange}
            handleSwitchValueChange={onExcludeFilterChange}
            reportType={report.report}
            activePopkey={activePopKey}
            handleActivePopkey={(filterKey: any) => setActivePopKey(filterKey)}
            handleRemoveFilter={removeFilterOptionSelected}
            useGlobalFilters={true}
            partialFilterError={partialFiltersErrors}
            fieldTypeList={fieldTypeList}
          />
        )}

      {(filterKey.includes(CUSTOM_FIELD_PREFIX) || filterKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) &&
        !uiFilterKeys.includes(filterKey) && (
          <APIFilter
            filterData={transformCustomData(filterKey)}
            filters={filters}
            supportExcludeFilters={supportExcludeFilters}
            supportPartialStringFilters={supportPartialStringFilters}
            handlePartialValueChange={handlePartialValueChange}
            handleFilterValueChange={onFilterValueChange}
            handleSwitchValueChange={onExcludeFilterChange}
            reportType={report.report}
            activePopkey={activePopKey}
            handleActivePopkey={(filterKey: any) => setActivePopKey(filterKey)}
            handleRemoveFilter={removeFilterOptionSelected}
            isCustom={true}
            partialFilterError={partialFiltersErrors}
            fieldTypeList={fieldTypeList}
          />
        )}
      {isDateTypeField && (
        <CustomTimeRangeFilters
          data={[transformCustomData(filterKey) as any]}
          fieldTypeList={fieldTypeList || []}
          filters={filters}
          metaData={metadata}
          onDelete={removeFilterOptionSelected}
          onRangeTypeChange={(key: string, value: any) => handleTimeRangeTypeChange?.(key, value)}
          application={report.application}
          onFilterValueChange={(value: any, type?: any, rangeType?: string, isCustom?: boolean) => {
            handleTimeRangeFilterValueChange?.(value, type, rangeType, isCustom);
          }}
        />
      )}
      {uiFilterKeys.includes(filterKey) &&
        UiFilters(
          filters,
          filterKey,
          report,
          onFilterValueChange,
          removeFilterOptionSelected,
          onExcludeFilterChange,
          handleTimeRangeFilterValueChange,
          metaData,
          handleTimeRangeTypeChange,
          integrationIds
        )}
    </>
  );
};

export default CategoryFilterComponent;
