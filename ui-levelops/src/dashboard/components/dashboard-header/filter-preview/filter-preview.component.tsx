import * as React from "react";
import { Dropdown, Empty, Tag, Menu } from "antd";
import { AntButton, AntText, AntTooltip } from "../../../../shared-resources/components";
import { useMemo } from "react";
import { useState } from "react";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { useEffect } from "react";
import { get, upperCase } from "lodash";
import {
  filterCount,
  getGlobalFiltersHelper,
  getJiraOrFiltersHelper,
  jiraOrFiltersCustomFiltersHelper,
  globalCustomFieldFiltersHelper,
  different_value_format_fields,
  lt_gt_format_fields,
  time_Range_Filters_fields
} from "../helper";
import JiraOrFiltersContainer from "../../../../configurable-dashboard/components/configure-widget/configuration/tabs/JiraOrFiltersContainer";
import CustomFieldsApiComponent from "../../../../configurable-dashboard/components/configure-widget/configuration/tabs/CustomFieldsApiComponent";
import { STARTS_WITH } from "../../../constants/constants";
import widgetConstants, { getWidgetConstant } from "../../../constants/widgetConstants";
import moment from "moment";
import GithubKeysFilters from "../GithubKeysFilterComponent";
import MicrosoftGlobalFilters from "../MicrosoftFilterComponent";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import Loader from "components/Loader/Loader";
import queryString from "query-string";
import { OR_QUERY_APPLICATIONS } from "dashboard/pages/dashboard-drill-down-preview/helper";
import {
  getApplicationFilters,
  sanitizeGlobalMetaDataFilters,
  sanitizeReportGlobalFilters
} from "../../dashboard-application-filters/helper";
import TriageGridViewFilters from "./triageFilterComponent";
import UserFilterComponent from "./userFilterComponent";
import LevelopsFiltersPreview from "../LevelopsFiltersPreview";
import { workItemSupportedFilters } from "../constants";
import ApiFiltersPreview from "dashboard/components/widget-filter-preview/ApiFiltersPreview";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { doraReports } from "dashboard/constants/applications/names";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { useLocation } from "react-router-dom";
import { IntegrationTypes } from "constants/IntegrationTypes";
const LONG_TAG_THRESHOLD = 25;
type triggerType = ("click" | "hover" | "contextMenu")[] | undefined;
interface filtersProps {
  jiraOrFilters: any;
  globalFilters: any;
}
interface DashboardHeaderPreviewFilterProps {
  showFiltersDropDown: boolean;
  setShowFiltersDropDown: (value: boolean) => void;
  handleGlobalFilterButtonClick: any;
  integrationIds: any;
  filters: filtersProps;
  isReportFilter?: boolean;
  showTriageGridFilters?: boolean;
  showUsersFilters?: boolean;
  showIssueFilters?: boolean;
  reportType?: string;
  filtersConfig?: any;
  hideFilterButton?: boolean;
}

