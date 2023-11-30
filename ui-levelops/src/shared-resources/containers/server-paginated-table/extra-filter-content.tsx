import * as React from "react";
import { Button, Dropdown, Icon, Tooltip } from "antd";
import { AntButton, AntButtonGroup, AntInput, CustomFilterButton, SvgIcon } from "shared-resources/components";
import "./extra-filter-content.style.scss";
import { AntBadgeComponent } from "shared-resources/components/ant-badge/ant-badge.component";
import {
  DashboardActionButtonLabelType,
  filterCount,
  getReportFilterCount
} from "dashboard/components/dashboard-header/helper";
import { DashboardHeaderPreviewFilter } from "dashboard/components/dashboard-header/filter-preview/filter-preview.component";
import { sanitizeReportGlobalFilters } from "../../../dashboard/components/dashboard-application-filters/helper";
import DrilldownColumnSelecor from "./components/drilldown-filter-content/DrilldownColumnSelector";
import { ColumnProps } from "antd/lib/table";

interface ExtraFilterContentProps {
  hasSearch: boolean;
  hasFilters: boolean;
  hasDelete: boolean;
  hasAppliedFilters: boolean;
  generalSearchField: string;
  onGeneralSearchHandler: any;
  countForAppliedFilters?: string | number;
  onToggleFilters?: any;
  downloadCSV?: any;
  clearFilters?: any;
  handleCSVDownload?: any;
  setShowDeletePopup?: any;
  selectedRows: number;
  partialParsed?: any;
  integrationIds?: any;
  filters?: any;
  showFiltersDropDown?: boolean;
  setShowFiltersDropDown?: (val: boolean) => void;
  showCustomFilters?: boolean;
  showFilters?: boolean;
  showTriageGridFilters?: boolean;
  filtersConfig?: any;
  customExtraContent?: any;
  extraSuffixActionButtons?: React.ReactNode;
  searchPlaceholder?: string;
  showUsersFilters?: boolean;
  showIssueFilters?: boolean;
  reportType?: string;
  availableColumns?: Array<{ title: string; dataIndex: string }>;
  visibleColumns?: Array<ColumnProps<any>>;
  widgetId?: string;
  defaultColumns?: Array<ColumnProps<any>>;
  setSelectedColumns?: (selectedColumns: Array<string>) => {};
  isDevRawStatsDrilldown?: boolean;
  hideFilterButton?: boolean;
  showOnlyFilterIcon?: boolean;
  newSearch?: boolean;
}

