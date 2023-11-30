import classNames from "classnames";
import { useDebounce } from "custom-hooks/useDebounce";
import { SELECT_NAME_TYPE_ITERATION } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import PopupPaginatedTable, {
  PopupPaginatedTableProps
} from "dashboard/graph-filters/components/popup-paginated-table/PopupPaginatedTable";
import { capitalize } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  AntBadge,
  AntButton,
  AntFormItem,
  AntIcon,
  AntInput,
  AntPopover,
  AntTag,
  AntTooltip,
  BigTag,
  FilterList,
  SwitchWithSelect
} from "shared-resources/components";
import { convertArrayToTree, convertTreeToArray } from "shared-resources/components/custom-tree-select/helper";
import PopupTree from "shared-resources/components/popup-tree/PopupTree";
import { toTitleCase, valuetoLabel, isValidRegEx, sanitizeRegexString } from "utils/stringUtils";
import { getTagSelectHeightClassName } from "./getTagSelectHeightClassName";
import "./TagPaginatedSelect.scss";

export const STARTS_WITH = "$begins";
export const CONTAINS = "$contains";
export const REGEX = "$regex";

interface TagSelectProps extends Omit<PopupPaginatedTableProps, "onSelectionChange" | "selectedRowsKeys"> {
  isVisible: boolean;
  isCustom: boolean;
  onVisibleChange: (dataKey?: string) => any;
  dataKey: string;
  onChange: (value: any) => any;
  switchWithDropdown: any;
  selectMode: "full" | "partial";
  onCancel: (...args: any) => any;
  selectedValues: any[];
  partialValue: any;
  switchValue: any;
  onFilterValueChange: (...args: any) => any;
  onPartialValueChange: (...args: any) => any;
  createOption?: boolean | undefined;
  // Limits user to one selection
  singleSelect?: boolean;
  reportType?: string;
  useDefaultOptionKeys?: boolean;
  partialKey?: string;
  customClassNames?: string;
}

const LONG_TAG_THRESHOLD = 20;
const DISPLAY_NAMES = {
  [STARTS_WITH]: "starts with",
  [CONTAINS]: "contains",
  [REGEX]: "Regex"
};

