import React, { useEffect, useMemo, useState } from "react";
import { Dropdown, Empty, Menu, Tag } from "antd";
import { get, upperCase } from "lodash";
import moment from "moment";
import queryString from "query-string";
import { useDispatch, useSelector } from "react-redux";

import "./FilterButton.scss";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  filterCount,
  getGlobalFiltersHelper,
  getJiraOrFiltersHelper,
  globalCustomFieldFiltersHelper,
  jiraOrFiltersCustomFiltersHelper,
  different_value_format_fields,
  lt_gt_format_fields,
  time_Range_Filters_fields
} from "../helper";
import JiraOrFiltersContainer from "../../../../configurable-dashboard/components/configure-widget/configuration/tabs/JiraOrFiltersContainer";
import CustomFieldsApiComponent from "../../../../configurable-dashboard/components/configure-widget/configuration/tabs/CustomFieldsApiComponent";
import GithubKeysFilters from "../GithubKeysFilterComponent";
import MicrosoftGlobalFilters from "../MicrosoftFilterComponent";
import Loader from "components/Loader/Loader";
import { OR_QUERY_APPLICATIONS } from "dashboard/pages/dashboard-drill-down-preview/helper";
import { AntButton, AntText, AntBadge } from "../../../../shared-resources/components";
import {
  getApplicationFilters,
  sanitizeGlobalMetaDataFilters,
  sanitizeReportGlobalFilters
} from "dashboard/components/dashboard-application-filters/helper";
import {
  cachedIntegrationsLoadingAndError,
  cachedIntegrationsListSelector
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { getCachedIntegrations } from "reduxConfigs/actions/cachedIntegrationActions";
import { IntegrationTypes } from "constants/IntegrationTypes";

type triggerType = ("click" | "hover" | "contextMenu")[] | undefined;

interface filtersProps {
  jiraOrFilters: any;
  globalFilters: any;
}

interface FilterButtonProps {
  showFiltersDropDown: boolean;
  setShowFiltersDropDown: (value: boolean) => void;
  handleGlobalFilterButtonClick: any;
  integrationIds: any;
  filters: filtersProps;
  isReportFilter?: boolean;
  filtersCount: number;
  disableActions: boolean;
}

const FilterButton: React.FC<FilterButtonProps> = ({
  showFiltersDropDown,
  setShowFiltersDropDown,
  handleGlobalFilterButtonClick,
  integrationIds,
  filters,
  isReportFilter,
  filtersCount,
  disableActions
}) => {
  const getTriggers: triggerType = useMemo(() => ["click"], []);
  const [containGithubIntegration, setContainGithubIntegration] = useState(false);
  const [containJiraIntegration, setContainJiraIntegration] = useState(false);
  const [loadingIntegrations, setLoadingIntegrations] = useState<boolean>(true);
  const dispatch = useDispatch();
  const integrationsLoadingState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const [application, setApplication] = useState<string>("");

  useEffect(() => {
    if (integrations.length === 0 && !isReportFilter) {
      dispatch(getCachedIntegrations("list", undefined, integrationIds));
    }

    if (isReportFilter) {
      setApplication(queryString.parse(window.location.href.split("?")[1]).application as string);
      setLoadingIntegrations(false);
    }
  }, []);

  useEffect(() => {
    const loadingIntegrations = get(integrationsLoadingState, ["loading"], true);
    const error = get(integrationsLoadingState, ["error"], true);
    if (!loadingIntegrations && !error) {
      setContainGithubIntegration(
        !!integrations?.find((integration: any) => integration.application === IntegrationTypes.GITHUB)
      );
      setContainJiraIntegration(
        !!integrations?.find((integration: any) => integration.application === IntegrationTypes.JIRA)
      );
      setLoadingIntegrations(false);
    }
  }, [integrationsLoadingState, integrations]);

  const jiraOrCustomFieldsFilters = useMemo(() => {
    return jiraOrFiltersCustomFiltersHelper(filters?.jiraOrFilters);
  }, [filters.jiraOrFilters]);

  const getJiraOrCustomFieldsCount = useMemo(() => {
    let count = 0;
    Object.values(jiraOrCustomFieldsFilters).forEach(fieldValues => {
      count = count + filterCount(fieldValues || {});
    });
    return count;
  }, [jiraOrCustomFieldsFilters]);

  const globalCustomFieldsFilters = useMemo(() => {
    return globalCustomFieldFiltersHelper(filters.globalFilters || {});
  }, [filters.globalFilters]);

  const githubPrsFilters = useMemo(
    () => get(filters, ["globalFilters", "github_prs_filter_values"], {}),
    [filters.globalFilters]
  );

  const microSoftFilters = useMemo(
    () => get(filters, ["globalFilters", "microsoft_issues_filter_values"], {}),
    [filters.globalFilters]
  );

  const githubCommitsFilters = useMemo(
    () => get(filters, ["globalFilters", "github_commits_filter_values"], {}),
    [filters.globalFilters]
  );

  const getGlobalFilters = useMemo(() => {
    return getGlobalFiltersHelper(filters.globalFilters);
  }, [filters.globalFilters]);

  const getJiraOrFilters = useMemo(() => {
    return getJiraOrFiltersHelper(filters.jiraOrFilters, "jira_filter_values");
  }, [filters.jiraOrFilters]);

  const getWidgetFiltersCount = useMemo(() => {
    let count = 0;
    let _global_filters = filters?.globalFilters;
    if (isReportFilter) {
      _global_filters = sanitizeReportGlobalFilters(filters.globalFilters);
    } else {
      _global_filters = sanitizeGlobalMetaDataFilters(filters?.globalFilters);
    }
    let all_global_filters = Object.keys(getApplicationFilters());
    for (const [key, value] of Object.entries(_global_filters)) {
      if (!isReportFilter) {
        // Added this logic for some already set invalid URI in filters
        if (all_global_filters.includes(key)) {
          // @ts-ignore
          count = count + filterCount(value);
        }
      } else {
        // @ts-ignore
        count = count + filterCount(value);
      }
    }
    return count;
  }, [filters.globalFilters]);

  const getDashFiltersCount = useMemo(() => filterCount(filters?.jiraOrFilters || {}), [filters.jiraOrFilters]);

  const showDashboardFilters = isReportFilter
    ? OR_QUERY_APPLICATIONS.filter((key: string) => application.includes(key)).length > 0 && containJiraIntegration
    : containJiraIntegration;

  const menu = useMemo(() => {
    const jiraPreview = (
      <div className={"menu-filters-list"}>
        {getJiraOrFilters.length > 0 && <JiraOrFiltersContainer jiraOrFilters={getJiraOrFilters} />}

        {getJiraOrCustomFieldsCount > 0 && (
          <CustomFieldsApiComponent integrationIds={integrationIds} dashboardCustomFields={jiraOrCustomFieldsFilters} />
        )}

        {getDashFiltersCount === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
      </div>
    );
    return (
      <Menu className={"filters-menu"}>
        <Menu.Item className={"flex menu-item-title"}>
          <span>Insight Filters({getDashFiltersCount})</span>
          {!disableActions && (
            <a
              className={"edit-filters-link"}
              onClick={() => {
                setShowFiltersDropDown(false);
                handleGlobalFilterButtonClick();
              }}>
              Add/Edit filters
            </a>
          )}
        </Menu.Item>
        {jiraPreview}
        <Menu.Item className={"flex menu-item-title"}>
          {isReportFilter ? "Report Filters" : "Widget Defaults"}({getWidgetFiltersCount})
        </Menu.Item>
        <div className={"menu-filters-list"}>
          {(getGlobalFilters || []).map((filter: any) => (
            <div className="widget-default-filters-list" key={filter.key}>
              <AntText className={"widget-default-filters-list_label"}>
                {upperCase(filter.label)} {filter.missing_field && " (Missing Field)"}
              </AntText>
              {(filter.exclude || filter.partial) && (
                <>
                  {filter.exclude && <AntText className={"widget-default-filters-list_extras"}>Excludes</AntText>}
                  {filter.partial && (
                    <AntText className={"widget-default-filters-list_extras"}>
                      {`Includes all the values that: ${filter.partial}`}
                    </AntText>
                  )}
                </>
              )}
              <div>
                {!different_value_format_fields.includes(filter?.key) &&
                  filter.value &&
                  Array.isArray(filter.value) &&
                  filter?.value?.map((filter_val: any) => {
                    return <Tag key={filter_val} className="widget-default-filters-list_tags">{`${filter_val}`}</Tag>;
                  })}
                {!different_value_format_fields.includes(filter?.key) &&
                  filter.value &&
                  !Array.isArray(filter.value) && (
                    <Tag key={filter.value} className="widget-default-filters-list_tags">{`${filter.value}`}</Tag>
                  )}
                {lt_gt_format_fields.includes(filter?.key) && (
                  <>
                    <Tag key={filter} className="widget-default-filters-list_tags">
                      {`${filter?.value[0]} `} - {` ${filter?.value[1]}`}
                    </Tag>
                  </>
                )}
                {time_Range_Filters_fields.includes(filter?.key) && (
                  <Tag key={filter} className="widget-default-filters-list_tags">
                    {`${moment.unix(parseInt(filter?.value[0])).utc().format("MM-DD-YYYY")} `} -{" "}
                    {` ${moment.unix(parseInt(filter?.value[1])).format("MM-DD-YYYY")}`}
                  </Tag>
                )}
              </div>
            </div>
          ))}
          {Object.keys(globalCustomFieldsFilters).length > 0 && (
            <CustomFieldsApiComponent
              integrationIds={integrationIds}
              dashboardCustomFields={globalCustomFieldsFilters}
            />
          )}
          {containGithubIntegration && (
            <div style={{ marginTop: "15px" }}>
              <GithubKeysFilters
                githubPrsFilter={githubPrsFilters}
                githubCommitsFilter={githubCommitsFilters}
                integrationIds={integrationIds}
              />
            </div>
          )}
          {
            <div style={{ marginTop: "15px" }}>
              <MicrosoftGlobalFilters microSoftFilter={microSoftFilters} integrationIds={integrationIds} />
            </div>
          }
          {getWidgetFiltersCount === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
        </div>
        {/* <div className="menu-item-sticky-bottom">
          <a
            className={"edit-filters-link"}
            onClick={() => {
              setShowFiltersDropDown(false);
              handleGlobalFilterButtonClick();
            }}>
            Edit filters
          </a>
        </div> */}
      </Menu>
    );
  }, [containJiraIntegration, containGithubIntegration, getJiraOrFilters, globalCustomFieldsFilters]);

  const showFilterDropdown = !!filtersCount && !loadingIntegrations;

  const renderDropdown = () => {
    if (loadingIntegrations) {
      return <Loader />;
    }

    return (
      <Dropdown
        overlay={menu}
        overlayClassName={"action-buttons"}
        visible={showFiltersDropDown}
        trigger={getTriggers}
        placement="bottomRight"
        onVisibleChange={val => setShowFiltersDropDown(val)}>
        <AntButton className="action-button-border" icon="filter" onClick={() => setShowFiltersDropDown(true)} />
      </Dropdown>
    );
  };

  return (
    <AntBadge
      count={filtersCount}
      // className={cx({ "mr-1": filtersCount < 9 }, { "mr-2": !(filtersCount < 9) })}
      style={{
        backgroundColor: "rgb(46, 109, 217)",
        zIndex: "3"
      }}>
      {/* <AntButtonGroup className={cx({ "filter-button": showFilterDropdown })}>
        <AntButton icon="filter" className="action-button-border">
        </AntButton>
      </AntButtonGroup> */}
      {renderDropdown()}
    </AntBadge>
  );
};

export default FilterButton;
