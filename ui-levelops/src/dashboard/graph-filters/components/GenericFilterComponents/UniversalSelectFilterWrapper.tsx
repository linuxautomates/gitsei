import { Form, Input } from "antd";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  USE_PAGINATED_FILTERS_THRESHOLD
} from "dashboard/constants/constants";
import { getFilterValue } from "helper/widgetFilter.helper";
import { debounce, get, isArray, uniqBy } from "lodash";
import { ApiDropDownData, APIFilterConfigType, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { customTimeFilterKeysSelector } from "reduxConfigs/selectors/jira.selector";
import { AntText, CustomSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { showInfoProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { toTitleCase } from "utils/stringUtils";
import APIFilterManyOptions from "../APIFilterManyOptions/APIFilterManyOptions";
import {
  ITEM_TEST_ID,
  RESOLUTION_TIME_INCOMPATIBLE_FILTERS,
  RESOLUTION_TIME_INCOMPATIBLE_XAXIS,
  SCM_PRS_REPORT_STACK_DISABLED_MSG,
  TESTRAIL_INCOMPATIBLE_FILTERS
} from "../Constants";
import {
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  SCM_REPORTS,
  TESTRAILS_REPORTS
} from "dashboard/constants/applications/names";

export interface UniversalFilterWrapperProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (
    value: any,
    type?: any,
    exclude?: boolean,
    addToMetaData?: any,
    returnUpdatedQuery?: any,
    customEpics?: any
  ) => void;
  onExcludeFilterChange: (key: string, value: boolean, selectMode?: string) => void;
  handlePartialValueChange: (key: string, value: any, otherFlagData?: any) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string) => void;
  handleRemoveFilter: (key: string) => void;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  onChildFilterRemove?: (filterpayload: any, returnUpdatedQuery?: boolean | undefined) => any;
  handleMetadataChange?: (key: any, value: any) => void;
  activePopKey?: string;
  handleActivePopkey?: (key: string | undefined) => void;
  onModifiedFilterValueChange?: (args: any) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  report?: string;
  partialFiltersErrors?: any;
}

const UniversalSelectFilterWrapper: React.FC<UniversalFilterWrapperProps> = props => {
  const {
    filterProps,
    onFilterValueChange,
    onModifiedFilterValueChange,
    onChildFilterRemove,
    activePopKey,
    handleActivePopkey,
    handleMetadataChange,
    report,
    onAggregationAcrossSelection
  } = props;
  const {
    labelCase,
    label,
    beKey,
    filterMetaData,
    allFilters,
    disabled,
    required,
    defaultValue,
    filterInfo,
    apiFilterProps,
    partialFilterKey,
    partialKey,
    supportPaginatedSelect,
    updateInWidgetMetadata,
    getMappedValue,
    hideFilter,
    subtitle,
    placeholder,
    modifiedFilterValueChange,
    modifiedFilterRemove,
    modifyFilterValue
  } = filterProps;
  const {
    options,
    selectMode,
    reportType,
    sortOptions,
    createOption,
    clearSupport,
    alwaysExclude,
    mapFilterValueForBE,
    customEpics,
    selectModeFunction
  } = filterMetaData as DropDownData;

  const [activeFocusKey, setActiveFocusKey] = useState<string | undefined>();
  const debouncedOnPartialChange = debounce(props.handlePartialValueChange, 800);

  const dataFilterNameKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}`;
  const dataFilterNameDropdownKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}_dropdown`;

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        report: props.report,
        handleSwitchValueChange: props.onExcludeFilterChange,
        handlePartialValueChange: props.handlePartialValueChange,
        handleRemoveFilter: props.handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const value = getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, beKey, true)?.value ?? defaultValue;
  const customTimeFilterKeys: Array<any> = useSelector(customTimeFilterKeysSelector);

  const partialValue = useMemo(() => {
    let partialDataValue = get(allFilters, [partialFilterKey ?? "partial_match", partialKey ?? beKey], {});
    if (Object.keys(partialDataValue).length === 0 && apiFilters?.allowExcludeWithPartialMatch) {
      partialDataValue = get(allFilters, ["exclude", "partial_match", partialKey ?? beKey], {});
    }
    return partialDataValue;
  }, [allFilters, apiFilters]);

  const isDisabled = useMemo(() => {
    if (typeof disabled === "boolean") return disabled;
    if (disabled instanceof Function) return disabled({ filters: allFilters });
    return false;
  }, [disabled, allFilters]);

  const isRequired = useMemo(() => {
    if (typeof required === "boolean") return required;
    if (required instanceof Function) return required({ filters: allFilters });
    return false;
  }, [required, allFilters]);

  const isFilterHidden = useMemo(() => {
    if (typeof hideFilter === "boolean") return hideFilter;
    if (hideFilter instanceof Function) return hideFilter({ filters: allFilters });
    return false;
  }, [hideFilter, allFilters]);

  const getOptions = useMemo(() => {
    let _options: Array<{ label: string; value: string | number }> = [];
    if (isArray(options)) _options = options;
    if (options instanceof Function) _options = options({ ...filterProps, customTimeFilterKeys });
    return uniqBy(_options, "value");
  }, [filterProps, options]);

  const getSelecteMode = useMemo(() => {
    if (selectModeFunction instanceof Function) return selectModeFunction({ filters: allFilters, report });
    return selectMode;
  }, [filterProps, selectMode, selectModeFunction]);

  const helpValue = !!get(props.partialFiltersErrors, [partialKey ?? beKey], undefined) && (
    <AntText style={{ fontSize: "12px" }} type={"danger"}>
      {get(props.partialFiltersErrors, [partialKey ?? beKey], "")}
    </AntText>
  );

  const filterInfoConfig: showInfoProps = useMemo(() => {
    if (typeof filterInfo === "function")
      return {
        showInfo: !!filterInfo({ filters: allFilters }),
        description: filterInfo({ filters: allFilters }) || ""
      };
    return { showInfo: !!filterInfo, description: filterInfo || "" };
  }, [filterInfo, allFilters]);

  const onFilterRemove = (key: string) => {
    if (!!modifiedFilterRemove) {
      modifiedFilterRemove({ onChildFilterRemove, key });
      return;
    }
    props.handleRemoveFilter(key);
  };

  const mappedApiFilterProps = () => {
    return {
      dataKey: beKey,
      selectName: label,
      value: Object.keys(partialValue).length > 0 ? undefined : value,
      options: getOptions,
      withSwitchConfig: apiFilters.withSwitch,
      partialValue,
      switchValue: !!apiFilters.withSwitch?.switchValue,
      partialKey
    };
  };

  const APIFiltersProps = () => {
    return {
      filters: allFilters,
      handleFilterValueChange: props.onFilterValueChange,
      handleLastSprintChange: props.handleLastSprintChange,
      handlePartialValueChange: props.handlePartialValueChange,
      activePopkey: activePopKey,
      handleActivePopkey,
      handleRemoveFilter: props.handleRemoveFilter,
      isCustom: beKey.includes(CUSTOM_FIELD_PREFIX) || beKey.includes(AZURE_CUSTOM_FIELD_PREFIX),
      allowExcludeWithPartialMatch: apiFilters?.allowExcludeWithPartialMatch
    };
  };

  const onChange = (value: any) => {
    if (!!modifyFilterValue) {
      modifyFilterValue({ value, beKey, onAggregationAcrossSelection, allFilters });
      return;
    }
    if (!!modifiedFilterValueChange) {
      modifiedFilterValueChange({ value, onModifiedFilterValueChange });
      return;
    }
    if (updateInWidgetMetadata) {
      handleMetadataChange?.(value, beKey);
      return;
    }
    if (beKey === "across") {
      onAggregationAcrossSelection?.(value);
      return;
    }
    const exclude = !!(apiFilters?.withSwitch?.switchValue ?? alwaysExclude);
    let _val = value;
    if (mapFilterValueForBE) {
      _val = mapFilterValueForBE(value);
    }
    onFilterValueChange(_val, beKey, exclude, undefined, undefined, customEpics);
  };

  const apiConfig = useMemo(() => (filterMetaData as ApiDropDownData)?.apiConfig, [filterMetaData]) as
    | APIFilterConfigType
    | undefined;

  const loading = useMemo(() => !!apiConfig?.loading, [apiConfig]);

  if (isFilterHidden) return null;

  if (getOptions.length > USE_PAGINATED_FILTERS_THRESHOLD && supportPaginatedSelect) {
    return (
      <APIFilterManyOptions
        data_testId={ITEM_TEST_ID}
        APIFiltersProps={APIFiltersProps()}
        useDefaultOptionKeys
        apiFilterProps={mappedApiFilterProps()}
        help={helpValue}
        switchWithDropdown={apiFilters.switchWithDropdown}
        hideSprintFilters={!!props.handleRemoveFilter}
        createOption={createOption}
        reportType={reportType}
        formLabel={
          <NewCustomFormItemLabel
            label={label}
            required={isRequired}
            withInfo={filterInfoConfig}
            withDelete={apiFilters.withDelete}
            withSwitch={apiFilters.withSwitch}
          />
        }
      />
    );
  }

  return (
    <Form.Item
      data-filterselectornamekey={dataFilterNameKey}
      data-filtervaluesnamekey={dataFilterNameKey}
      className={"custom-form-item custom-filter-padding"}
      help={helpValue}
      label={
        <div style={{ display: "flex", width: "100%" }}>
          <NewCustomFormItemLabel
            label={toTitleCase(label)}
            required={isRequired}
            withInfo={filterInfoConfig}
            {...apiFilters}
          />
        </div>
      }>
      {[JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT, ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(
        report as any
      ) &&
        ((filterProps?.id === "metric" && (
          <p className="widget-velocity-note">{RESOLUTION_TIME_INCOMPATIBLE_FILTERS}</p>
        )) ||
          (filterProps?.id === "sort_xaxis" && (
            <p className="widget-velocity-note">{RESOLUTION_TIME_INCOMPATIBLE_XAXIS}</p>
          )))}

      {[TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT, TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT].includes(
        report as any
      ) &&
        filterProps?.id === "metric" && <p className="widget-velocity-note">{TESTRAIL_INCOMPATIBLE_FILTERS}</p>}
      {Object.keys(partialValue).length > 0 ? (
        <Input
          type="tag"
          data-testid="filter-list-element-input"
          autoFocus={activeFocusKey === beKey}
          onFocus={() => setActiveFocusKey(beKey)}
          onBlur={() => setActiveFocusKey(undefined)}
          placeholder={"Case Sensitive"}
          defaultValue={Object.values(partialValue)[0] as any}
          onChange={e =>
            debouncedOnPartialChange(
              partialKey ?? beKey,
              { [Object.keys(partialValue)[0]]: e.target.value },
              {
                allowExcludeWithPartialMatch: apiFilters?.allowExcludeWithPartialMatch,
                keyName: partialKey ?? beKey
              }
            )
          }
        />
      ) : (
        <Form.Item key={subtitle} label={subtitle}>
          <CustomSelect
            dataFilterNameDropdownKey={dataFilterNameDropdownKey}
            createOption={!!createOption}
            allowClear={clearSupport}
            labelKey={"label"}
            placeholder={placeholder}
            valueKey={"value"}
            labelCase={labelCase ?? "title_case"}
            options={getOptions}
            mode={getSelecteMode}
            showArrow={true}
            value={value}
            loading={loading}
            truncateOptions={true}
            truncateValue={supportPaginatedSelect ? USE_PAGINATED_FILTERS_THRESHOLD : undefined}
            sortOptions={sortOptions}
            disabled={isDisabled || loading}
            dropdownVisible={true}
            onChange={onChange}
          />
        </Form.Item>
      )}
      {[JENKINS_REPORTS.SCM_PRS_REPORT].includes(report as any) && isDisabled && (
        <p className="widget-velocity-note">{SCM_PRS_REPORT_STACK_DISABLED_MSG}</p>
      )}
    </Form.Item>
  );
};

export default UniversalSelectFilterWrapper;
