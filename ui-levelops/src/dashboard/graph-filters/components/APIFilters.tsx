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
import { debounce, get, map } from "lodash";
import { getAPIFilterProps } from "dashboard/graph-filters/components/utils/getAPIFilterProps";
import { STARTS_WITH, CONTAINS, REGEX } from "dashboard/constants/constants";
import APIFilterManyOptions from "./APIFilterManyOptions/APIFilterManyOptions";
import { projectMappingApplications, projectMappingKeys } from "../../constants/applications/names";
import { timeBasedFields } from "./helper";
import { ITEM_TEST_ID } from "./Constants";

interface APIFiltersComponentProps {
  data: Array<any>;
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
  fieldTypeList?: { key: string; type: string; name: string }[];
  customProps?: Record<any, any>;
}

const FILTER_KEYS_TO_PRESERVE_STRING_CASE = ["sprint_report", "file_type"];

const APIFiltersComponent: React.FC<APIFiltersComponentProps> = props => {
  const debouncedOnPartialChange = debounce(props.handlePartialValueChange, 800);
  const [activeFocusKey, setActiveFocusKey] = useState<string | undefined>();

  const { supportPartialStringFilters, handlePartialValueChange, handleFilterValueChange, reportType } = props;

  const widgetConstant = useCallback((key: string) => get(widgetConstants, [reportType, key], undefined), [reportType]);

  const partialFilterKeyMappings = useMemo(() => widgetConstant(PARTIAL_FILTER_MAPPING_KEY) || {}, [reportType]);

  const partialFilterSelectDisableKeys = useMemo(
    () => widgetConstant(DISABLE_PARTIAL_FILTER_SELECT_MAPPING_KEY),
    [reportType]
  );

  const partialFilterDisableKeys = useMemo(() => widgetConstant(DISABLE_PARTIAL_FILTER_MAPPING_KEY), [reportType]);

  const widgetValuesToFilterMapping = useMemo(() => widgetConstant("valuesToFilters"), [reportType]);

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

  const applicationType = get(widgetConstants, [reportType, "application"], "");

  return (
    <>
      {map(props.data, (item: any, index) => {
        if (item) {
          //BE not support time based custom field for zendesk application
          if (props.isCustom && timeBasedFields(item, props.fieldTypeList) && !["zendesk"].includes(applicationType)) {
            return null;
          }

          const apiFilterProps = getAPIFilterProps(
            item,
            props,
            partialFilterKeyMappings,
            false,
            widgetValuesToFilterMapping
          );

          if (!apiFilterProps) return null;
          const { dataKey, selectName, value, options, switchValue, partialValue, withSwitchConfig } = apiFilterProps;
          const labelCase =
            (projectMappingApplications.includes(applicationType) && projectMappingKeys.includes(dataKey)) ||
            FILTER_KEYS_TO_PRESERVE_STRING_CASE.includes(dataKey)
              ? "none"
              : "title_case";
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
            switchValue ||
            !(Object.keys(partialValue).length > 0) ||
            (partialFilterSelectDisableKeys || []).includes(dataKey);

          let selectPropOptions = [
            {
              label: "Start With",
              value: STARTS_WITH
            },
            {
              label: "Contain",
              value: CONTAINS
            }
          ];

          // Sprint goal only support Contains option
          if (["sprint_goal"].includes(reportType)) {
            selectPropOptions = [
              {
                label: "Contain",
                value: CONTAINS
              }
            ];
          }
          // New regex support
          if (props?.customProps?.showRegex) {
            selectPropOptions = [
              ...selectPropOptions,
              {
                label: "Regex",
                value: REGEX
              }
            ];
          }

          const selectProps = {
            options: selectPropOptions,
            value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : selectPropOptions[0].value,
            disabled: disablePartialSelect,
            onSelectChange: (key: any) =>
              handlePartialValueChange?.(dataKey, key ? { [key]: Object.values(partialValue)[0] || "" } : undefined)
          };

          const switchWithDropdown = {
            showSwitchWithDropdown:
              (supportPartialStringFilters && !(partialFilterDisableKeys || []).includes(dataKey)) ||
              dataKey === "job_normalized_full_name" ||
              (bullseyeJobReports.includes(reportType) && dataKey === "name"),
            checkboxProps,
            selectProps
          };

          // Note: hiding it till BE support partial support in lead time profile. customProps is only coming from lead time profile component.
          if (props?.customProps?.hidePartialSelect) {
            switchWithDropdown.showSwitchWithDropdown = false;
          }
          const shouldUsePaginatedFilters = options.length > USE_PAGINATED_FILTERS_THRESHOLD;

          if (
            (shouldUsePaginatedFilters || selectName.toLowerCase() === "azure iteration") &&
            !["sprint_goal"].includes(reportType)
          ) {
            return (
              <div key={`api-filters-container-${index}`} style={{ display: "flex", marginTop: "24px" }}>
                <APIFilterManyOptions
                  key={`api-filter-many-options-${index}`}
                  data_testId={ITEM_TEST_ID}
                  APIFiltersProps={props}
                  apiFilterProps={apiFilterProps}
                  help={helpValue}
                  switchWithDropdown={switchWithDropdown}
                  hideSprintFilters={!!props.handleRemoveFilter}
                  createOption={true}
                  reportType={props.reportType}
                />
                {props.handleRemoveFilter && (
                  <AntIcon
                    key={`api-filter-icon-${index}`}
                    style={selectDeleteIconStyle}
                    type={"delete"}
                    onClick={e => {
                      e.stopPropagation();
                      props.handleRemoveFilter!(item);
                    }}
                  />
                )}
              </div>
            );
          }

          const dataFilterNameKey = `${ITEM_TEST_ID}-${(selectName || "").split(" ").join("-")}`;
          const dataFilterNameDropdownKey = `${ITEM_TEST_ID}-${(selectName || "").split(" ").join("-")}_dropdown`;

          return (
            <Form.Item
              data-testid={ITEM_TEST_ID}
              data-filterselectornamekey={dataFilterNameKey}
              data-filtervaluesnamekey={dataFilterNameKey}
              key={`api-filter-partial-${index}`}
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
                        props.handleRemoveFilter!(item);
                      }}
                    />
                  )}
                </div>
              }>
              {!props.handleRemoveFilter &&
                (selectName || "").toLowerCase() === "sprint" &&
                !["sprint_goal"].includes(reportType) && (
                  <div style={{ display: "flex" }}>
                    <div className={"flex mb-5"} style={{ alignItems: "center" }}>
                      <Checkbox
                        className="mr-5"
                        checked={
                          props.filters.hasOwnProperty("jira_sprint_states") &&
                          (props.filters["jira_sprint_states"] || []).length > 0
                        }
                        onChange={event => {
                          props.handleFilterValueChange(event.target.checked, "jira_sprint_states");
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
              {!props.handleRemoveFilter &&
                (selectName || "").toLowerCase() === "sprint" &&
                ["sprint_goal"].includes(reportType) && (
                  <div style={{ display: "flex" }}>
                    <div className={"flex mb-5"} style={{ alignItems: "center" }}>
                      <Checkbox
                        className="mr-5"
                        checked={props.filters.hasOwnProperty("state") && (props.filters["state"] || []).length > 0}
                        onChange={event => {
                          props.handleFilterValueChange(event.target.checked, "active_sprint_state");
                        }}>
                        <AntText style={newCheckTextStyle} className="action-text-select">
                          Includes active sprints only
                        </AntText>
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
                  dataFilterNameDropdownKey={dataFilterNameDropdownKey}
                  valueKey={props.isCustom ? "value" : "key"}
                  labelKey={props.isCustom ? "label" : options && options[0] && options[0].value ? "value" : "key"}
                  createOption={true}
                  labelCase={labelCase}
                  options={options}
                  mode={!["sprint_goal"].includes(reportType) ? "multiple" : "default"}
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
        }
      })}
    </>
  );
};

export default APIFiltersComponent;