export const ExtraFilterContent: React.FC<ExtraFilterContentProps> = ({
  downloadCSV,
  hasSearch,
  hasAppliedFilters,
  hasDelete,
  hasFilters,
  countForAppliedFilters,
  generalSearchField,
  onGeneralSearchHandler,
  onToggleFilters,
  clearFilters,
  handleCSVDownload,
  setShowDeletePopup,
  selectedRows,
  partialParsed,
  integrationIds,
  filters,
  showFiltersDropDown,
  setShowFiltersDropDown,
  showCustomFilters,
  filtersConfig,
  showFilters,
  showTriageGridFilters,
  customExtraContent,
  extraSuffixActionButtons,
  searchPlaceholder,
  showUsersFilters,
  showIssueFilters,
  reportType,
  visibleColumns,
  availableColumns,
  widgetId,
  defaultColumns,
  setSelectedColumns,
  isDevRawStatsDrilldown,
  hideFilterButton,
  showOnlyFilterIcon = false,
  newSearch = false
}) => {
  const [showColumnList, setShowColumnList] = React.useState<boolean>(false);
  const [newSearchToggle, setNewSearchToggle] = React.useState<boolean>(false);
  const showDelete = selectedRows > 0 && hasDelete;
  if (!hasSearch && !hasFilters && !showDelete && !downloadCSV && !extraSuffixActionButtons && !customExtraContent) {
    return null;
  }

  const filtercount = () => {
    if (!showIssueFilters && !showUsersFilters && !showTriageGridFilters && showCustomFilters) {
      let count = filterCount(filters?.jiraOrFilters || {});
      count =
        count +
        getReportFilterCount(sanitizeReportGlobalFilters(filters?.globalFilters)?.alltypes || {}, filtersConfig);
      return count;
    } else {
      return filterCount(filters);
    }
  };

  return (
    <>
      <div className="flex direction-row justify-end extra-filter-content">
        {newSearch && (
          <>
            {newSearchToggle && (
              <AntInput
                id={`${generalSearchField}-search`}
                placeholder={searchPlaceholder || "Search ... "}
                onChange={onGeneralSearchHandler}
                name="general-search"
                className="extra-filter-content__search"
                value={(partialParsed && partialParsed[generalSearchField]) || ""}
              />
            )}
            {
              <Button
                className="user-search"
                type="primary"
                icon="search"
                onClick={() => setNewSearchToggle(!newSearchToggle)}
              />
            }
          </>
        )}
        {hasSearch && newSearch === false && (
          <AntInput
            id={`${generalSearchField}-search`}
            placeholder={searchPlaceholder || "Search ... "}
            type="search"
            onChange={onGeneralSearchHandler}
            name="general-search"
            className="extra-filter-content__search"
            value={(partialParsed && partialParsed[generalSearchField]) || ""}
          />
        )}
        {hasFilters && !showCustomFilters && (
          <CustomFilterButton
            count={countForAppliedFilters as number}
            onFilterClick={onToggleFilters}
            onClearFilter={clearFilters}
          />
        )}

        {hasFilters && showCustomFilters && !showFilters && (
          <AntBadgeComponent
            count={filtercount()}
            className={`ml-10 mr-5  ${showDelete ? "hide-badge" : ""}`}
            style={{
              backgroundColor: "rgb(46, 109, 217)",
              zIndex: "3"
            }}>
            <AntButtonGroup className="flex">
              {!hideFilterButton &&
                (showOnlyFilterIcon ? (
                  <Tooltip title="Filters" trigger="hover">
                    <AntButton
                      type="link"
                      icon="filter"
                      className="only-icon action-button-border"
                      onClick={() => onToggleFilters()}
                    />
                  </Tooltip>
                ) : (
                  <AntButton icon="filter" className="action-button-border" onClick={() => onToggleFilters()}>
                    {DashboardActionButtonLabelType.GLOBAL_FILTERS}
                  </AntButton>
                ))}
              {filtercount() > 0 && (
                <DashboardHeaderPreviewFilter
                  handleGlobalFilterButtonClick={() => onToggleFilters()}
                  setShowFiltersDropDown={val => {
                    //@ts-ignore
                    setShowFiltersDropDown(val);
                  }}
                  filtersConfig={filtersConfig}
                  showFiltersDropDown={showFiltersDropDown ? showFiltersDropDown : false}
                  integrationIds={integrationIds}
                  filters={filters}
                  isReportFilter={true}
                  reportType={reportType}
                  showIssueFilters={showIssueFilters}
                  showTriageGridFilters={showTriageGridFilters}
                  showUsersFilters={showUsersFilters}
                  hideFilterButton={hideFilterButton}
                />
              )}
            </AntButtonGroup>
          </AntBadgeComponent>
        )}
        {availableColumns?.length ? (
          <Dropdown
            overlay={
              <DrilldownColumnSelecor
                visibleColumns={visibleColumns || []}
                availableColumns={availableColumns}
                widgetId={widgetId}
                closeDropdown={() => setShowColumnList(false)}
                defaultColumns={defaultColumns || []}
                onColumnsChange={setSelectedColumns}
              />
            }
            trigger={["click"]}
            placement="bottomRight"
            visible={showColumnList}
            onVisibleChange={setShowColumnList}>
            <Tooltip title="Select Columns">
              <Button className="drilldown-icon">
                <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                  <SvgIcon className="reports-btn-icon" icon="selectColumn" />
                </div>
              </Button>
            </Tooltip>
          </Dropdown>
        ) : null}
        {downloadCSV && isDevRawStatsDrilldown && (
          <div className="drilldown-filter-content">
            <Tooltip title="Download">
              <Button onClick={handleCSVDownload} className="drilldown-icon">
                <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                  <SvgIcon className="reports-btn-icon" icon="download" />
                </div>
              </Button>
            </Tooltip>
          </div>
        )}
        {downloadCSV && !isDevRawStatsDrilldown && (
          <Tooltip title="Download">
            <Button onClick={handleCSVDownload} className="extra-filter-content__download-icon">
              <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                <SvgIcon className="reports-btn-icon" icon="download" />
              </div>
            </Button>
          </Tooltip>
        )}
        {customExtraContent}
        {showDelete && (
          <AntButton
            className="extra-filter-content__delete-btn mr-5"
            onClick={setShowDeletePopup}
            icon="delete"
            type={"default"}
          />
        )}

        {extraSuffixActionButtons && extraSuffixActionButtons}
      </div>
    </>
  );
};
