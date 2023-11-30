import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Collapse, Icon } from "antd";
import { get, unset } from "lodash";
import { useHistory } from "react-router-dom";
import { RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  dashboardWidgetChildrenSelector,
  getDashboard,
  isDashboardHasAccessSelector,
  selectedDashboard
} from "reduxConfigs/selectors/dashboardSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { WebRoutes } from "routes/WebRoutes";
import { AntBadge, AntText } from "shared-resources/components";
import { getReportNameByKey } from "utils/reportListUtils";
import { sanitizeObject, sanitizeObjectCompletely } from "utils/commonUtils";
import WidgetFilterPreview from "./WidgetFilterPreview";
import widgetConstants from "../../constants/widgetConstants";
import "./WidgetFilterPreview.scss";
import { getFiltersCount } from "./helper";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import JiraOrFiltersContainer from "configurable-dashboard/components/configure-widget/configuration/tabs/JiraOrFiltersContainer";
import CustomFieldsApiComponent from "configurable-dashboard/components/configure-widget/configuration/tabs/CustomFieldsApiComponent";
import { getJiraOrFiltersHelper } from "../dashboard-header/helper";
import {
  doraReports,
  LEAD_TIME_MTTR_REPORTS,
  NO_LONGER_SUPPORTED_FILTER
} from "dashboard/constants/applications/names";
import { updateTimeFiltersValue } from "shared-resources/containers/widget-api-wrapper/helper";
import { useLocation, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { TICKET_CATEGORIZATION_SCHEMES_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { ProjectPathProps } from "classes/routeInterface";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";

const { Panel } = Collapse;

interface WidgetFilterPreviewWrapperProps {
  widgetId: string;
  dashboardId: string;
}

const WidgetFilterPreviewWrapper: React.FC<WidgetFilterPreviewWrapperProps> = (
  props: WidgetFilterPreviewWrapperProps
) => {
  const { widgetId, dashboardId } = props;
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();

  const [selectedWidgetId, setSelectedWidgetId] = useState<string | undefined>(undefined);
  const dashboard = useSelector(selectedDashboard);
  const integrationIds = useMemo(() => dashboard?.query?.integration_ids || [], [dashboard]);
  const selectedOUState = useSelector(getSelectedOU);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });

  const { edit = true } = useParamSelector(isDashboardHasAccessSelector, dashboard);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const childWidgetIds = widget?.children || [];

  const childWidgets: RestWidget[] = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: widgetId
  });
  const location = useLocation();
  useEffect(() => {
    if (widget.isComposite && Array.isArray(childWidgetIds) && childWidgetIds.length > 0) {
      setSelectedWidgetId(childWidgetIds[0]);
    }
  }, []);

  const reportType = useMemo(() => widget?.type, [widget]);
  const uri = useMemo(() => {
    let _uri = get(widgetConstants, [reportType, "supported_filters", "uri"], "default_uri");
    const conditionalUriMethod = get(widgetConstants, [reportType, "conditionalUriMethod"], undefined);
    if (conditionalUriMethod) {
      if (doraReports.includes(reportType) || LEAD_TIME_MTTR_REPORTS.includes(reportType)) {
        _uri = conditionalUriMethod({
          integrationState: integrations,
          workspaceProfile: workspaceProfile,
          reportType: reportType
        });
      } else {
        _uri = conditionalUriMethod({ query: widget?.query, defaultUri: _uri });
      }
    }
    return _uri;
  }, [widget]);

  const metaData = useMemo(() => widget?.metadata, [widget]);
  const filters = useMemo(() => {
    let _filters = sanitizeObject(widget?.query || {});
    const isDashboardEffortProfileEnabled = get(dashboard?._metadata, ["effort_investment_profile"], false);
    const dashboardAlignmentProfile = get(dashboard?._metadata, ["effort_investment_profile_filter"], false);

    const removeNoLongerSupportedFilter = get(widgetConstants, [widget.type, NO_LONGER_SUPPORTED_FILTER], undefined);
    if (removeNoLongerSupportedFilter) {
      _filters = removeNoLongerSupportedFilter(_filters);
    }
    _filters = updateIssueCreatedAndUpdatedFilters({ filter: _filters }, metaData, reportType)?.filter;

    const removeHiddenFilters = get(widgetConstants, [reportType, "removeHiddenFiltersFromPreview"], undefined);

    if (removeHiddenFilters) {
      _filters = removeHiddenFilters({ filter: _filters });
    }

    _filters = sanitizeObjectCompletely({
      ...(_filters || {}),
      [TICKET_CATEGORIZATION_SCHEMES_KEY]:
        isDashboardEffortProfileEnabled && widget?.query?.[TICKET_CATEGORIZATION_SCHEMES_KEY]
          ? dashboardAlignmentProfile
          : _filters?.[TICKET_CATEGORIZATION_SCHEMES_KEY] || "" // setting dashboard level profile if it s enabled
    });
    return {
      [uri || "default_uri"]: _filters
    };
  }, [widget, dashboard?._metadata?.effort_investment_profile_filter]);

  const activeKey = useMemo(() => (selectedWidgetId ? [selectedWidgetId] : []), [selectedWidgetId]);
  const collapseStyle = useMemo(
    () => ({
      maxHeight: "calc(100% - 40px)",
      overflow: "auto"
    }),
    []
  );

  const handleEditClick = useCallback(() => {
    history.push({
      pathname: WebRoutes.dashboard.widgets.details(projectParams, dashboardId, widgetId),
      search: location?.search || undefined
    });
  }, [dashboardId, widgetId]);

  const handleSelectedReportChange = useCallback((key: string | string[]) => {
    if (!key) {
      setSelectedWidgetId(undefined);
      return;
    }
    const _key = typeof key === "string" ? key : key[0];
    setSelectedWidgetId(_key);
  }, []);

  const widgetFilterCount = useMemo(() => {
    let count = 0;
    if (widget.isComposite) {
      childWidgets.forEach((childWidget: RestWidget) => {
        const childUri = get(widgetConstants, [childWidget.type, "supported_filters", "uri"], undefined);
        const newQuery = {
          ...childWidget?.query,
          ...updateTimeFiltersValue(dashboard?._metadata, childWidget.metadata, { ...childWidget?.query })
        };
        count = count + getFiltersCount(sanitizeObject(newQuery || {}), childWidget.type, childUri) || 0;
      }, 0);
      return count;
    } else {
      let newQuery = sanitizeObject(widget?.query || {});
      const removeNoLongerSupportedFilter = get(widgetConstants, [widget.type, NO_LONGER_SUPPORTED_FILTER], undefined);
      if (removeNoLongerSupportedFilter) {
        newQuery = removeNoLongerSupportedFilter(newQuery);
      }
      const removeHiddenFilters = get(widgetConstants, [reportType, "removeHiddenFiltersFromPreview"], undefined);

      if (removeHiddenFilters) {
        newQuery = removeHiddenFilters({ filter: newQuery });
      }
      newQuery = {
        ...newQuery,
        ...updateTimeFiltersValue(dashboard?._metadata, metaData, { ...newQuery })
      };
      return getFiltersCount(newQuery, widget.type, uri) || 0;
    }
  }, [widget, metaData, dashboard?._metadata, childWidgets]);

  const jiraOrFilters = useMemo(() => get(dashboard, ["_metadata", "jira_or_query"], {}), [dashboard]);

  const getJiraOrFilters = useMemo(() => {
    return getJiraOrFiltersHelper(jiraOrFilters, "jira_filter_values");
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
    return {
      normalcustomfields: { ...customFields },
      exclude: { ...excludeCustomFields },
      partial: { ...partialCustomFields }
    };
  }, [dashboard]);

  const oldAccess = getRBACPermission(PermeableMetrics.ADMIN_WIDGET_EXTRAS);
  const permissions = useDashboardPermissions();
  const hasEditAccess = window.isStandaloneApp ? oldAccess : permissions[1];

  const disableGlobalFilters = useMemo(() => {
    if (!widget) {
      return false;
    }
    return get(widget?.metadata, "disable_or_filters", false);
  }, [widget]);

  const renderWidgetHeader = useMemo(() => {
    if (widget?.isStat) {
      return null;
    }

    return (
      <div className="widget-filter-container">
        <div className="widget-filter-header">
          <div className="filters-count-container">
            <AntText className="filters-label">Widget Filters ({widgetFilterCount ? widgetFilterCount : 0})</AntText>
          </div>
        </div>
      </div>
    );
  }, [widget, childWidgets]);

  const renderEditFooter = useMemo(() => {
    return (
      <div className="edit-Container">
        <AntText onClick={handleEditClick} className="edit-title">
          Edit Filters
        </AntText>
      </div>
    );
  }, [handleEditClick]);

  const renderDashboardHeader = useMemo(() => {
    if (widget?.isStat) {
      return null;
    }
    const allowEdit = get(widget?.metadata, "disable_or_filters", false);
    return (
      <div className="widget-filter-header">
        <div className="filters-count-container">
          <div className="filters-label-container">
            <AntText className="filters-label">Insights Filters ({getJiraOrFilters.length})</AntText>
            {disableGlobalFilters && <AntText className="filters-subLabel">Disabled</AntText>}
          </div>
          <AntBadge className="filters-count" count={0} overflowCount={1000} />
        </div>
        {edit && hasEditAccess && !allowEdit && <Icon type="edit" onClick={handleEditClick} />}
      </div>
    );
  }, [widget, childWidgets, disableGlobalFilters]);

  const getPanelHeader = useCallback((childWidget: RestWidget, index: number) => {
    const name = !childWidget?.name
      ? `Report ${index + 1} - ${getReportNameByKey(childWidget.type)}`
      : childWidget?.name;
    const childUri = get(widgetConstants, [childWidget.type, "supported_filters", "uri"], undefined);
    const newQuery = {
      ...childWidget?.query,
      ...updateTimeFiltersValue(dashboard?._metadata, childWidget.metadata, { ...childWidget?.query })
    };
    const childFiltersCount = getFiltersCount(sanitizeObject(newQuery || {}), childWidget.type, childUri) || 0;
    return (
      <div className="panel-header">
        <AntText className="panel-title">{name}</AntText>
        <AntBadge className="panel-filter-count" count={childFiltersCount || 0} overflowCount={1000} />
      </div>
    );
  }, []);

  const renderWidgetFilters = useMemo(() => {
    if (widget.isComposite && childWidgets.length) {
      const panels = childWidgets.map((childWidget: RestWidget, index: number) => {
        const childReport = childWidget?.type;
        const childUri = get(widgetConstants, [childReport, "supported_filters", "uri"], undefined);

        const childFilters = {
          [childUri]: sanitizeObject(childWidget?.query || {})
        };
        return (
          <Panel header={getPanelHeader(childWidget, index)} key={childWidget?.id}>
            <WidgetFilterPreview
              key={childWidget?.id}
              id={childWidget.id}
              filters={childFilters}
              metaData={childWidget?.metadata}
              uri={childUri}
              reportType={childReport}
              integrationIds={integrationIds}
              dashboardMetaData={dashboard?._metadata}
            />
          </Panel>
        );
      });

      return (
        <Collapse
          style={collapseStyle}
          accordion
          activeKey={activeKey}
          defaultActiveKey={childWidgets?.[0]?.id}
          onChange={handleSelectedReportChange}>
          {panels}
        </Collapse>
      );
    }
    return (
      <WidgetFilterPreview
        id={widget.id}
        filters={filters}
        metaData={metaData}
        uri={uri}
        reportType={reportType}
        integrationIds={integrationIds}
        dashboardMetaData={dashboard?._metadata}
      />
    );
  }, [widget, integrationIds, selectedWidgetId, childWidgets, dashboard?._metadata]);

  const renderDashboardFilters = useMemo(() => {
    return (
      <div className="db-filter-container">
        <JiraOrFiltersContainer jiraOrFilters={getJiraOrFilters} />
        {Object.keys(jiraOrcustomFileldsFilters).length > 0 && (
          <CustomFieldsApiComponent
            integrationIds={integrationIds}
            dashboardCustomFields={jiraOrcustomFileldsFilters}
          />
        )}
      </div>
    );
  }, [widget, integrationIds, selectedWidgetId, childWidgets]);

  return (
    <div className="widget-filters-wrapper">
      <div className={disableGlobalFilters ? "disabled-db-filters" : ""}>
        {renderDashboardHeader}
        {renderDashboardFilters}
      </div>
      {renderWidgetHeader}
      {renderWidgetFilters}
      {edit && hasEditAccess && renderEditFooter}
    </div>
  );
};

export default WidgetFilterPreviewWrapper;