const TagSelect: React.FC<TagSelectProps> = props => {
  const {
    isVisible,
    tableHeader,
    dataSource,
    filterValueLoading,
    columns,
    valueKey,
    selectedValues,
    dataKey,
    onChange,
    switchWithDropdown,
    selectMode,
    switchValue,
    reportType,
    useDefaultOptionKeys,
    partialKey
  } = props;

  const { selectProps } = switchWithDropdown || {};

  const onCancel = useCallback(() => {
    props.onCancel();
  }, []);

  const partialValueText: string = Object.values((props.partialValue as Object) || {})[0] || "";
  const partialFilterType: "$begins" | "$contains" | "$regex" = selectProps?.value || STARTS_WITH;

  const [localSelectedValues, setLocalSelectedValues] = useState<any[]>(selectedValues || []);
  const [localSelectMode, setLocalSelectMode] = useState<"full" | "partial">(selectMode || "full");
  const [localPartialValue, setLocalPartialValue] = useState<any>(partialValueText);
  const [localPartialFilterType, setLocalPartialFilterType] = useState<string>(partialFilterType);
  const [showLocalPartialFilteredData, setShowLocalPartialFilteredData] = useState<boolean>(false);
  const [localExcludeSwitchValue, setLocalExcludeSwitchValue] = useState<any>(switchValue);
  const debouncedLocalPartialValue = useDebounce(localPartialValue, 500);
  const localSelectedValuesCount = (localSelectedValues || []).filter((item: any) => {
    //we are getting (_UNNASSIGNED_) in the localselectedvalues and the value in datasource is in titleCase so for comparison need to convert in the title case
    return !!dataSource.find((val: any) =>
      toTitleCase(val[props.valueKey || props.labelKey || "key"])?.includes(toTitleCase(item))
    );
  }).length;
  const optionsLabelMap = useMemo(() => {
    let result: any = {};
    if (props.isCustom || props.labelKey === "value" || useDefaultOptionKeys) {
      (props.dataSource || []).forEach(option => {
        result[option[props.valueKey]] = option[props.labelKey || props.valueKey];
      });
    }

    return result;
  }, [props.isCustom, props.dataSource, props.valueKey, props.labelKey]);

  const updatedDataSource = useMemo(() => {
    if (tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION) {
      const treeData = convertArrayToTree(dataSource || []);
      return convertTreeToArray(treeData);
    }
    return props.dataSource;
  }, [props.dataSource, tableHeader]);

  useEffect(() => {
    setLocalSelectedValues(props.selectedValues || []);
  }, [props.selectedValues]);

  useEffect(() => {
    setLocalSelectMode(props.selectMode || "full");
  }, [props.selectMode]);

  useEffect(() => {
    setLocalPartialValue(partialValueText);
  }, [partialValueText]);

  useEffect(() => {
    setLocalExcludeSwitchValue(props.switchValue);
  }, [props.switchValue]);

  useEffect(() => {
    setLocalPartialFilterType(partialFilterType);
  }, [partialFilterType]);

  const resetValues = () => {
    setLocalSelectedValues(props.selectedValues || []);
    setLocalSelectMode(props.selectMode || "full");
    setLocalPartialValue(partialValueText);
    setLocalExcludeSwitchValue(props.switchValue);
    setLocalPartialFilterType(partialFilterType);
  };

  const handleVisibleChange = (visible: boolean) => {
    props.onVisibleChange(visible ? dataKey : undefined);

    // If we are opening the popover.
    // if (!isVisible && visible) {
    //   resetValues();
    // }
  };

  const removeTag = (tag: any) => {
    let newTags = [];
    if (localSelectMode === "full" && tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION) {
      newTags = [
        ...selectedValues.filter(value => {
          if (typeof value === "string") {
            return value !== tag;
          }
          return `${value.parent}\\${value.child}` !== tag;
        })
      ];
    } else if (localSelectMode === "full" && tableHeader?.toLowerCase() === "azure areas") {
      newTags = [
        ...selectedValues.filter(value => {
          if (typeof value === "string") {
            return value !== tag;
          }
          return `${value.child}` !== tag;
        })
      ];
    } else {
      newTags = selectedValues.filter(val => val !== tag);
    }
    onChange(newTags);
  };

  const removeAllTags = () => {
    onChange([]);
  };

  const fieldLabel = typeof tableHeader === "string" ? valuetoLabel(tableHeader) : tableHeader;

  const onSave = () => {
    if (localSelectMode === "partial") {
      props.onFilterValueChange([], dataKey, switchValue);
      props.onPartialValueChange(partialKey ?? dataKey, { [localPartialFilterType]: localPartialValue });
    } else {
      props.onPartialValueChange(partialKey ?? dataKey, undefined);
      props.onFilterValueChange(localSelectedValues, dataKey, switchValue);
    }

    onCancel();
  };

  const addbuttonHandler = (value: string) => {
    props.onPartialValueChange(partialKey ?? dataKey, undefined);
    props.onFilterValueChange([...localSelectedValues, value], dataKey, switchValue);
    onCancel();
  };

  const shouldShowPartialTag = props.selectMode === "partial" && !!partialValueText.length;

  const shouldShowSelectedValues = Array.isArray(selectedValues) && !!selectedValues.length;

  const shouldHaveMarginBottom = shouldShowPartialTag || shouldShowSelectedValues;

  let formattedFilterText = typeof partialValueText === "string" ? partialValueText : "";

  formattedFilterText = sanitizeRegexString(formattedFilterText);
  const beginsRegex = new RegExp(`^${formattedFilterText}`, "g");

  let localFormattedFilterText = typeof debouncedLocalPartialValue === "string" ? debouncedLocalPartialValue : "";

  // sanitizing input string for regex error
  localFormattedFilterText = sanitizeRegexString(localFormattedFilterText);
  const localBeginsRegex = new RegExp(`^${localFormattedFilterText}`, "g");

  const partialFilteredData = shouldShowPartialTag
    ? updatedDataSource.filter((item: any) => {
        if (item?.children && tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION) {
          return false;
        }
        let keep = false;
        const formattedItemValue =
          tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION
            ? item.key
            : item?.[props.labelKey || valueKey] || "";
        if (partialFilterType === "$contains") {
          keep = !!formattedItemValue.includes(formattedFilterText);
        } else if (localPartialFilterType === "$regex") {
          keep = !!formattedItemValue.match(formattedFilterText);
        } else {
          // filterStyle === "$begins"
          keep = !!formattedItemValue.match(beginsRegex);
        }

        return keep;
      })
    : [];

  const localPartialFilteredData =
    (showLocalPartialFilteredData || localSelectMode === "partial") && !!debouncedLocalPartialValue
      ? updatedDataSource.filter((item: any) => {
          if (item?.children && tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION) {
            return false;
          }
          let keep = false;
          const formattedItemValue =
            tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION
              ? item.key
              : item?.[props.labelKey || valueKey] || "";
          if (localPartialFilterType === "$contains") {
            keep = !!formattedItemValue.includes(localFormattedFilterText);
          } else if (localPartialFilterType === "$regex") {
            if (isValidRegEx(localFormattedFilterText)) {
              var reg = new RegExp(localFormattedFilterText);
              keep = reg.test(formattedItemValue);
            }
          } else {
            // filterStyle === "$begins"
            keep = !!formattedItemValue.match(localBeginsRegex);
          }

          return keep;
        })
      : [];

  const getDataKeyForLocalFilteredList = useMemo(() => {
    if (useDefaultOptionKeys) return "value";
    if (
      [
        "github_commits_report",
        "github_prs_report",
        "github_prs_single_stat",
        "github_prs_report_trends",
        "github_prs_merge_trends",
        "github_prs_first_review_trends",
        "github_prs_first_review_to_merge_trends",
        "github_prs_merge_single_stat",
        "github_prs_first_review_single_stat",
        "github_prs_first_review_to_merge_single_stat",
        "github_prs_response_time_report",
        "github_prs_response_time_single_stat",
        "review_collaboration_report"
      ].includes(reportType || "") &&
      ["author", "committer", "assignee", "creator", "reviewer", "target_branches", "source_branches"].includes(dataKey)
    ) {
      return "value";
    }
    return "key";
  }, [dataKey, reportType, useDefaultOptionKeys]);

  const partialMatchesCount = partialFilteredData.length;
  const partialValueWithCount = `${partialValueText} (${partialMatchesCount})`;
  const antbadgeCount = (dataSource || []).length;
  const localPartialMatchesCount = localPartialFilteredData.length;
  const localPartialValueWithCount = `${localPartialValue} (${localPartialMatchesCount})`;

  const tagSelectHeightClassName = useMemo(() => {
    return getTagSelectHeightClassName(showLocalPartialFilteredData, localSelectMode);
  }, [showLocalPartialFilteredData, localSelectMode]);

  // const tagSelectContentClassName = classnames("tag-paginated-select__popover-content")
  const tagSelectContentClassName = useMemo(() => {
    return classNames("tag-paginated-select__popover-content", tagSelectHeightClassName);
  }, [tagSelectHeightClassName]);

  const updatedSelectedValue = useMemo(() => {
    if (localSelectMode === "full" && tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION) {
      return selectedValues.map(value => {
        if (typeof value === "string") {
          return value;
        }
        return `${value.parent}\\${value.child}`;
      });
    }
    if (localSelectMode === "full" && tableHeader?.toLowerCase() === "azure areas") {
      return selectedValues.map(value => {
        if (typeof value === "string") {
          return value;
        }
        return `${value.child}`;
      });
    }
    return selectedValues;
  }, [selectedValues]);

  return (
    <div className="tag-paginated-select">
      <div style={{ overflow: "auto", maxHeight: "100px", marginBottom: shouldHaveMarginBottom ? "10px" : "0px" }}>
        {shouldShowSelectedValues &&
          updatedSelectedValue.map((tagInternalValue: string, index: number) => {
            let tagDisplayValue = optionsLabelMap[tagInternalValue] || tagInternalValue;
            const isLongTag = tagDisplayValue.length > LONG_TAG_THRESHOLD;
            const tagElem = (
              <AntTag
                key={tagInternalValue}
                className="tag-paginated-select__tag"
                closable
                onClose={() => removeTag(tagInternalValue)}>
                {isLongTag ? `${tagDisplayValue.slice(0, LONG_TAG_THRESHOLD)}...` : tagDisplayValue}
              </AntTag>
            );

            return isLongTag ? (
              <AntTooltip title={tagDisplayValue} key={tagInternalValue}>
                {tagElem}
              </AntTooltip>
            ) : (
              tagElem
            );
          })}
        {shouldShowPartialTag && (
          <BigTag
            label={partialFilterType === STARTS_WITH ? DISPLAY_NAMES[STARTS_WITH] : DISPLAY_NAMES[CONTAINS]}
            value={partialValueWithCount}
            onValueClick={() => {
              // Open popover.
              if (isVisible && showLocalPartialFilteredData) {
                handleVisibleChange(false);
              } else {
                handleVisibleChange(true);
              }
              setShowLocalPartialFilteredData(true);
            }}
            onCloseClick={() => {
              props.onPartialValueChange(partialKey ?? dataKey, { [partialFilterType]: "" });
            }}
          />
        )}
      </div>
      <div className="flex align-center tag-paginated-select__popover-row">
        <div style={{ flex: 1 }}>
          <AntPopover
            trigger={[]}
            overlayClassName={classNames("tag-paginated-select__popover", props.customClassNames)}
            placement="right"
            title={
              <div style={{ position: "relative", fontSize: "21px", padding: "38px 30px 0 35px" }}>
                {capitalize(`Select ${fieldLabel}`)}
                <div className="close-icon-container">
                  <AntIcon type={"close"} onClick={onCancel} className="close-icon" />
                </div>
              </div>
            }
            content={
              <div className={tagSelectContentClassName}>
                {showLocalPartialFilteredData ? (
                  <FilterList
                    header={
                      <div style={{ position: "relative", paddingRight: "30px" }}>
                        <span>Showing results for: </span>
                        <span>
                          <strong>{localPartialValueWithCount}</strong>
                        </span>
                        <div style={{ position: "absolute", right: "0px", top: "0px" }}>
                          <AntIcon
                            type={"close"}
                            onClick={() => {
                              setShowLocalPartialFilteredData(false);
                            }}
                            style={{ fontSize: "16px" }}
                          />
                        </div>
                      </div>
                    }
                    dataSource={localPartialFilteredData}
                    dataKey={getDataKeyForLocalFilteredList}
                    labelKey={props.labelKey}
                    tableHeader={tableHeader || ""}
                  />
                ) : (
                  <React.Fragment>
                    <div style={{ flex: 1 }}>
                      {switchWithDropdown && switchWithDropdown.showSwitchWithDropdown && (
                        <SwitchWithSelect
                          selectType="radio"
                          data_testId="tag-select-switch-with-select"
                          checkboxProps={{
                            ...switchWithDropdown.checkboxProps,
                            value: localSelectMode === "partial",
                            onCheckboxChange: (checked: boolean) => {
                              setLocalSelectMode(checked ? "partial" : "full");
                            }
                          }}
                          selectProps={{
                            ...switchWithDropdown.selectProps,
                            value: localPartialFilterType || STARTS_WITH,
                            disabled: localSelectMode !== "partial",
                            onSelectChange: (key: any) => {
                              setLocalPartialFilterType(key);
                            }
                          }}
                        />
                      )}
                      {localSelectMode === "partial" && (
                        <React.Fragment>
                          <AntFormItem help={"Case sensitive. Limited to one value."}>
                            <AntInput
                              value={localPartialValue}
                              onChange={(event: any) => {
                                setLocalPartialValue(event.target.value);
                              }}
                            />
                          </AntFormItem>
                          <AntButton
                            type="link"
                            style={{
                              paddingLeft: "0px",
                              fontSize: "12px"
                            }}
                            onClick={() => {
                              setShowLocalPartialFilteredData(true);
                            }}>
                            Show results ({localPartialMatchesCount})
                          </AntButton>
                        </React.Fragment>
                      )}
                      {localSelectMode === "full" &&
                        (tableHeader &&
                        [SELECT_NAME_TYPE_ITERATION, "azure areas"].includes(tableHeader.toLowerCase()) ? (
                          <PopupTree
                            dataSource={dataSource}
                            selectedRowsKeys={localSelectedValues}
                            onSelectionChange={(newSelectedRows: any[]) => {
                              setLocalSelectedValues(newSelectedRows);
                            }}
                            addbuttonHandler={addbuttonHandler}
                            label={tableHeader}
                          />
                        ) : (
                          <PopupPaginatedTable
                            columns={columns}
                            valueKey={valueKey}
                            labelKey={props.labelKey}
                            filterValueLoading={filterValueLoading}
                            dataSource={dataSource}
                            singleSelect={props.singleSelect}
                            selectedRowsKeys={localSelectedValues}
                            //@ts-ignore
                            tableHeader={
                              <div style={{ display: "flex", flexDirection: "row" }}>
                                <AntTag
                                  color={
                                    localSelectedValuesCount === 0 ? "" : "blue"
                                  }>{`${localSelectedValuesCount} Selected`}</AntTag>
                                <AntBadge
                                  overflowCount={antbadgeCount}
                                  count={antbadgeCount}
                                  className={classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}
                                  style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }}
                                />
                              </div>
                            }
                            onSelectionChange={(newSelectedRows: any[]) => {
                              setLocalSelectedValues(newSelectedRows);
                            }}
                            createOption={props.createOption}
                            isCustom={props.isCustom}
                            addbuttonHandler={addbuttonHandler}
                          />
                        ))}
                    </div>
                    <div className="flex justify-space-between p-10">
                      {localSelectMode === "full" && !!localSelectedValues.length ? (
                        <AntButton
                          type="link"
                          style={{
                            paddingLeft: "0px",
                            fontSize: "12px"
                          }}
                          onClick={() => {
                            removeAllTags();
                          }}>
                          Clear
                        </AntButton>
                      ) : (
                        <div />
                      )}
                      <div className="flex">
                        <AntButton className="mr-10" onClick={onCancel}>
                          Cancel
                        </AntButton>
                        <AntButton type="primary" onClick={onSave}>
                          Save
                        </AntButton>
                      </div>
                    </div>
                  </React.Fragment>
                )}
              </div>
            }
            visible={isVisible}
            onVisibleChange={handleVisibleChange}>
            <hr />
            <AntButton
              onClick={() => {
                setShowLocalPartialFilteredData(false);
                if (isVisible && !showLocalPartialFilteredData) {
                  handleVisibleChange(false);
                } else {
                  handleVisibleChange(true);
                }
              }}>
              Select
            </AntButton>
          </AntPopover>
        </div>
      </div>
    </div>
  );
};

export default TagSelect;
