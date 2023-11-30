import { forEach, get } from "lodash";
import CompactReport from "model/report/CompactReport";
import React, { useContext, useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { resetWidgetLibraryState } from "reduxConfigs/actions/widgetLibraryActions";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { libraryReportListSelector } from "reduxConfigs/selectors/widgetLibrarySelectors";
import { AntButton, AntCard, AntCol, AntRow, AntTooltip, SvgIcon } from "../../../../../shared-resources/components";
import { CategoryTheme, DISABLE_WIDGET_MESSAGE } from "../../report.constant";
import Widget from "model/widget/Widget";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { updateWidgetFiltersForReport } from "utils/widgetUtils";
import { RestWidget } from "classes/RestDashboards";
import { dashboardWidgetAdd, widgetDelete } from "reduxConfigs/actions/restapi";
import { WebRoutes } from "routes/WebRoutes";
import queryString from "query-string";
import { DashboardWidgetResolverContext } from "dashboard/pages/context";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { LTFC_MTTR_REPORTS } from "dashboard/constants/applications/names";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { ProjectPathProps } from "classes/routeInterface";

interface WidgetThemeProps {
  theme: CategoryTheme;
}

const WidgetTheme: React.FC<WidgetThemeProps> = ({ theme }) => {
  const history = useHistory();
  const location = useLocation();
  const dashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });
  const projectParams = useParams<ProjectPathProps>();
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const LTFCAndMTTRSupport = useHasEntitlements(Entitlement.LTFC_MTTR_DORA_IMPROVEMENTS, EntitlementCheckType.AND);
  const dispatch = useDispatch();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);
  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: integrationIds
  });
  const isDisabled = !workspaceProfile;

  let reportList = useSelector(libraryReportListSelector);

  useEffect(() => {
    return () => {
      dispatch(resetWidgetLibraryState());
    };
  }, []);

  const handleClickAddAllWidgets = () => {
    reportList = reportList.filter((_report: any) => _report?.categories.includes("dora"));
    if (!LTFCAndMTTRSupport) {
      reportList = reportList.filter((report: any) => !LTFC_MTTR_REPORTS.includes(report.key));
    }
    const draftWidgets: RestWidget[] = widgets.filter((widget: RestWidget) => widget.draft === true);
    if (draftWidgets.length) {
      forEach(draftWidgets, draftWidget => {
        dispatch(widgetDelete(draftWidget.id));
      });
    }
    forEach(reportList, (report: CompactReport) => {
      if (dashboard) {
        if (report) {
          const widget = Widget.newInstance(dashboard, report, widgets);
          if (!widget) {
            console.error("Error: Failed to create widget");
            return;
          }
          widget.name = report?.name?.toUpperCase() || "";
          widget.description = report.description ?? "";
          let reportType = report.report_type;
          const defaultMetaData = get(widgetConstants, [reportType, DEFAULT_METADATA], {});
          const _defaultMetadata =
            typeof defaultMetaData === "function" ? defaultMetaData({ dashboard }) : defaultMetaData;
          widget.metadata = {
            ...(widget.metadata || {}),
            ..._defaultMetadata
          };

          const getdoraProfileType = get(
            widgetConstants,
            [widget.type as string, "getDoraProfileIntegrationType"],
            undefined
          );
          let doraProfileIntegrationType;
          if (getdoraProfileType) {
            doraProfileIntegrationType = getdoraProfileType({
              integrations,
              workspaceOuProfilestate: workspaceProfile
            });
            widget.metadata = {
              ...(widget.metadata || {}),
              integration_type: doraProfileIntegrationType
            };
          }
          const updatedWidget = updateWidgetFiltersForReport(
            widget as RestWidget,
            reportType,
            dashboard?.global_filters,
            dashboard,
            doraProfileIntegrationType
          );
          dispatch(dashboardWidgetAdd(dashboard.id, updatedWidget.json));
          history.push({
            pathname: WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboardId),
            search: location?.search || undefined
          });
        }
      }
    });
  };
  return (
    <AntCard
      className="widget-theme"
      onClick={theme?.key !== "dora" ? () => history.push(`${theme.link}${location.search}`) : () => {}}>
      <AntRow className="">
        <AntCol className="d-flex align-center h-85" span={6}>
          <SvgIcon className="icon" icon={theme.icon} />
        </AntCol>
        <AntCol className="pl-25" span={18}>
          <div className="title">{theme.label}</div>
          <div className="description">{theme.description}</div>
        </AntCol>
      </AntRow>
      {theme?.key === "dora" && (
        <AntRow className="d-flex justify-space-between">
          <AntButton onClick={() => history.push(`${theme.link}${location.search}`)}>
            Select Individual Widgets
          </AntButton>
          <AntTooltip placement="bottom" title={isDisabled ? DISABLE_WIDGET_MESSAGE : null}>
            <AntButton
              className={!isDisabled ? "add-all-widgets-btn" : "disabled-all-widgets-btn"}
              disabled={isDisabled}
              onClick={handleClickAddAllWidgets}>
              Add all Widgets
            </AntButton>
          </AntTooltip>
        </AntRow>
      )}
    </AntCard>
  );
};

export default React.memo(WidgetTheme);
