import React, { useMemo, useState } from "react";
import { Icon } from "antd";
import {
  filterFieldType,
  filterOptionConfig,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { AntButton, AntInput, AntSelect, CustomSelect } from "shared-resources/components";
import {
  equalMatchBlockedFilters,
  integrationParameters,
  oUIntegrationEqualsMatchParameters,
  ouIntegrationParameters,
  oUIntegrationPartialMatchParameters,
  partialMatchBlockedFilters,
  partialMatchBlockedKeys,
  workflowProfilePartialMatchBlocker
} from "configurations/constants";
import TimeRangeFilter from "./components/TimeRangeFilter";
import { ITEM_TEST_ID } from "dashboard/graph-filters/components/Constants";
import "./OrgUnitIntegrationFilterField.style.scss";
import { cloneDeep, difference, uniq } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";

const OrgUnitIntegrationFilterField: React.FC<{
  apiRecords: filterOptionConfig[];
  apiLoading: boolean;
  index: number;
  readonly?: boolean;
  fieldLabel?: string;
  field: sectionSelectedFilterType;
  handleRemoveFilter: (index: number, parentKeyData?: any) => void;
  handleFieldChange: (type: filterFieldType, index: number, value: any, parentKeyData?: any) => void;
  integrationApplication?: string;
  allFiltersConfig?: any[];
  integrationIds?: string[];
  hideFilterText?: boolean;
  addFilter?: () => void;
  labelCase?: string;
  integrationType?: string;
  pageName?: string;
  additionalFilter?: any;
  childComponent?: boolean;
  parentKeyData?: any;
}> = ({
  index,
  field,
  apiRecords,
  fieldLabel,
  apiLoading,
  readonly,
  handleFieldChange,
  handleRemoveFilter,
  integrationApplication,
  allFiltersConfig,
  integrationIds,
  hideFilterText,
  addFilter,
  labelCase,
  integrationType,
  pageName,
  additionalFilter,
  childComponent,
  parentKeyData
}) => {
  const [selectedFilter, setSelectedFilter] = useState<string>(field?.key);
  const getValueOptions = useMemo(() => {
    return apiRecords.find(obj => obj.value === field.key)?.options || [];
  }, [field, apiRecords]);

  const handleRemove = (index: number) => {
    handleRemoveFilter(index, parentKeyData);
  };

  const integrationParamOptions = useMemo(() => {
    if (field.key === "sprint") {
      return ouIntegrationParameters.filter(param => oUIntegrationPartialMatchParameters.includes(param.value));
    }
    //MATCH ONLY EQUALS & DOSE NOT EQUAL
    if (
      partialMatchBlockedFilters.includes(`${integrationApplication}_${field?.key}`) ||
      (pageName && workflowProfilePartialMatchBlocker.includes(`${integrationApplication}_${field?.key}`)) ||
      (pageName && partialMatchBlockedKeys.includes(`${integrationApplication}_${field?.key}_${integrationType}`))
    ) {
      return ouIntegrationParameters.filter(param => !oUIntegrationPartialMatchParameters.includes(param.value));
    }
    //MATCH ONLY EQUALS CONDTION
    if (equalMatchBlockedFilters.includes(`${integrationApplication}_${field?.key}`)) {
      return ouIntegrationParameters.filter(param => oUIntegrationEqualsMatchParameters.includes(param.value));
    }
    const filterConfig = (allFiltersConfig || []).find((filterItem: LevelOpsFilter) => filterItem.beKey === field.key);

    if (filterConfig && CustomTimeBasedTypes.includes(filterConfig?.BEType)) {
      return [
        ...ouIntegrationParameters,
        {
          label: "Is Between",
          value: integrationParameters.IS_BETWEEN
        }
      ];
    }

    return ouIntegrationParameters;
  }, [field, allFiltersConfig]);

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

  const handleFieldChangeChildFilter = (type: filterFieldType, index: number, value: any) => {
    return handleFieldChange(type, index, value, parentKeyData);
  };

  const renderValueField = useMemo(() => {
    /**
     * two possible data structures for field.value
     * 1.
     *   value = [ el, el, el, ... ]
     * 2.
     *   value = [
     *     {
     *       value: [ el, el, el, ... ]
     *     }
     *   ]
     * 3. value = ""
     */

    let values = [];
    if (field?.value.length > 0 && typeof field?.value[0] === "object") {
      if ("values" in field?.value[0]) {
        values = field?.value[0]["values"];
      }
    } else {
      values = field?.value;
    }

    const value =
      values.hasOwnProperty("$lt") || values.hasOwnProperty("$gt") || values.hasOwnProperty("$age") || values === ""
        ? undefined
        : values;

    let select;
    const filterConfig = (allFiltersConfig || []).find(
      (filterItem: LevelOpsFilter) => filterItem.beKey === selectedFilter
    );
    if (filterConfig) {
      select = React.createElement(filterConfig.apiContainer ?? filterConfig.renderComponent, {
        filterProps: {
          ...filterConfig,
          allFilters: {
            ...field
          },
          filterMetaData: {
            ...filterConfig?.filterMetaData,
            additionalFilter: additionalFilter || {},
            integration_ids: integrationIds,
            customFieldsRecords: []
          },
          labelCase: labelCase ?? filterConfig.labelCase,
          disabled: readonly
        },
        handleRemoveFilter,
        index: index,
        handleFieldChange,
        handleAzureAreasSelectChange
      });
    } else if (childComponent && parentKeyData) {
      const filterConfigChild = (allFiltersConfig || []).find(
        (filterItem: LevelOpsFilter) => filterItem.beKey === parentKeyData?.key
      );

      let childComponentFilter = parentKeyData?.value ? { [parentKeyData?.key]: parentKeyData.value } : {};

      let childComponentArr = filterConfigChild?.renderChildComponent(selectedFilter);
      select = React.createElement(childComponentArr.apiContainer ?? childComponentArr.renderComponent, {
        filterProps: {
          ...childComponentArr,
          allFilters: {
            ...field
          },
          filterMetaData: {
            ...childComponentArr?.filterMetaData,
            additionalFilter: additionalFilter || {},
            integration_ids: integrationIds,
            customFieldsRecords: [],
            childComponentFilter: childComponentFilter,
            callApiOnParentValueChange: true
          },
          labelCase: labelCase ?? childComponentArr.labelCase,
          disabled: readonly
        },
        handleRemoveFilter,
        index: index,
        handleFieldChange : handleFieldChangeChildFilter,
      });
      
    } else {
      select = (
        <div className="customSelect-width">
          <CustomSelect
            dataFilterNameDropdownKey={`${ITEM_TEST_ID}-filter-match-select`}
            className="filter-col-select"
            showArrow={true}
            mode="multiple"
            createOption={false}
            disabled={readonly}
            labelCase="none"
            labelKey={"label"}
            valueKey={"value"}
            options={getValueOptions}
            value={value}
            onChange={(value: string) => handleFieldChange("value", index, value)}
            placeholder="select value"
          />
        </div>
      );
    }

    const input = (
      <div className="customSelect-width">
        <AntInput
          value={values || ""}
          onChange={(e: any) => handleFieldChange("value", index, e.target.value)}
          placeholder="Enter value"
          disabled={readonly}
        />
      </div>
    );

    switch (field?.param) {
      case integrationParameters.IS_BETWEEN:
        return null;
      case integrationParameters.STARTS_WITH:
      case integrationParameters.CONTAINS:
        return input;
      default:
        return select;
    }
  }, [field, apiRecords, selectedFilter, additionalFilter, readonly]);

  return (
    <div className={"filter-field-container " + (childComponent ? 'child-filter-add-button' : '')} key={index}>
      <div className="filter-row">
        <div className="select-content">
          {!hideFilterText && <span className="text">{fieldLabel || "FILTER :"}</span>}
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
              disabled={apiLoading || readonly}
              options={apiRecords}
              value={field?.key || undefined}
              onChange={(value: string) => handleFieldChange("key", index, value, parentKeyData)}
              placeholder="select filter"
            />
          </div>
          <div className="filter-col">
            <CustomSelect
              dataFilterNameDropdownKey={`${ITEM_TEST_ID}-filter-value-select`}
              className="filter-col-select"
              mode="default"
              createOption={false}
              labelCase="none"
              disabled={readonly || !field?.key}
              labelKey={"label"}
              valueKey={"value"}
              options={integrationParamOptions}
              value={field?.param || undefined}
              onChange={(value: string) => {
                if ([integrationParameters.EQUALS, integrationParameters.DOES_NOT_EQUAL].includes(value as any)) {
                  setSelectedFilter(field.key);
                }
                handleFieldChange("param", index, value, parentKeyData);
              }}
              placeholder="select condition"
            />
          </div>
          <div className="filter-value-col">{renderValueField}</div>
        </div>
        <div className="action-filter-col">
          {addFilter && (
            <AntButton onClick={addFilter}>
              <Icon type="filter" />
            </AntButton>
          )}
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

export default OrgUnitIntegrationFilterField;
