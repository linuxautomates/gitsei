import { Button, Empty, Icon, Popover } from "antd";
import React, { useCallback } from "react";
import { AntText, IntegrationIcon } from "shared-resources/components";
import { ReactNode } from "react";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  issueManagementToIntegrationLabelMapping,
  IssueManagementType
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

interface CategoryFiltersComponentWrapperProps {
  application: string;
  showFiltersPopOver: boolean;
  filters: any;
  orderedFilters: string[];
  partialFiltersErrors: any;
  activePopkey: string | undefined;
  handleActivePopKey: (key: string | undefined) => void;
  integrationIds: Array<string>;
  Menu: ReactNode;
  allFilterConfig: LevelOpsFilter[];
  transformedCustomFieldRecords: any[];
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string, isCustom?: boolean) => void;
  removeFilterOptionSelected: (key: any) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleVisibleChange: (visible: any) => void;
  handleSetShowFiltersPopOver: (e: any) => void;
  metadata: any;
}

const CategoryFiltersComponentWrapper: React.FC<CategoryFiltersComponentWrapperProps> = (
  props: CategoryFiltersComponentWrapperProps
) => {
  const {
    showFiltersPopOver,
    filters,
    orderedFilters,
    allFilterConfig,
    integrationIds,
    Menu,
    handleSetShowFiltersPopOver,
    handleVisibleChange,
    onFilterValueChange,
    removeFilterOptionSelected,
    metadata,
    transformedCustomFieldRecords,
    application,
    activePopkey,
    handleActivePopKey
  } = props;

  const renderValueField = (selectedFilter: string) => {
    const filterConfig = (allFilterConfig || []).find(
      (filterItem: LevelOpsFilter) => filterItem.beKey === selectedFilter
    );
    if (filterConfig) {
      return React.createElement(filterConfig.apiContainer ?? filterConfig.renderComponent, {
        filterProps: {
          ...filterConfig,
          allFilters: {
            ...(filters ?? {}),
            ...(metadata ?? {})
          },
          filterMetaData: {
            ...filterConfig?.filterMetaData,
            integration_ids: integrationIds,
            customFieldsRecords: transformedCustomFieldRecords
          },
          metadata
        },
        handleRemoveFilter: removeFilterOptionSelected,
        handleFieldChange: onFilterValueChange,
        activePopKey: activePopkey,
        handleActivePopkey: handleActivePopKey,
        ...props
      });
    }
    return <></>;
  };

  return (
    <div className="add_filters_component_container">
      <div className="add_filters_component_container_header p-1">
        <div className={"add_filters_component_container_header_title"}>
          {application && (
            // @ts-ignore
            <IntegrationIcon style={{ marginRight: "5px" }} type={application} />
          )}
          <AntText>{issueManagementToIntegrationLabelMapping[application as IssueManagementType]}</AntText>
        </div>
        <div>
          <span className={"report_category_dropdown"}>
            <Popover
              className={"search-popover"}
              placement={"bottomLeft"}
              content={Menu}
              trigger="click"
              visible={showFiltersPopOver}
              onVisibleChange={handleVisibleChange}>
              <Button
                className={"add_filters_component_container_header_dropdown"}
                onClick={handleSetShowFiltersPopOver}>
                Add Filter <Icon type="down" />
              </Button>
            </Popover>
          </span>
        </div>
      </div>
      <div className={"add_filters_component_container_list"}>
        <div>{(orderedFilters || []).map((key: string) => renderValueField(key))}</div>
        {(orderedFilters || []).length === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
      </div>
    </div>
  );
};

export default CategoryFiltersComponentWrapper;
