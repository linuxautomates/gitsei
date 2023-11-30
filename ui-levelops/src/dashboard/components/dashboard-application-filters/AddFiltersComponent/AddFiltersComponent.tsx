import React, { useCallback, useMemo, useState, useEffect, memo } from "react";
import { AntText, AntIcon, IntegrationIcon } from "shared-resources/components";
import "./AddFiltersComponent.scss";
import { useSupportedFilters } from "../../../../custom-hooks";
import { Button, Empty, Popover, Spin } from "antd";
import Icon from "antd/lib/icon";
import { SearchInput } from "../../../pages/dashboard-drill-down-preview/components/SearchInput.component";
import { get, uniqBy } from "lodash";
import { ADDITIONAL_KEY_FILTERS, valuesToFilters } from "../../../constants/constants";
import widgetConstants from "../../../constants/widgetConstants";
import { APIFilter } from "../../../graph-filters/components";
import { getInitialOptions } from "./helpers";
import { uiFiltersMapping, supportedFiltersLabelMapping, filesFilters, GlobalFilesFilters } from "./filterConstants";
import cx from "classnames";
import { uiFilterKeys, UiFilters } from "./UiFilters";
import { CoverityValuesReports, PagerDutyReports } from "./Constants";
import { useFieldList } from "../../../../custom-hooks/useFieldList";
import { timeBasedFields } from "../../../graph-filters/components/helper";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../../../graph-filters/components/time-range-abs-rel-wrapper.component";

interface AddFiltersComponentProps {
  report: any;
  onDeleteReportFilters?: (report: any) => void;
  integrationIds: Array<any>;
  filters: any;
  hideHeader?: boolean;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterRemoved: (key: string) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string, isCustom?: boolean) => void;
  dropdownButtonClass?: string;
  orderedFilters: string[];
  setOrderedFilters: any;
  additionalId?: string;
  partialFiltersErrors: any;
  ignoreFilterKeys?: string[];
}

