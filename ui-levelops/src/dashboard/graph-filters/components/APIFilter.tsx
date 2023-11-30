import React, { useCallback, useMemo, useState } from "react";
import { Checkbox, Form, Input } from "antd";
import { bullseyeJobReports, USE_PAGINATED_FILTERS_THRESHOLD } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import {
  DISABLE_PARTIAL_FILTER_SELECT_MAPPING_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  DISABLE_PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { AntIcon, AntText, CustomFormItemLabel, CustomSelect } from "shared-resources/components";
import { debounce, get } from "lodash";
import { getAPIFilterProps } from "dashboard/graph-filters/components/utils/getAPIFilterProps";
import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import APIFilterManyOptions from "./APIFilterManyOptions/APIFilterManyOptions";
import { projectMappingApplications, projectMappingKeys } from "dashboard/constants/applications/names";
import { timeBasedFields } from "./helper";

interface APIFilterComponentProps {
  fromEIProfileFlow?: boolean;
  filterData: any;
  partialFilterError: any;
  supportPartialStringFilters: boolean;
  supportExcludeFilters: boolean;
  filters: any;
  handlePartialValueChange: (key: string, value: any) => void;
  handleSwitchValueChange: (key: string, value: boolean) => void;
  handleFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  hasNext?: boolean;
  reportType: string;
  isCustom?: boolean;
  activePopkey?: string | undefined;
  handleActivePopkey?: (key: string | undefined) => void;
  handleRemoveFilter?: (item: any) => void;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  useGlobalFilters?: boolean;
  fieldTypeList?: { key: string; type: string; name: string }[];
}

const APIFilterComponent: React.FC<APIFilterComponentProps> = props => {
  const debouncedOnPartialChange = debounce(props.handlePartialValueChange, 800);
  const [activeFocusKey, setActiveFocusKey] = useState<string | undefined>();

  const { supportPartialStringFilters, handlePartialValueChange, handleFilterValueChange, reportType, filterData } =
    props;
  const ITEM_TEST_ID = "filter-list-element";

  const widgetConstant = useCallback((key: string) => get(widgetConstants, [reportType, key], undefined), [reportType]);

  const partialFilterKeyMappings = useMemo(() => widgetConstant(PARTIAL_FILTER_MAPPING_KEY) || {}, [reportType]);

  const partialFilterSelectDisableKeys = useMemo(
    () => widgetConstant(DISABLE_PARTIAL_FILTER_SELECT_MAPPING_KEY),
    [reportType]
  );

  const partialFilterDisableKeys = useMemo(() => widgetConstant(DISABLE_PARTIAL_FILTER_MAPPING_KEY), [reportType]);

  const newCheckTextStyle = useMemo(
    () => ({
      color: "#5f5f5f",
      fontSize: "12px",
      marginTop: "0.3rem",
      fontWeight: "500",
      textTransform: "none"
    }),
    []
  );

  const deleteIconStyle = useMemo(
    () => ({
      fontSize: "15px",
      marginLeft: "16px",
      marginTop: "35px"
    }),
    []
  );

  const selectDeleteIconStyle = useMemo(
    () => ({
      fontSize: "15px",
      marginLeft: "16px",
      marginTop: "8px"
    }),
    []
  );
  if (!filterData || Object.keys(filterData).length === 0) return null;

  const apiFilterProps = getAPIFilterProps(filterData, props, partialFilterKeyMappings, props.useGlobalFilters);

  if (!apiFilterProps) return null;

  const { dataKey, selectName, value, options, switchValue, partialValue, withSwitchConfig } = apiFilterProps;

  const helpValue = !!get(props.partialFilterError, [dataKey], undefined) && (
    <AntText style={{ fontSize: "12px" }} type={"danger"}>
      {get(props.partialFilterError, [dataKey], "")}
    </AntText>
  );

  const checkboxProps = {
    text: "Include all values that",
    disabled: switchValue,
    value: Object.keys(partialValue).length > 0
  };

  const disablePartialSelect =
    switchValue || !(Object.keys(partialValue).length > 0) || (partialFilterSelectDisableKeys || []).includes(dataKey);

  const selectProps = {
    options: [
      {
        label: "Start With",
        value: STARTS_WITH
      },
      {
        label: "Contain",
        value: CONTAINS
      }
    ],
    value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : STARTS_WITH,
    disabled: disablePartialSelect,
    onSelectChange: (key: any) =>
      handlePartialValueChange?.(dataKey, key ? { [key]: Object.values(partialValue)[0] || "" } : undefined)
  };

  const switchWithDropdown = {
    showSwitchWithDropdown:
      (supportPartialStringFilters && !(partialFilterDisableKeys || []).includes(dataKey)) ||
      dataKey === "jenkins_job_path" ||
      (bullseyeJobReports.includes(reportType) && dataKey === "name"),
    checkboxProps,
    selectProps
  };

  const shouldUsePaginatedFilters = options.length > USE_PAGINATED_FILTERS_THRESHOLD;
  const applicationType = get(widgetConstants, [reportType, "application"], "");
  const labelCase =
    projectMappingApplications.includes(applicationType) && projectMappingKeys.includes(dataKey)
      ? "none"
      : "title_case";

  if (props.isCustom && timeBasedFields(filterData, props.fieldTypeList)) {
    return null;
  }

  if (shouldUsePaginatedFilters) {
    return (
      <div style={{ display: "flex", marginTop: "24px" }}>
        <APIFilterManyOptions
          key={`api-filter-many-options-${selectName}`}
          data_testId={ITEM_TEST_ID}
          APIFiltersProps={props}
          apiFilterProps={apiFilterProps}
          help={helpValue}
          switchWithDropdown={switchWithDropdown}
          hideSprintFilters={!!props.handleRemoveFilter}
        />
        {props.handleRemoveFilter && (
          <AntIcon
            style={selectDeleteIconStyle}
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              e.preventDefault();
              props.handleRemoveFilter!(filterData);
            }}
          />
        )}
      </div>
    );
  }

  return (
    <Form.Item
      data-testid={ITEM_TEST_ID}
      key={`api-filter-partial-${selectName}`}
      className={"custom-form-item"}
      help={helpValue}
      label={
        <div style={{ display: "flex", width: "100%" }}>
          <CustomFormItemLabel
            label={selectName}
            switchWithDropdown={switchWithDropdown}
            withSwitch={withSwitchConfig}
          />
          {props.handleRemoveFilter && (
            <AntIcon
              style={deleteIconStyle}
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                e.preventDefault();
                props.handleRemoveFilter!(filterData);
              }}
            />
          )}
        </div>
      }>
      {!props.handleRemoveFilter && (selectName || "").toLowerCase() === "sprint" && (
        <div style={{ display: "flex" }}>
          <div className={"flex mb-5"} style={{ alignItems: "center" }}>
            <Checkbox
              className="mr-5"
              checked={
                props.filters.hasOwnProperty("sprint_states") && (props.filters["sprint_states"] || []).length > 0
              }
              onChange={event => {
                props.handleFilterValueChange(event.target.checked, "sprint_states");
              }}>
              <AntText style={newCheckTextStyle} className="action-text-select">
                Includes active sprints only
              </AntText>
            </Checkbox>
          </div>
          <div>
            <Checkbox
              className="mr-5"
              checked={props.filters.hasOwnProperty("last_sprint") && props.filters["last_sprint"]}
              onChange={event =>
                props.handleLastSprintChange && props.handleLastSprintChange(event.target.checked, dataKey)
              }>
              <AntText style={newCheckTextStyle}>Filter by last sprint</AntText>
            </Checkbox>
          </div>
        </div>
      )}

      {Object.keys(partialValue).length > 0 ? (
        <Input
          type="tag"
          data-testid="filter-list-element-input"
          autoFocus={activeFocusKey === dataKey}
          onFocus={() => setActiveFocusKey(dataKey)}
          onBlur={() => setActiveFocusKey(undefined)}
          placeholder={"Case Sensitive"}
          defaultValue={Object.values(partialValue)[0] as any}
          onChange={e => debouncedOnPartialChange(dataKey, { [Object.keys(partialValue)[0]]: e.target.value })}
        />
      ) : (
        <CustomSelect
          dataTestid="filter-list-element-select"
          valueKey={props.isCustom ? "value" : "key"}
          labelKey={props.isCustom ? "label" : options && options[0] && options[0].value ? "value" : "key"}
          createOption={props.isCustom !== true}
          labelCase={labelCase}
          options={options}
          mode={"multiple"}
          showArrow={true}
          value={value}
          truncateOptions={true}
          truncateValue={USE_PAGINATED_FILTERS_THRESHOLD}
          sortOptions
          dropdownVisible={true}
          onChange={(value: any) => {
            handleFilterValueChange(value, dataKey, switchValue);
          }}
        />
      )}
    </Form.Item>
  );
};

export default APIFilterComponent;
