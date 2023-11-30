import React, { useCallback, useMemo } from "react";
import { RestDashboard } from "../../../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getJiraOrFiltersHelper } from "../../../../../../dashboard/components/dashboard-header/helper";
import JiraOrFiltersContainer from "../../../../../../configurable-dashboard/components/configure-widget/configuration/tabs/JiraOrFiltersContainer";
import CustomFieldsApiComponent from "../../../../../../configurable-dashboard/components/configure-widget/configuration/tabs/CustomFieldsApiComponent";
import { Empty } from "antd";
import { get } from "lodash";
import "./dashboard-filters-component.scss";
import { AntText } from "shared-resources/components";
import { DASHBOARD_ROUTES, getBaseUrl } from "../../../../../../constants/routePaths";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface DashboardFilterComponent {
  dashboardId: string;
}

const DashboardFilterComponent: React.FC<DashboardFilterComponent> = ({ dashboardId }) => {
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const projectParams = useParams<ProjectPathProps>();
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const getJiraOrFilters = useMemo(() => {
    const jiraOrFilters = getJiraOrFiltersHelper(dashboard.jira_or_query, "jira_filter_values");
    return jiraOrFilters;
  }, [dashboard]);

  const jiraOrcustomFileldsFilters = useMemo(() => {
    const customFields = get(dashboard, ["_metadata", "jira_or_query", "custom_fields"], {});
    const excludeCustomFields = get(dashboard, ["_metadata", "jira_or_query", "exclude", "custom_fields"], {});
    const partialFilters = get(dashboard, ["_metadata", "jira_or_query", "partial_match"], {});
    const partialobjectKeys = Object.keys(partialFilters);
    let partialCustomFields: any = {};
    partialobjectKeys.forEach((partialKey: any) => {
      if (partialKey.includes("customfield_")) {
        partialCustomFields = { ...partialCustomFields, [partialKey]: partialFilters[partialKey] };
      }
    });
    const jiraOrcustomFilters = {
      normalcustomfields: { ...customFields },
      exclude: { ...excludeCustomFields },
      partial: { ...partialCustomFields }
    };
    return jiraOrcustomFilters;
  }, [dashboard]);

  const getExcludedFilters = useCallback(
    (type: string) => {
      return get(dashboard, ["_metadata", type, "exclude"]);
    },
    [dashboard]
  );

  const handleRedirection = () => {
    const url = `${getBaseUrl(projectParams)}${DASHBOARD_ROUTES._ROOT}/${dashboardId}?editDashboardFilters=true`;
    window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
  };

  const integrationIds = get(dashboard, ["_query", "integration_ids"], []).sort();
  return (
    <div className={"dashboard-filters-component-container"}>
      <div className={"dashboard-filters-component-container-description"}>
        <AntText>
          Defines default values for commonly used widget filters on the dashboard. Used when most widgets on a
          dashboard are about a specific issue type or a project
        </AntText>
      </div>
      <div className={"dashboard-filters-component-container-list"}>
        {(getJiraOrFilters || []).length === 0 && (
          <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}
        {getJiraOrFilters.length > 0 && (
          <JiraOrFiltersContainer jiraOrFilters={getJiraOrFilters} filterWidth={"half"} />
        )}

        {Object.keys(jiraOrcustomFileldsFilters).length > 0 && (
          <CustomFieldsApiComponent
            integrationIds={integrationIds}
            dashboardCustomFields={jiraOrcustomFileldsFilters}
            filterWidth={"half"}
          />
        )}
      </div>
      <div className={"dashboard-filters-component-container-footer"}>
        <a className={"edit-filters-link"} onClick={handleRedirection}>
          Edit Dashboard Filters
        </a>
      </div>
    </div>
  );
};

export default DashboardFilterComponent;
