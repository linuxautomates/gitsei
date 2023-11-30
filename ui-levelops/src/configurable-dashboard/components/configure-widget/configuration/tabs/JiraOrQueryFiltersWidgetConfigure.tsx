import { Collapse } from "antd";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { getJiraOrFiltersHelper, filterCount } from "dashboard/components/dashboard-header/helper";
import { get } from "lodash";
import React, { useMemo, useState, useCallback } from "react";
import { useDispatch } from "react-redux";
import { _widgetUpdate } from "reduxConfigs/actions/restapi";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getChildWidget } from "reduxConfigs/selectors/widgetSelector";
import { AntBadge, AntSwitch, AntText } from "shared-resources/components";
import CustomFieldsApiComponent from "./CustomFieldsApiComponent";
import JiraOrFiltersContainer from "./JiraOrFiltersContainer";
import cx from "classnames";

const { Panel } = Collapse;

const tooltipInfo =
  "Top Level filters applied on Jira data before the widgets filters are applied. Tickets matching ANY of the insights filters are selected.";
interface JiraOrQueryFiltersWidgetConfigureProps {
  dashboardId: string;
  widgetId: string;
}

const JiraOrQueryFiltersWidgetConfigure: React.FC<JiraOrQueryFiltersWidgetConfigureProps> = ({
  dashboardId,
  widgetId
}) => {
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });

  const widget: RestWidget = useParamSelector(getChildWidget, { widget_id: widgetId });

  const integrationIds = get(dashboard, ["_query", "integration_ids"], []).sort();

  const [filterClicked, setFilterClicked] = useState<boolean>(false);

  const dispatch = useDispatch();

  const disableGlobalFilters = useMemo(() => {
    if (!widget) {
      return false;
    }
    return get(widget?.metadata, "disable_or_filters", false);
  }, [widget]);

  const jiraOrFilters = useMemo(() => get(dashboard, ["_metadata", "jira_or_query"], {}), [dashboard]);

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
    return {
      normalcustomfields: { ...customFields },
      exclude: { ...excludeCustomFields },
      partial: { ...partialCustomFields }
    };
  }, [dashboard]);

  const onFilterClickhandler = (value: boolean) => {
    setFilterClicked(value);
  };

  const handleGlobalFilterToggle = useCallback(
    (checked: boolean, e: any) => {
      e.stopPropagation();
      if (!widget) {
        return;
      }
      const widgetData: any = widget?.json || {};
      const updatedWidgetData = {
        ...widgetData,
        metadata: {
          ...(widgetData?.metadata || {}),
          disable_or_filters: !checked
        }
      };
      dispatch(_widgetUpdate(dashboardId, widgetId, updatedWidgetData));
    },
    [widget]
  );

  const getJiraOrFilters = useMemo(() => {
    return getJiraOrFiltersHelper(jiraOrFilters, "jira_filter_values");
  }, [dashboard]);

  const filtersCount = useMemo(() => filterCount(get(dashboard, ["_metadata", "jira_or_query"], {})), [dashboard]);

  return (
    <div>
      {(getJiraOrFilters.length > 0 || Object.keys(jiraOrcustomFileldsFilters).length > 0) && (
        <Collapse>
          <Panel
            header={
              <AntText className={"dashboard-text"} onClick={() => onFilterClickhandler(true)}>
                Insight Filters {<AntBadge className="filters-count" count={filtersCount || 0} />}
              </AntText>
            }
            key="global-filters"
            className={cx("dashboard-filter-wrapper", { "disabled-filters": disableGlobalFilters })}
            extra={
              <div className={"flex align-center direction-column"}>
                <AntSwitch checked={!disableGlobalFilters} onChange={handleGlobalFilterToggle} />
                <AntText className="toggle-global-filters-label">Active</AntText>
              </div>
            }>
            <div className="help-container">
              <AntText>
                {tooltipInfo.split("ANY")[0]}
                <AntText strong>ANY</AntText>
                {tooltipInfo.split("ANY")[1]}
              </AntText>
            </div>
            <div>
              <JiraOrFiltersContainer jiraOrFilters={getJiraOrFilters} />
              {Object.keys(jiraOrcustomFileldsFilters).length > 0 && (
                <CustomFieldsApiComponent
                  integrationIds={integrationIds}
                  dashboardCustomFields={jiraOrcustomFileldsFilters}
                />
              )}
            </div>
          </Panel>
        </Collapse>
      )}
    </div>
  );
};

export default JiraOrQueryFiltersWidgetConfigure;