const AddFiltersComponent: React.FC<AddFiltersComponentProps> = ({
  report,
  onDeleteReportFilters,
  integrationIds,
  filters,
  hideHeader,
  onFilterValueChange,
  onExcludeFilterChange,
  handlePartialValueChange,
  onFilterRemoved,
  handleTimeRangeTypeChange,
  handleTimeRangeFilterValueChange,
  dropdownButtonClass,
  orderedFilters,
  setOrderedFilters,
  additionalId,
  partialFiltersErrors,
  ignoreFilterKeys
}) => {
  const [showFiltersPopOver, setShowFiltersPopOver] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [dropDownOptions, setDropDownOptions] = useState<any[]>([]);
  const iconStyle = useMemo(() => ({ marginRight: "5px" }), []);

  const handleVisibleChange = useCallback(visible => {
    setShowFiltersPopOver(visible);
    setSearchQuery("");
  }, []);

  const [activePopKey, setActivePopKey] = useState<string | undefined>();

  const getReportFilter = useMemo(() => {
    let _filter = report.filter;
    if (["praetorian_issues_values", "ncc_group_issues_values"].includes(report.filter.uri)) {
      const values =
        report.filter.uri === "ncc_group_issues_values" ? ["component", "risk", "category"] : ["priority", "category"];
      _filter = {
        uri: report.filter.uri,
        values: values
      };
      return _filter;
    } else {
      return _filter;
    }
  }, [report.filter]);

  const supportedFilterId = useMemo(() => (additionalId ? additionalId : report.uri), [additionalId]);

  const { loading: fieldsLoading, list: fieldList } = useFieldList(report.application, integrationIds);

  const { loading: apiLoading, apiData } = useSupportedFilters(
    getReportFilter,
    integrationIds,
    report.application,
    [report.uri],
    false,
    supportedFilterId
  );

  const metaData = get(filters, ["metadata"], {});

  const supportExcludeFilters = useMemo(
    () => (!report.report ? true : get(widgetConstants, [report.report, "supportExcludeFilters"], false)),
    [report.report]
  );

  const supportPartialStringFilters = useMemo(
    () => (!report.report ? true : get(widgetConstants, [report.report, "supportPartialStringFilters"], false)),
    [report.report]
  );

  useEffect(() => {
    if (!apiLoading && apiData.length > 0) {
      const filteredField = apiData.find(filter => Object.keys(filter)[0] === "custom_fields");
      let list: any = [];
      if (Array.isArray(getReportFilter)) {
        getReportFilter.forEach(data => {
          if (report.uri === "scm_files_filter_values-jira_filter_values") {
            list = [
              ...list,
              ...getInitialOptions(
                data.values,
                filteredField ? filteredField["custom_fields"] : [],
                uiFiltersMapping[report.uri],
                filters,
                report.uri
              )
            ];
          } else {
            list = [
              ...list,
              ...getInitialOptions(
                data.values,
                filteredField ? filteredField["custom_fields"] : [],
                uiFiltersMapping[report.uri],
                filters,
                report.uri
              )
            ];
          }
        });
      } else {
        list = getInitialOptions(
          getReportFilter.values,
          filteredField ? filteredField["custom_fields"] : [],
          uiFiltersMapping[report.uri],
          filters,
          report.uri
        );
      }
      list = uniqBy(list, "key");
      if (ignoreFilterKeys) {
        list = (list || []).filter((filter: any) => !ignoreFilterKeys.includes(filter?.key));
      }
      if (filesFilters.includes(report.uri)) {
        const filterKey = report.uri === GlobalFilesFilters.SCM_FILES_FILTERS ? "module" : "scm_module";
        list = list.filter((filter: any) => filter.key !== filterKey);
      }
      setDropDownOptions(list);
    }
  }, [apiData, apiLoading]);

  const memoizedStyle = useMemo(() => ({ width: "23.5rem" }), []);

  const getCustomFieldData = useMemo(() => {
    const fields = apiData.find((item: any) => Object.keys(item)[0] === "custom_fields");
    if (fields && fields.hasOwnProperty("custom_fields")) {
      return fields.custom_fields.map((field: any) => {
        const valuesRecord = apiData.find(item => Object.keys(item)[0] === field.key);
        if (valuesRecord) {
          return {
            name: field.name,
            key: field.key,
            values: valuesRecord[Object.keys(valuesRecord)[0]]
          };
        }
        return undefined;
      });
    } else return [];
  }, [apiLoading, apiData]);

  const getMappedSupportedFilters = useCallback(
    key => {
      let _apiData = apiData.map(item => {
        let key = Object.keys(item)[0];
        let _item = item[key];
        if (Array.isArray(_item)) {
          _item = _item.map(options => {
            if (options.hasOwnProperty("cicd_job_id") && key !== "job_normalized_full_name") {
              return {
                key: options["cicd_job_id"],
                value: options["key"]
              };
            } else if (options.hasOwnProperty("additional_key") && ADDITIONAL_KEY_FILTERS.includes(key)) {
              return {
                key: options["key"],
                value: options["additional_key"]
              };
            }
            if (get(supportedFiltersLabelMapping, [report.uri, options.key, "key"])) {
              return {
                ...options,
                key: get(supportedFiltersLabelMapping, [report.uri, options.key, "key"])
              };
            }
            if (PagerDutyReports.includes(report.uri)) {
              if (["user_id", "pd_service"].includes(key)) {
                return {
                  key: options["id"],
                  value: options["name"]
                };
              }
              if (["incident_priority", "incident_urgency", "alert_severity"].includes(key)) {
                return {
                  key: options,
                  value: options
                };
              }
            }
            if (
              CoverityValuesReports.includes(report.uri) &&
              ["last_detected_stream", "first_detected_stream"].includes(key)
            ) {
              return {
                key: options["additional_key"],
                value: options["additional_key"]
              };
            }
            return options;
          });
        }

        const mappedKey = get(supportedFiltersLabelMapping, [report.uri, key, "key"], key);
        return { [mappedKey]: _item };
      });

      const filterData = _apiData.find(item => {
        const itemKey = Object.keys(item)[0];

        return ((valuesToFilters as any)[itemKey] || itemKey) === key;
      });

      if (filterData) {
        return filterData;
      }

      return {};
    },
    [filters, apiData]
  );

  const transformCustomData = useCallback(
    (filterKey: string): { [key: string]: any } => {
      const filterData = getCustomFieldData.find(
        (item: { name: string; key: string; values: any[] }) => item.key === filterKey
      );
      if (filterData) {
        const dataKey = `${filterData.key}@${filterData.name}`;
        return {
          [dataKey]: filterData.values
        };
      }

      return [];
    },
    [getCustomFieldData, filters]
  );

  const filterOptionSelected = (key: string) => {
    const index = dropDownOptions.findIndex(filter => filter.key === key);
    if (index !== -1) {
      let updatedOptions = [...dropDownOptions];
      updatedOptions[index].selected = true;
      setDropDownOptions(updatedOptions);
      onFilterValueChange(updatedOptions[index].defaultValue, updatedOptions[index].key);
      setShowFiltersPopOver(false);
      setOrderedFilters([...orderedFilters, (valuesToFilters as any)[key] || key]);
    }
  };

  const removeFilterOptionSelected = (key: any) => {
    let _key = key;
    if (typeof key === "object") {
      _key = Object.keys(key)[0];
    }

    _key = (valuesToFilters as any)[_key] || _key;

    if (_key.includes("customfield")) {
      _key = _key.split("@")[0];
    }
    const index = dropDownOptions.findIndex(filter => filter.key === _key);
    if (index !== -1) {
      let updatedOptions = [...dropDownOptions];
      updatedOptions[index].selected = false;
      setDropDownOptions(updatedOptions);
      onFilterRemoved && onFilterRemoved(_key);
      setOrderedFilters(orderedFilters.filter(orderedKey => orderedKey !== _key));
    }
  };

  const menu = useMemo(() => {
    const options = dropDownOptions.filter(
      filter => !filter.selected && filter.label.toLowerCase().includes(searchQuery.toLowerCase())
    );
    return (
      <div style={memoizedStyle}>
        <SearchInput value={searchQuery} onChange={(query: string) => setSearchQuery(query)} />
        <div className={`widget_defaults_reports_list ${dropdownButtonClass}`}>
          {options.map(filter => (
            <div className={"widget_defaults_reports_list_name"} onClick={() => filterOptionSelected(filter.key)}>
              <AntText className={"widget_defaults_reports_list_name_select"}>{filter.label}</AntText>
            </div>
          ))}
          {((apiData || []).length === 0 || options.length === 0) && (
            <div>
              <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          )}
        </div>
      </div>
    );
  }, [dropDownOptions, searchQuery]);

  const spinner = useMemo(() => {
    return (
      <div className="filters-spinner">
        <Spin />
      </div>
    );
  }, []);

  return (
    <div className={"add_filters_component_container"}>
      <div className={cx("add_filters_component_container_header", { "p-1 pl-0": hideHeader }, { "p-1": !hideHeader })}>
        {!hideHeader && (
          <div className={"add_filters_component_container_header_title"}>
            {report.application && (
              // @ts-ignore
              <IntegrationIcon style={iconStyle} type={report.application} />
            )}
            <AntText>{report.name}</AntText>
          </div>
        )}
        <div>
          <span className={"report_category_dropdown"}>
            <Popover
              className={"search-popover"}
              placement={"bottomLeft"}
              content={menu}
              trigger="click"
              visible={showFiltersPopOver}
              onVisibleChange={handleVisibleChange}>
              <Button
                className={"add_filters_component_container_header_dropdown"}
                onClick={() => setShowFiltersPopOver(!showFiltersPopOver)}
                disabled={apiLoading}>
                Add Filter <Icon type="down" />
              </Button>
            </Popover>
          </span>
          {onDeleteReportFilters && <AntIcon type={"delete"} onClick={() => onDeleteReportFilters(report)} />}
        </div>
      </div>
      {(apiLoading || fieldsLoading) && spinner}
      {!(apiLoading || fieldsLoading) && (
        <div className={"add_filters_component_container_list"}>
          <div>
            {(orderedFilters || []).map((key, index) => {
              if (!key.includes("customfield_") && !uiFilterKeys.includes(key)) {
                return (
                  <APIFilter
                    filterData={getMappedSupportedFilters(key)}
                    filters={filters}
                    supportExcludeFilters={supportExcludeFilters}
                    supportPartialStringFilters={supportPartialStringFilters}
                    handlePartialValueChange={handlePartialValueChange}
                    handleFilterValueChange={onFilterValueChange}
                    handleSwitchValueChange={onExcludeFilterChange}
                    reportType={report.report}
                    activePopkey={activePopKey}
                    handleActivePopkey={(key: any) => setActivePopKey(key)}
                    handleRemoveFilter={removeFilterOptionSelected}
                    useGlobalFilters={true}
                    partialFilterError={partialFiltersErrors}
                  />
                );
              }

              if (
                key.includes("customfield_") &&
                !uiFilterKeys.includes(key) &&
                !timeBasedFields(transformCustomData(key), fieldList)
              ) {
                return (
                  <APIFilter
                    filterData={transformCustomData(key)}
                    filters={filters}
                    supportExcludeFilters={supportExcludeFilters}
                    supportPartialStringFilters={supportPartialStringFilters}
                    handlePartialValueChange={handlePartialValueChange}
                    handleFilterValueChange={onFilterValueChange}
                    handleSwitchValueChange={onExcludeFilterChange}
                    reportType={report.report}
                    activePopkey={activePopKey}
                    handleActivePopkey={(key: any) => setActivePopKey(key)}
                    handleRemoveFilter={removeFilterOptionSelected}
                    isCustom={true}
                    partialFilterError={partialFiltersErrors}
                    fieldTypeList={fieldList}
                  />
                );
              }

              if (
                key.includes("customfield_") &&
                !uiFilterKeys.includes(key) &&
                timeBasedFields(transformCustomData(key), fieldList)
              ) {
                const _itemKey = Object.keys(transformCustomData(key))[0];
                const _itemData = _itemKey.split("@")[0];
                const _itemName = _itemKey.split("@")[1];
                return (
                  <TimeRangeAbsoluteRelativeWrapperComponent
                    key={_itemData}
                    label={_itemName}
                    filterKey={_itemData}
                    metaData={metaData}
                    filters={filters.custom_fields || {}}
                    onFilterValueChange={(value: any, key: string) => {
                      handleTimeRangeFilterValueChange?.(value, key, undefined, true);
                    }}
                    onTypeChange={handleTimeRangeTypeChange}
                    onDelete={removeFilterOptionSelected}
                  />
                );
              }

              if (uiFilterKeys.includes(key)) {
                return UiFilters(
                  filters,
                  key,
                  report,
                  onFilterValueChange,
                  removeFilterOptionSelected,
                  onExcludeFilterChange,
                  handleTimeRangeFilterValueChange,
                  metaData,
                  handleTimeRangeTypeChange,
                  integrationIds
                );
              }
              return null;
            })}
          </div>
          {Object.keys(filters).length === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
        </div>
      )}
    </div>
  );
};

export default memo(AddFiltersComponent);