// TODO: IT's a duplicate of FilterButton. Move duplicate code to central place, rename this component & move to shared dir.
export const DashboardHeaderPreviewFilter: React.FC<DashboardHeaderPreviewFilterProps> = ({
  showFiltersDropDown,
  setShowFiltersDropDown,
  handleGlobalFilterButtonClick,
  integrationIds,
  filters,
  isReportFilter,
  showTriageGridFilters,
  showUsersFilters,
  showIssueFilters,
  reportType,
  filtersConfig,
  hideFilterButton = false
}) => {
  const getTriggers: triggerType = useMemo(() => ["click"], []);
  const [containGithubIntegration, setContainGithubIntegration] = useState(false);
  const [containJiraIntegration, setContainJiraIntegration] = useState(false);
  const [loading, setLoading] = useState<boolean>(true);
  const dispatch = useDispatch();
  const location = useLocation();
  const queries = queryString.parse(location.search);
  const queryOUFilters = get(queries, "OUFilter") ? JSON.parse(get(queries, "OUFilter", "") as string) : {};
  const queryParamOUArray = get(queryOUFilters, ["ou_ids"], undefined);
  const queryParamOU = queryParamOUArray ? queryParamOUArray?.[0] : "";
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  const integrationsState = useParamSelector(integrationListSelector, {
    integration_key: (integrationIds || []).sort().join("_")
  });
  const [application, setApplication] = useState<string>("");

  const getFilters = () => {
    const { globalFilters } = filters;
    const integrationIds: any = get(globalFilters, ["alltypes", "integration_ids"]);
    if (integrationIds) {
      const integrationsList = filtersConfig.find((child: any) => child.id === "integration_id").options;
      const f = integrationIds.map((value: any) => {
        return integrationsList.find((item: any) => item.value === value)?.label;
      });
      delete globalFilters.alltypes["integration_ids"];
      globalFilters.alltypes = {
        ...globalFilters.alltypes,
        integrations: f
      };
    }
    return globalFilters;
  };

  useEffect(() => {
    const data = get(integrationsState, ["data", "records"], []);
    if (data.length === 0 && !isReportFilter) {
      dispatch(
        genericList(
          "integrations",
          "list",
          { filter: { integration_ids: integrationIds } },
          null,
          integrationIds.sort().join("_")
        )
      );
    }

    if (isReportFilter) {
      const application = queryString.parse(window.location.href.split("?")[1]).application as string;
      setApplication(application);
      if (application === IntegrationTypes.GITHUB) {
        setContainGithubIntegration(true);
      }
      if (application === IntegrationTypes.JIRA) {
        setContainJiraIntegration(true);
      }
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const loading = get(integrationsState, ["loading"], true);
    const error = get(integrationsState, ["error"], true);
    if (!loading && !error) {
      const data = get(integrationsState, ["data", "records"], []);
      setContainGithubIntegration(
        !!data.find((integration: any) => integration.application === IntegrationTypes.GITHUB)
      );
      setContainJiraIntegration(!!data.find((integration: any) => integration.application === IntegrationTypes.JIRA));
      setLoading(false);
    }
  }, [integrationsState]);

  const uri = useMemo(() => {
    let _uri = get(widgetConstants, [reportType as string, "supported_filters", "uri"], "default_uri");
    const conditionalUriMethod = get(widgetConstants, [reportType as string, "conditionalUriMethod"], undefined);
    if (conditionalUriMethod) {
      if (doraReports.includes(reportType as any)) {
        _uri = conditionalUriMethod({
          integrationState: integrations,
          workspaceProfile: workspaceOuProfilestate,
          reportType: reportType
        });
      }
    }
    return _uri;
  }, [reportType, integrations, workspaceOuProfilestate]);

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

  const globalCustomFieldsFilters: any = useMemo(() => {
    if (showTriageGridFilters || showUsersFilters || showIssueFilters) {
      return {};
    }

    let _global_filters = {};
    if (isReportFilter) {
      _global_filters = sanitizeReportGlobalFilters(filters.globalFilters);
    } else {
      _global_filters = sanitizeGlobalMetaDataFilters(filters.globalFilters);
    }
    return globalCustomFieldFiltersHelper(_global_filters || {});
  }, [filters.globalFilters]);

  const globalFieldsComponentLoad = useMemo(() => {
    if (
      Object.keys(globalCustomFieldsFilters.normalcustomfields || {}).length ||
      Object.keys(globalCustomFieldsFilters.exclude || {}).length ||
      Object.keys(globalCustomFieldsFilters.missing_fields || {}).length ||
      Object.keys(globalCustomFieldsFilters.partial || {}).length
    ) {
      return true;
    }
    return false;
  }, [globalCustomFieldsFilters]);

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
    const apiBasedFilters = getWidgetConstant(reportType, "API_BASED_FILTER", []);
    let _global_filters = getFilters();
    if (isReportFilter) {
      _global_filters = sanitizeReportGlobalFilters(filters?.globalFilters);
    } else {
      _global_filters = sanitizeGlobalMetaDataFilters(filters?.globalFilters);
    }
    const allfilters = getGlobalFiltersHelper(_global_filters, reportType);
    const data = {
      filters: allfilters.filter((item: any) => !apiBasedFilters.includes(item.key)),
      apiBasedFilter: allfilters.filter((item: any) => apiBasedFilters.includes(item.key))
    };
    return data;
  }, [filters.globalFilters, reportType]);

  const getJiraOrFilters = useMemo(() => {
    return getJiraOrFiltersHelper(filters.jiraOrFilters, "jira_filter_values");
  }, [filters.jiraOrFilters]);

  const getWidgetFiltersCount = useMemo(() => {
    if (showTriageGridFilters || showUsersFilters || showIssueFilters) {
      return filterCount(filters);
    }

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
  }, [filters]);

  const getDashFiltersCount = useMemo(() => filterCount(filters?.jiraOrFilters || {}), [filters.jiraOrFilters]);

  const menu = useMemo(() => {
    const jiraPreview = (
      <div className={"menu-filters-list pb-24"}>
        {getJiraOrFilters.length > 0 && <JiraOrFiltersContainer jiraOrFilters={getJiraOrFilters} />}

        {getJiraOrCustomFieldsCount > 0 && (
          <CustomFieldsApiComponent integrationIds={integrationIds} dashboardCustomFields={jiraOrCustomFieldsFilters} />
        )}

        {getDashFiltersCount === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
      </div>
    );

    const showDashboardFilters = isReportFilter
      ? OR_QUERY_APPLICATIONS.filter((key: string) => (application || "").includes(key)).length > 0 &&
        containJiraIntegration
      : containJiraIntegration;

    const title = isReportFilter ? (showUsersFilters ? "Filters" : "Report Filters") : "Widget Defaults";

    return (
      <Menu className={"filters-menu"}>
        {showDashboardFilters && (
          <Menu.Item className={"menu-item-title"}>Insight Filters({getDashFiltersCount})</Menu.Item>
        )}
        {showDashboardFilters && jiraPreview}
        <Menu.Item className={"menu-item-title"}>
          {title}({getWidgetFiltersCount})
        </Menu.Item>
        <div className={"menu-filters-list"}>
          {(getGlobalFilters.filters || []).map((filter: any) => (
            <div className="widget-default-filters-list" key={filter.label}>
              <AntText className={"widget-default-filters-list_label"}>
                {upperCase(filter.label)} {filter.missing_field && " (Missing Field)"}
              </AntText>
              {(filter.exclude || filter.partial) && (
                <>
                  {filter.exclude && <AntText className={"widget-default-filters-list_extras"}>Excludes</AntText>}
                  {filter.partial && (
                    <AntText className={"widget-default-filters-list_extras"}>
                      Includes all the values that:
                      {filter.partial === STARTS_WITH ? " Start With" : " Contains"}
                    </AntText>
                  )}
                </>
              )}
              <div>
                {!different_value_format_fields.includes(filter?.key ?? filter?.label) &&
                  filter.value &&
                  Array.isArray(filter.value) &&
                  filter?.value?.map((filter_val: any) => {
                    const isLongTag = filter_val?.length > LONG_TAG_THRESHOLD;
                    if (isLongTag) {
                      const tagElem = (
                        <Tag key={filter_val} className="tag-paginated-select__tag">
                          {isLongTag ? `${filter_val.slice(0, LONG_TAG_THRESHOLD)}...` : filter_val}
                        </Tag>
                      );
                      return (
                        <AntTooltip title={filter_val} key={filter_val}>
                          {tagElem}
                        </AntTooltip>
                      );
                    } else {
                      return <Tag key={filter_val} className="widget-default-filters-list_tags">{`${filter_val}`}</Tag>;
                    }
                  })}
                {!different_value_format_fields.includes(filter?.key ?? filter?.label) &&
                  filter.value &&
                  !Array.isArray(filter.value) && (
                    <Tag key={filter.value} className="widget-default-filters-list_tags">{`${filter.value}`}</Tag>
                  )}
                {lt_gt_format_fields.includes(filter?.key ?? filter?.label) && (
                  <>
                    <Tag key={filter} className="widget-default-filters-list_tags">
                      {`${filter?.value[0]} `} - {` ${filter?.value[1]}`}
                    </Tag>
                  </>
                )}
                {time_Range_Filters_fields.includes(filter?.key ?? filter?.label) && (
                  <Tag key={filter} className="widget-default-filters-list_tags">
                    {`${moment.unix(parseInt(filter?.value[0])).utc().format("MM-DD-YYYY")} `} -{" "}
                    {` ${moment.unix(parseInt(filter?.value[1])).utc().format("MM-DD-YYYY")}`}
                  </Tag>
                )}
              </div>
            </div>
          ))}
          {
            <ApiFiltersPreview
              filters={getGlobalFilters.apiBasedFilter as any}
              uri={uri}
              reportType={reportType as string}
              integrationIds={integrationIds}
            />
          }
          {globalFieldsComponentLoad && (
            <div style={{ marginTop: "15px" }}>
              <CustomFieldsApiComponent
                integrationIds={integrationIds}
                dashboardCustomFields={globalCustomFieldsFilters}
                application={application}
              />
            </div>
          )}
          {(Object.keys(githubPrsFilters || {}).length > 0 || Object.keys(githubCommitsFilters || {}).length > 0) && (
            <div style={{ marginTop: "15px" }}>
              <GithubKeysFilters
                githubPrsFilter={githubPrsFilters}
                githubCommitsFilter={githubCommitsFilters}
                integrationIds={integrationIds}
              />
            </div>
          )}
          {Object.keys(microSoftFilters || {}).length > 0 && (
            <div style={{ marginTop: "15px" }}>
              <MicrosoftGlobalFilters microSoftFilter={microSoftFilters} integrationIds={integrationIds} />
            </div>
          )}
          {showTriageGridFilters && <TriageGridViewFilters filters={filters} />}
          {showUsersFilters && <UserFilterComponent filters={filters} />}
          {/*TODO: change this component */}
          {showIssueFilters && (
            <LevelopsFiltersPreview
              filters={filters}
              integrationIds={integrationIds || []}
              reportType={"issueFilters"}
              supportedFilters={workItemSupportedFilters}
              dateInTimeStamp={false}
            />
          )}
          {getWidgetFiltersCount === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
        </div>
        <div className={"menu-item-sticky-bottom"}>
          {!hideFilterButton && (
            <a
              className={"edit-filters-link"}
              onClick={() => {
                setShowFiltersDropDown(false);
                handleGlobalFilterButtonClick();
              }}>
              Edit filters
            </a>
          )}
        </div>
      </Menu>
    );
  }, [
    filters,
    containJiraIntegration,
    containGithubIntegration,
    getJiraOrFilters,
    globalCustomFieldsFilters,
    hideFilterButton
  ]);
  if (loading) {
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
      <AntButton icon="unordered-list" className="action-button-border" onClick={() => setShowFiltersDropDown(true)} />
    </Dropdown>
  );
};
