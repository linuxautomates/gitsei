import { Checkbox, Form, Input, Tooltip } from "antd";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  USE_PAGINATED_FILTERS_THRESHOLD
} from "dashboard/constants/constants";
import { getFilterValue } from "helper/widgetFilter.helper";
import { debounce, get, isArray } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo, useState } from "react";
import { AntText, CustomSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { showInfoProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import APIFilterManyOptions from "../APIFilterManyOptions/APIFilterManyOptions";
import { ITEM_TEST_ID, NOT_ALLOWED_FILTER_LAST_SPRINT } from "../Constants";
import "./UniversalCustomSprintFilter.scss";

interface UniversalCustomSprintFilterWrapperProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string) => void;
  handleRemoveFilter: (key: string) => void;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  handleMetadataChange?: (key: any, value: any) => void;
  activePopKey?: string;
  handleActivePopkey?: (key: string | undefined) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  partialFiltersErrors?: any;
}

const UniversalCustomSprintSelectFilterWrapper: React.FC<UniversalCustomSprintFilterWrapperProps> = props => {
  const {
    filterProps,
    onFilterValueChange,
    activePopKey,
    handleActivePopkey,
    handleMetadataChange,
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
    getMappedValue
  } = filterProps;
  const {
    options,
    selectMode,
    reportType,
    sortOptions,
    createOption,
    clearSupport,
    alwaysExclude,
    mapFilterValueForBE
  } = filterMetaData as DropDownData;

  const [activeFocusKey, setActiveFocusKey] = useState<string | undefined>();
  const debouncedOnPartialChange = debounce(props.handlePartialValueChange, 800);

  const dataFilterNameKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}`;
  const dataFilterNameDropdownKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}_dropdown`;

  const value = getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, beKey, true)?.value;

  const partialValue = useMemo(() => {
    return get(allFilters, [partialFilterKey ?? "partial_match", partialKey ?? beKey], {});
  }, [allFilters]);

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

  const getOptions = useMemo(() => {
    if (isArray(options)) return options;
    if (options instanceof Function) return options({ ...filterProps });
    return [];
  }, [filterProps, options]);

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

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleSwitchValueChange: props.onExcludeFilterChange,
        handlePartialValueChange: props.handlePartialValueChange,
        handleRemoveFilter: props.handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

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
      isCustom: beKey.includes(CUSTOM_FIELD_PREFIX) || beKey.includes(AZURE_CUSTOM_FIELD_PREFIX)
    };
  };

  const onChange = (value: any) => {
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
    onFilterValueChange(_val, beKey, exclude);
  };

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
        createOption={true}
        reportType={reportType}
        formLabel={
          <div className={"w-100 flex direction-column"}>
            <NewCustomFormItemLabel
              label={label}
              required={isRequired}
              withInfo={filterInfoConfig}
              withDelete={apiFilters.withDelete}
              withSwitch={apiFilters.withSwitch}
              lastSprint={allFilters?.["last_sprint"]}
            />
            <div className={"w-100 flex justify-space-between universal-custom-sprint-filter-action"}>
              <div className={"flex"}>
                <div className={"flex mb-5 align-center"}>
                  <Tooltip title={!!allFilters?.["last_sprint"] ? NOT_ALLOWED_FILTER_LAST_SPRINT : ''}>
                    <Checkbox
                      className="mr-5"
                      checked={(allFilters?.["jira_sprint_states"] ?? []).length > 0}
                      onChange={event => {
                        onFilterValueChange(event.target.checked, "jira_sprint_states");
                      }}
                      disabled={!!allFilters?.["last_sprint"]}
                    >
                      <AntText className="action-text-select">Includes active sprints only</AntText>
                    </Checkbox>
                  </Tooltip>
                </div>
                <div className={"flex mb-5 align-center"}>
                  <Checkbox
                    className="mr-5"
                    checked={!!allFilters?.["last_sprint"]}
                    onChange={event => props.handleLastSprintChange?.(event.target.checked, beKey)}>
                    <AntText className="action-text-select">Filter by last sprint</AntText>
                  </Checkbox>
                </div>
              </div>
            </div>
          </div>
        }
      />
    );
  }

  return (
    <Form.Item
      data-filterselectornamekey={dataFilterNameKey}
      data-filtervaluesnamekey={dataFilterNameKey}
      className={"custom-form-item"}
      help={helpValue}
      label={
        <div className={"w-100 flex direction-column"}>
          <NewCustomFormItemLabel
            label={label}
            required={isRequired}
            withInfo={filterInfoConfig}
            {...apiFilters}
            lastSprint={allFilters?.["last_sprint"]}
          />
          <div className={"w-100 flex universal-custom-sprint-filter-action"}>
            <div className={"flex mb-5 align-center"}>
              <Tooltip title={!!allFilters?.["last_sprint"] ? NOT_ALLOWED_FILTER_LAST_SPRINT : ''}>
                <Checkbox
                  className="mr-5"
                  checked={(allFilters?.["jira_sprint_states"] ?? []).length > 0}
                  onChange={event => {
                    onFilterValueChange(event.target.checked, "jira_sprint_states");
                  }}
                  disabled={!!allFilters?.["last_sprint"]}
                >
                  <AntText className="action-text-select">Includes active sprints only</AntText>
                </Checkbox>
              </Tooltip>
            </div>
            <div className={"flex mb-5 align-center"}>
              <Checkbox
                className="mr-5"
                checked={!!allFilters?.["last_sprint"]}
                onChange={event => props.handleLastSprintChange?.(event.target.checked, beKey)}>
                <AntText className="action-text-select">Filter by last sprint</AntText>
              </Checkbox>
            </div>
          </div>
        </div>
      }>

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
            debouncedOnPartialChange(partialKey ?? beKey, { [Object.keys(partialValue)[0]]: e.target.value })
          }
        />
      ) : (
        (!allFilters?.["last_sprint"] &&
          <CustomSelect
            dataFilterNameDropdownKey={dataFilterNameDropdownKey}
            createOption={!!createOption}
            allowClear={clearSupport}
            labelKey={"label"}
            valueKey={"value"}
            labelCase={labelCase ?? "title_case"}
            options={getOptions}
            mode={selectMode}
            showArrow={true}
            value={value}
            truncateOptions={true}
            truncateValue={supportPaginatedSelect ? USE_PAGINATED_FILTERS_THRESHOLD : undefined}
            sortOptions={sortOptions}
            defaultValue={defaultValue}
            disabled={isDisabled}
            dropdownVisible={true}
            onChange={(value: any) => onChange(value)}
          />
        )
      )}
    </Form.Item>
  );
};

export default UniversalCustomSprintSelectFilterWrapper;
