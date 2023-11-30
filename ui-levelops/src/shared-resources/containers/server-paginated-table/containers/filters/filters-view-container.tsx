import React, { ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { Button, DatePicker, Empty, InputNumber, Popover, Radio, Tag } from "antd";
import { AntIcon, AntText, CustomFormItemLabel, InputRangeFilter } from "../../../../components";
import { AntCol, AntInput, AntRow, AntSelect, EditableTag } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { getKeyForFilter } from "../../../../../dashboard/constants/helper";
import { get, isEqual } from "lodash";
import { getStartOfDayFromDateString, getEndOfDayFromDateString, DateFormats } from "../../../../../utils/dateUtils";
import "./filters.view.container.scss";
import Icon from "antd/lib/icon";
import { SearchInput } from "../../../../../dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { usePrevious } from "shared-resources/hooks/usePrevious";

interface FiltersViewContainerProps {
  filtersConfig: any;
  onSearchEvent: (field: any, event: any) => void;
  onOptionSelectEvent: (field: any, value: any, type?: string) => void;
  onInputChange: (field: any, value: string) => void;
  onBinaryChange: (field: any, event: any) => void;
  onTagsChange: (type: string, value: any) => void;
  onMultiOptionsChangeEvent: () => void;
  more_filters: any;
  onExcludeSwitchChange: (field: string, value: any) => void;
  onCheckBoxValueChange: (field: string, key: any, value: any) => void;
  onRemoveFilter: (field: string) => void;
}

const { RangePicker } = DatePicker;
import moment from "moment";

const FiltersViewContainer: React.FC<FiltersViewContainerProps> = props => {
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [searchFilterQuery, setSearchFilterQuery] = useState<any>({});
  const [showFiltersPopOver, setShowFiltersPopOver] = useState<boolean>(false);
  const [addedFilterFields, setAddedFilterFields] = useState<Array<string | ReactNode>>([]);
  const prevFilters = usePrevious(props.more_filters);

  useEffect(() => {
    if (!isEqual(prevFilters, props.more_filters)) {
      setAddedFilterFields(addedFilters => [
        ...(addedFilters ?? []),
        ...props.filtersConfig
          .filter((filter: any) => {
            if (addedFilters.includes(filter.label)) {
              return false;
            }
            if (
              Array.isArray(filter.selected) &&
              filter.selected.length === 0 &&
              filterCheckboxValue(filter.field) === undefined
            ) {
              if (!filterCheckboxValue(filter.field) !== undefined && !filter.excludeSwitchValue) {
                return false;
              }
              return true;
            }
            return filter.selected || filterCheckboxValue(filter.field) !== undefined || filter.excludeSwitchValue;
          })
          .map((field: any) => field.label)
      ]);
    }
  }, [props.more_filters]);

  const handleVisibleChange = useCallback(visible => {
    setSearchQuery("");
    setShowFiltersPopOver(visible);
  }, []);
  const memoizedStyle = useMemo(() => ({ width: "23.5rem" }), []);

  const getSelectedFields = useMemo(() => {
    const _filters: any = [];
    addedFilterFields.forEach((field_label: any) => {
      _filters.push(
        props.filtersConfig.find((filter: any) => {
          if ("label" in filter && !!filter.label) {
            return typeof filter.label === "string" && typeof field_label === "string"
              ? filter.label === field_label
              : filter.label?.props?.title === field_label?.props?.title;
          }
        })
      );
    });
    return _filters;
  }, [props.filtersConfig, addedFilterFields]);

  const dropdownOptions = useMemo(() => {
    const _filters = props.filtersConfig.filter((filter: any) => {
      //Comment 1.1
      // There is two posibilities for label: one Simple string and other React Node
      //We are using this 'typeof' check to properly show the options in dropdown as react node will not work in searching
      const label = typeof filter.label === "string" ? filter.label : filter.label?.props?.title;
      let selected_fields_includes_filter = true;
      getSelectedFields.forEach((selectedField: any) => {
        const selectedLabel =
          typeof selectedField.label === "string" ? selectedField.label : selectedField.label?.props?.title;
        selected_fields_includes_filter =
          selected_fields_includes_filter && !(label.toLowerCase() === selectedLabel.toLowerCase());
      });
      return selected_fields_includes_filter;
    });
    return _filters.filter((filter: any) => {
      //Refer Comment 1.1
      const label = typeof filter.label === "string" ? filter.label : filter.label?.props?.title;
      return label.toLowerCase().includes(searchQuery.toLowerCase());
    });
  }, [props.filtersConfig, searchQuery, getSelectedFields]);

  const Menu = useMemo(() => {
    return (
      <div style={memoizedStyle}>
        <SearchInput value={searchQuery} onChange={(query: string) => setSearchQuery(query)} />
        <div className={"reports-filters-list"}>
          {dropdownOptions.map((filter: any, index: number) => (
            <span className={"reports-filters-list-label"} key={filter.field}>
              <AntText
                className={"reports-filters-list-label-select"}
                onClick={() => {
                  setShowFiltersPopOver(false);
                  setAddedFilterFields((fields: any) => [...fields, filter.label]);
                }}>
                {filter.filterLabel || filter.label}
              </AntText>
            </span>
          ))}
          {(dropdownOptions || []).length === 0 && (
            <div>
              <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          )}
        </div>
      </div>
    );
  }, [props.filtersConfig, searchQuery, getSelectedFields, dropdownOptions]);

  const isFilterPresentInMissingFields = (field: any) => {
    const missingFields = props?.more_filters?.missing_fields || {};
    return Object.keys(missingFields).includes(field);
  };

  const filterCheckboxValue = (field: any) => {
    let filterField = getKeyForFilter(field);
    if (isFilterPresentInMissingFields(filterField)) {
      return get(props, ["more_filters", "missing_fields", filterField]);
    }
    return undefined;
  };

  const onSearchHandler = (field: any, value: string) => {
    props.onSearchEvent(field, value);
  };

  const onInputChange = (field: any, value: any) => {
    props.onInputChange(field, value);
  };

  const onBinaryChange = (field: any, value: any) => {
    props.onBinaryChange(field, value);
  };

  const onOptionSelect = (field: any, value: any) => {
    let val = undefined;
    if (value !== undefined) {
      if (value.key !== undefined) {
        val = value.key;
      } else {
        val = value;
      }
    }
    props.onOptionSelectEvent(field, val);
  };

  const onRestOptionSelect = (field: any) => {
    return (option: any) => {
      const filter = props.filtersConfig.filter((rec: any) => rec.field === field);
      if (filter.length > 0 && filter[0].returnCall) {
        filter[0].returnCall(field, option ? option : undefined);
      }
      props.onOptionSelectEvent(field, option ? option : undefined);
    };
  };

  const onRangeSelect = (field: any, dataType: any) => {
    return (dates: any, dateStrings: any) => {
      if (dates.length > 1) {
        // converting the date in UTC
        let data: { [key: string]: any } = {
          $gt: getStartOfDayFromDateString(dateStrings[0], DateFormats.DAY),
          $lt: getEndOfDayFromDateString(dateStrings[1], DateFormats.DAY)
        };
        if (dataType === "string") {
          data = {
            $gt: data["$gt"].toString(),
            $lt: data["$lt"].toString()
          };
        }
        return props.onOptionSelectEvent(field, data, "date");
      } else {
        return props.onOptionSelectEvent(field, undefined, "date");
      }
    };
  };

  const handleFilterDelete = (label: any) => {
    setAddedFilterFields((addedFilterFields: Array<string | ReactNode>) =>
      addedFilterFields.filter((field_label: any) => {
        const label1 = typeof field_label === "string" ? field_label : field_label?.props?.title;
        const label2 = typeof label === "string" ? label : label?.props?.title;
        return label1 !== label2;
      })
    );
  };

  const renderLabel = (label: string) => (
    <div className="filter-label">
      <span>{label}</span>
    </div>
  );
  return (
    <div className={"report-filters-container"}>
      <span className={"report-filters-container-dropdown"}>
        <Popover
          className={"search-popover"}
          placement={"bottomLeft"}
          content={Menu}
          trigger="click"
          visible={showFiltersPopOver}
          onVisibleChange={handleVisibleChange}>
          <Button onClick={() => setShowFiltersPopOver(!showFiltersPopOver)}>
            Add Filter <Icon type="down" />
          </Button>
        </Popover>
      </span>
      <AntRow gutter={[32, 32]}>
        {getSelectedFields.length === 0 && (
          <div>
            <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
          </div>
        )}
        {getSelectedFields.map((filter: any, index: number) => {
          if (filter.type === "search") {
            return (
              <AntCol key={index} className="gutter-row" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.label)}
                    <AntInput
                      id={`search-${filter.id}`}
                      placeholder={filter.label}
                      onChange={(e: any) => onSearchHandler(filter.field, e.target.value)}
                      value={filter.selected || ""}
                      name={filter.id}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }
          if (filter.type === "binary") {
            return (
              <AntCol key={index} className="gutter-row flex align-center" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.label)}
                    <Radio.Group
                      id={`binary-${filter.id}`}
                      style={{ marginLeft: "1rem" }}
                      onChange={(e: any) => onBinaryChange(filter.field, e.target.value)}
                      value={filter.selected === undefined ? "all" : filter.selected}>
                      <Radio value={true}>Yes</Radio>
                      <Radio value={false}>No</Radio>
                      <Radio value={"all"}>All</Radio>
                    </Radio.Group>
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (filter.type === "input") {
            return (
              <AntCol key={index} className="gutter-row" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.label)}
                    <InputNumber
                      style={{ width: "100%" }}
                      id={`input-${filter.id}`}
                      placeholder={filter.label}
                      onChange={(value: any) => onInputChange(filter.field, value)}
                      value={filter.selected || ""}
                      name={filter.id}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (filter.type === "tags") {
            return (
              <AntCol key={index} className="gutter-row mb-20" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.label)}
                    <EditableTag
                      tagLabel={"Add Epic"}
                      style={{ width: "100%" }}
                      tags={filter.selected || []}
                      onTagsChange={(value: any) => props.onTagsChange("epics", value)}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (["select", "multiSelect"].includes(filter.type)) {
            const optionLength = (filter.options || []).length;
            let filteredOptions = filter.options ?? [];
            const maxLength = filter.unlimitedLength ? optionLength : Math.min(20, optionLength);
            if (optionLength > 20 && !filter.unlimitedLength) {
              filteredOptions = (filter.options || [])
                .filter((opt: any) => {
                  if (typeof opt === "string") {
                    return (opt || "").toLowerCase().includes((searchFilterQuery.search_value || "").toLowerCase());
                  }
                  return (
                    (opt.value || "").includes(searchFilterQuery.search_value) ||
                    (opt.label || "").toLowerCase().includes((searchFilterQuery.search_value || "").toLowerCase())
                  );
                })
                .slice(0, maxLength);
            }
            //done because we have case where we have more than 20 options and the selected option has index > 20
            const filterKeys = filteredOptions.map((option: any) => option.value);
            if (filter.type === "multiSelect") {
              (filter.selected || []).forEach((item: any) => {
                const option = filter.options.find((option: any) => option.value === item);
                if (option && !filterKeys.includes(item)) {
                  filteredOptions.push(option);
                }
              });
            }
            if (filter.type === "select") {
              const option = filter.options.find((option: any) => option.value === filter.selected);
              if (option && !filterKeys.includes(filter.selected)) {
                filteredOptions.push(option);
              }
            }
            return (
              <AntCol key={index} className="gutter-row" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    <CustomFormItemLabel
                      label={filter.label}
                      withSwitch={{
                        showSwitch: filter.showExcludeSwitch,
                        showSwitchText: filter.showExcludeSwitch,
                        switchValue: filter.excludeSwitchValue,
                        disabled: filterCheckboxValue(filter.field) !== undefined,
                        onSwitchValueChange: value => props?.onExcludeSwitchChange?.(filter.field, value)
                      }}
                      withCheckBoxes={{
                        showCheckboxes: filter.showCheckboxes,
                        checkboxes: [
                          { label: "PR", key: "pr", value: filterCheckboxValue(filter.field) === false },
                          { label: "AB", key: "ab", value: filterCheckboxValue(filter.field) === true }
                        ],
                        onCheckBoxChange: (key, value) => props?.onCheckBoxValueChange?.(filter.field, key, value)
                      }}
                    />
                    <AntSelect
                      style={{ width: "100%" }}
                      id={`select-${filter.id}`}
                      placeholder={filter.label}
                      onSearch={(value: any) => {
                        setSearchFilterQuery({ search_value: value });
                      }}
                      options={[...filteredOptions]}
                      filterOption={optionLength < 20}
                      defaultValue={filter.selected}
                      onBlur={(e: any) => setSearchFilterQuery({ search_value: "" })}
                      value={filter.selected}
                      onChange={(value: any) => onOptionSelect(filter.field, value)}
                      allowClear={true}
                      disabled={filterCheckboxValue(filter.field) !== undefined}
                      mode={filter.type === "multiSelect" ? "multiple" : "default"}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (filter.type === "dateRange") {
            // considering the timestamp is in UTC, showing in local timezone
            const selected = filter.selected
              ? Object.keys(filter.selected).map(key =>
                  moment.unix(filter.selected[key]).add(moment().utcOffset() * -1, "m")
                )
              : [];
            return (
              <AntCol key={index} className={"gutter-row"} span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.filterLabel || filter.label)}
                    <RangePicker
                      style={{ width: "100%" }}
                      onChange={onRangeSelect(filter.field, filter.rangeDataType)}
                      allowClear={true}
                      // @ts-ignore
                      value={selected}
                      format={DateFormats.DAY}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (filter.type === "timeRange") {
            return (
              <AntCol key={index} className={"gutter-row"} span={12}>
                <AntRow>
                  <AntCol span={20}>
                    <InputRangeFilter
                      formClass="w-100"
                      label={filter.label}
                      onChange={(value: any) => onInputChange(filter.field, value)}
                      value={filter.selected || {}}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }

          if (["apiSelect", "apiMultiSelect"].includes(filter.type)) {
            return (
              <AntCol key={index} className="gutter-row" span={12}>
                <AntRow>
                  <AntCol span={20}>
                    {renderLabel(filter.label === "" ? filter.uri : filter.label)}
                    <SelectRestapi
                      style={{ width: "100%" }}
                      id={`apiselect-${filter.id}`}
                      placeholder={filter.label}
                      //rest_api={this.props.rest_api}
                      moreFilters={filter.moreFilters}
                      transformOptions={filter.transformOptions}
                      transformPayload={filter.transformPayload}
                      uri={filter.uri}
                      morePayload={filter.morePayload}
                      //fetchData={filter.apiCall}
                      searchField={filter.searchField}
                      defaultValue={filter.selected || null}
                      additionalOptions={filter.options || []}
                      value={filter.selected || ""}
                      onChange={onRestOptionSelect(filter.field)}
                      createOption={false}
                      mode={filter.type === "apiMultiSelect" ? "multiple" : "default"}
                      specialKey={filter.specialKey}
                    />
                  </AntCol>
                  <AntCol span={4}>
                    <AntIcon
                      style={{ fontSize: "16px", marginLeft: "20px" }}
                      type={"delete"}
                      onClick={e => {
                        e.stopPropagation();
                        props.onRemoveFilter(filter.field);
                        handleFilterDelete(filter.label);
                      }}
                    />
                  </AntCol>
                </AntRow>
              </AntCol>
            );
          }
        })}
      </AntRow>
    </div>
  );
};

export default FiltersViewContainer;
