import React, { useCallback, useEffect, useMemo, useState } from "react";
import "./WidgetDefaultsWrapper.scss";
import { AntCard, AntText } from "shared-resources/components";
import { Button, Empty, Popover } from "antd";
import Icon from "antd/lib/icon";
import { get, debounce } from "lodash";
import { SearchInput } from "../../../pages/dashboard-drill-down-preview/components/SearchInput.component";
import { getApplicationFilters } from "../helper";
import { DEFAULT_WIDGET_FILTER_INFO_TEXT } from "../constants";
import { getGroupByRootFolderKey } from "../../../../configurable-dashboard/helpers/helper";
import { filesFilters } from "../AddFiltersComponent/filterConstants";
import AddFiltersComponent from "../AddFiltersComponent/AddFiltersComponent";

interface WidgetDefaultWrapperProps {
  integrationIds: Array<any>;
  filters: any;
  onRemoveReportFilters: (report: string) => void;
  onAddReportFilters: (report: string) => void;
  onFilterValueChange: (value: any, uri: string, type?: any, exclude?: boolean) => void;
  handlePartialValueChange: (key: string, value: any, report: string, uri: string) => void;
  handleTimeRangeTypeChange: (key: string, value: any, uri: string) => void;
  handleExcludeFilter: (key: string, value: boolean, uri: string) => void;
  onFilterRemoved: (key: string, uri: string) => void;
  handleTimeRangeFilterValueChange: any;
  orderedFilters: { [Key: string]: string[] };
  setOrderedFilters: any;
  partialFiltersErrors: any;
}

const WidgetDefaultsWrapper: React.FC<WidgetDefaultWrapperProps> = props => {
  const [showMore, setShowMore] = useState<boolean>(false);
  const [showReportsPopOver, setShowReportsPopOver] = useState<boolean>(false);
  const [searchString, setSearchString] = useState<string>("");
  const [selectedReports, setSelectedReports] = useState<any>([]);

  const description = useMemo(() => {
    return !showMore ? DEFAULT_WIDGET_FILTER_INFO_TEXT.substr(0, 76) : DEFAULT_WIDGET_FILTER_INFO_TEXT;
  }, [showMore]);

  const globalFilters = useMemo(
    () =>
      Object.values(getApplicationFilters()).filter(
        report => !Object.keys(props.filters).includes(get(report, ["uri"], ""))
      ),
    [props.filters]
  );

  const getReportsList = useMemo(
    () =>
      Object.values(globalFilters).filter(report =>
        get(report, ["name"], "").toLowerCase().includes(searchString.toLowerCase())
      ),
    [searchString, globalFilters]
  );

  const memoizedStyle = useMemo(() => ({ width: "23.5rem" }), []);

  const handleVisibleChange = useCallback(visible => {
    setShowReportsPopOver(visible);
    setSearchString("");
  }, []);

  const debouncedSearchStringChange = debounce(val => setSearchString(val), 800);

  const addReportFilters = (report: any) => {
    props.onAddReportFilters(report.uri);
    setShowReportsPopOver(false);
  };

  const handleRemoveReport = useCallback(
    (reportToRemove: any) => {
      props.onRemoveReportFilters(reportToRemove.uri);
    },
    [props.filters]
  );

  // Added filtering logic for selected reports for some already set invalid URI in meta data
  useEffect(
    () =>
      setSelectedReports(
        Object.keys(props.filters)
          .map((key: string) => getApplicationFilters()[key])
          .filter((filter: any) => !!filter)
      ),
    [props.filters]
  );

  const menu = useMemo(
    () => (
      <div style={memoizedStyle}>
        <SearchInput value={searchString} onChange={debouncedSearchStringChange} />
        <div className={"widget_defaults_reports_list"}>
          {(getReportsList || []).map((report: any) => (
            <div className={"widget_defaults_reports_list_name"} onClick={() => addReportFilters(report)}>
              <AntText className={"widget_defaults_reports_list_name_select"}>{get(report, ["name"], "")}</AntText>
            </div>
          ))}
          {(getReportsList || []).length === 0 && (
            <div>
              <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          )}
        </div>
      </div>
    ),
    [getReportsList, selectedReports]
  );

  return (
    <div className={"widget_defaults_wrapper"}>
      <span>
        <AntText>{description}</AntText>
        <AntText className={"text-primary"} onClick={() => setShowMore(!showMore)}>
          {!showMore ? "Learn more" : " Show less"}
        </AntText>
      </span>
      <span className={"report_category_dropdown"}>
        <Popover
          className={"search-popover"}
          placement={"bottomLeft"}
          content={menu}
          trigger="click"
          visible={showReportsPopOver}
          onVisibleChange={handleVisibleChange}>
          <Button onClick={() => setShowReportsPopOver(!showReportsPopOver)}>
            Add Report Category <Icon type="down" />
          </Button>
        </Popover>
      </span>
      {selectedReports.length ? (
        <div className={"widget_defaults_wrapper_reports_container"}>
          {selectedReports.map((report: any) => {
            let _filters =
              {
                ...props.filters[report.uri],
                metadata: get(props.filters.metadata, [report.uri], {})
              } || {};
            if (
              filesFilters.includes(report.uri) &&
              (props.filters.metadata?.[report.uri] || {}).hasOwnProperty(getGroupByRootFolderKey(report.uri))
            ) {
              _filters = {
                ..._filters,
                group_by_modules: get(props.filters.metadata, [getGroupByRootFolderKey(report.uri)], false)
              };
            }
            return (
              <AntCard className={"widget_defaults_wrapper_reports_container_card"}>
                <AddFiltersComponent
                  key={report.uri}
                  report={report}
                  onDeleteReportFilters={handleRemoveReport}
                  integrationIds={props.integrationIds}
                  filters={_filters}
                  onExcludeFilterChange={(key, value) => props.handleExcludeFilter(key, value, report.uri)}
                  onFilterValueChange={(value, type, exclude) =>
                    props.onFilterValueChange(value, report.uri, type, exclude)
                  }
                  handlePartialValueChange={(key, value) =>
                    props.handlePartialValueChange(key, value, report.report, report.uri)
                  }
                  handleTimeRangeTypeChange={(key, value) => props.handleTimeRangeTypeChange(key, value, report.uri)}
                  onFilterRemoved={(key: string) => props.onFilterRemoved(key, report.uri)}
                  handleTimeRangeFilterValueChange={(value, type, range, isCustom) =>
                    props.handleTimeRangeFilterValueChange(value, report.uri, type, range, isCustom)
                  }
                  dropdownButtonClass={"filter-select-button"}
                  orderedFilters={props.orderedFilters[report.uri]}
                  setOrderedFilters={(keys: string[]) => {
                    props.setOrderedFilters(keys, report.uri);
                  }}
                  partialFiltersErrors={props.partialFiltersErrors[report.uri]}
                />
              </AntCard>
            );
          })}
        </div>
      ) : (
        <div className={"empty_container"}>
          <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        </div>
      )}
    </div>
  );
};

export default WidgetDefaultsWrapper;
