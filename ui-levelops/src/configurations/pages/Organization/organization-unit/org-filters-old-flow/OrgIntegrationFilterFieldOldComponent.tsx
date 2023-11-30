import React, { useMemo } from "react";
import { Icon } from "antd";
import {
  filterFieldType,
  filterOptionConfig,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { AntButton, AntInput, AntSelect, CustomSelect } from "shared-resources/components";
import {
  integrationParameters,
  ouIntegrationParameters,
  oUIntegrationPartialMatchParameters,
  partialMatchBlockedFilters
} from "configurations/constants";
import { ITEM_TEST_ID } from "dashboard/graph-filters/components/Constants";
import "../OrgUnitIntegrationFilterField.style.scss";
import { cloneDeep, difference } from "lodash";
import CustomTreeSelectComponent from "shared-resources/components/custom-tree-select-component/custom-tree-select-component";
import TimeRangeFilter from "../components/TimeRangeFilter";

const OrgUnitIntegrationFilterFieldOldComponent: React.FC<{
  apiRecords: filterOptionConfig[];
  apiLoading: boolean;
  index: number;
  fieldLabel?: string;
  field: sectionSelectedFilterType;
  handleRemoveFilter: (index: number) => void;
  handleFieldChange: (type: filterFieldType, index: number, value: any) => void;
  integrationApplication?: string;
}> = ({
  index,
  field,
  apiRecords,
  fieldLabel,
  apiLoading,
  handleFieldChange,
  handleRemoveFilter,
  integrationApplication
}) => {
  const getValueOptions = useMemo(() => {
    return apiRecords.find(obj => obj.value === field.key)?.options || [];
  }, [field, apiRecords]);

  const handleRemove = (index: number) => {
    handleRemoveFilter(index);
  };

  const integrationParamOptions = useMemo(() => {
    if (field.key === "sprint") {
      return ouIntegrationParameters.filter(param => oUIntegrationPartialMatchParameters.includes(param.value));
    }
    if (partialMatchBlockedFilters.includes(`${integrationApplication}_${field?.key}`)) {
      return ouIntegrationParameters.filter(param => !oUIntegrationPartialMatchParameters.includes(param.value));
    }
    return ouIntegrationParameters;
  }, [field]);

  const preventDefault = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleAzureAreasSelectChange = (event: string[]) => {
    let value = cloneDeep(event);
    const oldFiltersCodeArea = field?.value;
    const removed = difference(oldFiltersCodeArea, value);
    value = value.filter((_v: string) => !_v.startsWith(removed[0]));
    return handleFieldChange("value", index, value);
  };

  const renderValueField = useMemo(() => {
    const value =
      field?.value === "" ||
      field?.value?.hasOwnProperty("$lt") ||
      field?.value?.hasOwnProperty("$gt") ||
      field?.value?.hasOwnProperty("$age")
        ? undefined
        : field?.value;

    const select = (
      <CustomSelect
        dataFilterNameDropdownKey={`${ITEM_TEST_ID}-filter-match-select`}
        className="filter-col-select"
        showArrow={true}
        mode="multiple"
        createOption={false}
        labelCase="none"
        labelKey={"label"}
        valueKey={"value"}
        options={getValueOptions}
        value={value}
        onChange={(value: string) => handleFieldChange("value", index, value)}
      />
    );

    const input = (
      <AntInput value={field?.value || ""} onChange={(e: any) => handleFieldChange("value", index, e.target.value)} />
    );

    const customTreeSelect = (
      <AntSelect
        mode="multiple"
        className="azure-areas-select"
        value={value}
        onChange={handleAzureAreasSelectChange}
        dropdownRender={() => (
          <div className="customTree-component-wrapper">
            <div onMouseDown={preventDefault}>
              <CustomTreeSelectComponent
                data={getValueOptions}
                selected={value}
                returnAllValues={true}
                createParentChildRelationInData={true}
                onCheckboxValueChange={(value: string) => handleFieldChange("value", index, value)}
              />
            </div>
          </div>
        )}
      />
    );

    switch (field?.param) {
      case integrationParameters.IS_BETWEEN:
        return null;
      case integrationParameters.STARTS_WITH:
      case integrationParameters.CONTAINS:
        return input;
      default:
        if (["code_area"].includes(field?.key)) {
          return customTreeSelect;
        }
        return select;
    }
  }, [field, apiRecords]);

  return (
    <div className="filter-field-container" key={index}>
      <div className="filter-row">
        <div className="select-content">
          <span className="text">{fieldLabel || "FILTER :"}</span>
          <div className="filter-col">
            <CustomSelect
              dataFilterNameDropdownKey={`${ITEM_TEST_ID}-filter-select`}
              className="filter-col-select"
              mode="default"
              createOption={false}
              labelCase="none"
              labelKey={"label"}
              valueKey={"value"}
              loading={apiLoading}
              disabled={apiLoading}
              options={apiRecords}
              value={field?.key}
              onChange={(value: string) => handleFieldChange("key", index, value)}
            />
          </div>
          <div className="filter-col">
            <CustomSelect
              dataFilterNameDropdownKey={`${ITEM_TEST_ID}-filter-value-select`}
              className="filter-col-select"
              mode="default"
              createOption={false}
              labelCase="none"
              labelKey={"label"}
              valueKey={"value"}
              options={integrationParamOptions}
              value={field?.param}
              onChange={(value: string) => handleFieldChange("param", index, value)}
            />
          </div>
          <div className="filter-col">{renderValueField}</div>
        </div>
        <div className="action-filter-col">
          <AntButton
            onClick={(e: any) => {
              handleRemove(index);
            }}>
            <Icon type="delete" />
          </AntButton>
        </div>
      </div>
      {field?.param === integrationParameters.IS_BETWEEN && (
        <div className="time-filter-row">
          <TimeRangeFilter
            onFilterValueChange={(value: any) => handleFieldChange("value", index, value)}
            value={field?.value}
          />
        </div>
      )}
    </div>
  );
};

export default OrgUnitIntegrationFilterFieldOldComponent;
