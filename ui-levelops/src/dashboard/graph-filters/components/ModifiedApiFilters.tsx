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
import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import APIFilterManyOptions from "./APIFilterManyOptions/APIFilterManyOptions";
import { projectMappingApplications, projectMappingKeys } from "../../constants/applications/names";
import { timeBasedFields } from "./helper";
import { getModifiedApiFiltersProps } from "./utils/getModifiedApiFilterProps";
import { SUPPORTED_FILTERS_WITH_INFO } from "dashboard/constants/filter-name.mapping";

interface ModifiedApiFiltersComponentProps {
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
}

const FILTER_KEYS_TO_PRESERVE_STRING_CASE = ["sprint_report", "file_type"];

const ModifiedApiFiltersComponent: React.FC<ModifiedApiFiltersComponentProps> = props => {
  const debouncedOnPartialChange = debounce(props.handlePartialValueChange, 800);
  const [activeFocusKey, setActiveFocusKey] = useState<string | undefined>();

  const { supportPartialStringFilters, handlePartialValueChange, handleFilterValueChange, reportType } = props;

  const ITEM_TEST_ID = "filter-list-element";

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
          if (props.isCustom && timeBasedFields(item, props.fieldTypeList)) {
            return null;
          }

          const apiFilterProps = getModifiedApiFiltersProps(
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
              dataKey === "job_normalized_full_name" ||
              (bullseyeJobReports.includes(reportType) && dataKey === "name"),
            checkboxProps,
            selectProps
          };

          const shouldUsePaginatedFilters = options.length > USE_PAGINATED_FILTERS_THRESHOLD;

          if (shouldUsePaginatedFilters || selectName.toLowerCase() === "azure iteration") {
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
                  reportType={reportType}
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

          return (
            <Form.Item
              data-testid={ITEM_TEST_ID}
              key={`api-filter-partial-${index}`}
              className={"custom-form-item"}
              help={helpValue}
              label={
                <CustomFormItemLabel
                  label={selectName}
                  withInfo={{
                    showInfo: get(widgetConstants, [reportType, SUPPORTED_FILTERS_WITH_INFO, dataKey], false),
                    description: get(widgetConstants, [reportType, SUPPORTED_FILTERS_WITH_INFO, dataKey, "info"], "")
                  }}
                  required={get(widgetConstants, [reportType, "requiredSupportedFilter", dataKey], false)}
                />
              }>
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
                  createOption={true}
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
        }
      })}
    </>
  );
};

export default ModifiedApiFiltersComponent;
