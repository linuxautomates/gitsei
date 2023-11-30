import React, { useMemo, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import "./EditWidgetModal.scss";
import WidgetDetailsModal from "./WidgetDetailsModal";
import { RestWidget } from "classes/RestDashboards";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import CompactReport from "model/report/CompactReport";
import Widget from "model/widget/Widget";
import { dashboardWidgetAdd, multiReportWidgetReportAdd, widgetDelete } from "reduxConfigs/actions/restapi";
import { WebRoutes } from "routes/WebRoutes";
import { useHistory, useParams, useLocation } from "react-router-dom";
import { updateWidgetFiltersForReport } from "utils/widgetUtils";
import widgetConstants from "dashboard/constants/widgetConstants";
import { forEach, get } from "lodash";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { _selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import {
  LEAD_TIME_ISSUE_REPORT,
  leadTimeIssueReports,
  jiraManagementTicketReport,
  BITBUCKET_APPLICATIONS,
  supportReports,
  SCM_REPORTS
} from "dashboard/constants/applications/names";
import { getIssueManagementReportType, supportSystemToReportTypeMap } from "dashboard/graph-filters/components/helper";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { ProjectPathProps } from "classes/routeInterface";
interface EditWidgetModalProps {
  report?: CompactReport;
  onClose: () => void;
}

const CreateWidgetModal: React.FC<EditWidgetModalProps> = ({ onClose, report }) => {
  const widget: any = { name: "", description: "" };

  const dispatch = useDispatch();
  const dashboard: any = useSelector(selectedDashboard);
  const dashboardIntegrations = useSelector(_selectedDashboardIntegrations);
  const location = useLocation();
  const supportedApplications = useMemo(() => {
    const { records } = dashboardIntegrations;
    if ((records || []).length) {
      return records
        .filter((integration: any) => !!integration.id)
        .map((item: any) => {
          if (["helix", "gitlab", ...BITBUCKET_APPLICATIONS].includes(item.application)) {
            return "github";
          }
          return item.application;
        });
    }
    return [];
  }, [dashboardIntegrations]);

  const projectParams = useParams<ProjectPathProps>();
  const history = useHistory();
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });

  const handleSave = useCallback(
    (name: string, description: string) => {
      const draftWidgets: RestWidget[] = widgets.filter((widget: RestWidget) => widget.draft === true);
      if (draftWidgets.length) {
        forEach(draftWidgets, draftWidget => {
          dispatch(widgetDelete(draftWidget.id));
        });
      }
      // used when creating a widget
      if (dashboard && report) {
        const widget = Widget.newInstance(dashboard, report, widgets);
        if (!widget) {
          console.error("Error: Failed to create widget");
          return;
        }

        widget.name = name;
        widget.description = description;
        let reportType = report.report_type;

        if (supportReports.includes(report.report_type as any)) {
          const supportSystem =
            supportedApplications?.includes("salesforce") && !supportedApplications?.includes("zendesk")
              ? "salesforce"
              : "zendesk";
          reportType = get(supportSystemToReportTypeMap, [report.report_type, supportSystem], report.report_type);
          widget.type = reportType;
        }

        if ([...leadTimeIssueReports, ...jiraManagementTicketReport].includes(report.report_type as any)) {
          let issueManagementSystem;
          if (report.report_type === LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT) {
            issueManagementSystem =
              supportedApplications?.includes("azure_devops") && !supportedApplications?.includes("githubjira")
                ? "azure_devops"
                : "githubjira";
          } else {
            issueManagementSystem =
              supportedApplications?.includes("azure_devops") && !supportedApplications?.includes("jira")
                ? "azure_devops"
                : "jira";
          }

          reportType = getIssueManagementReportType(report.report_type, issueManagementSystem);
          widget.type = reportType;
        }

        const defaultMetaData = get(widgetConstants, [reportType, DEFAULT_METADATA], {});
        const _defaultMetadata =
          typeof defaultMetaData === "function" ? defaultMetaData({ dashboard }) : defaultMetaData;
        widget.metadata = {
          ...(widget.metadata || {}),
          ..._defaultMetadata
        };

        if (widget.type === SCM_REPORTS.COMMITTERS_REPORT) {
          const defaultSelected: any[] = OUFiltersMapping.github.filter((item: any) => item.defaultValue);
          widget.metadata = {
            ...(widget.metadata || {}),
            ou_user_filter_designation: {
              github: defaultSelected?.[0]?.defaultValue || []
            }
          };
        }
        const updatedWidget = updateWidgetFiltersForReport(
          widget as RestWidget,
          reportType,
          dashboard?.global_filters,
          dashboard
        );

        const widgetConstantComposite = get(widgetConstants, [reportType, "composite"], false);

        if (widgetConstantComposite) {
          widget.resetWidgetType = "compositegraph";

          if (widget.type === LEVELOPS_MULTITIME_SERIES_REPORT) {
            widget.setMultiSeriesTime = "quarter";
          }

          // add parent widget in dashboard
          dispatch(dashboardWidgetAdd(dashboard.id, updatedWidget.json));
          // add child widget in dashboard
          // don't add child for levelops multitime series report
          if (widget.type !== LEVELOPS_MULTITIME_SERIES_REPORT) {
            dispatch(multiReportWidgetReportAdd(updatedWidget.id, reportType as string));
          }
        } else {
          dispatch(dashboardWidgetAdd(dashboard.id, updatedWidget.json));
        }
        history.push({
          pathname: WebRoutes.dashboard.widgets.create(projectParams, dashboard.id, updatedWidget.id),
          search: location?.search || undefined
        });
      } else {
        console.error("Error: No insight selected");
      }
      onClose();
    },
    [widget, dashboardIntegrations]
  );

  return (
    <WidgetDetailsModal
      widget={widget}
      title={"Create Widget"}
      onSave={handleSave}
      onCancel={onClose}
      hideGraphConfiguration
    />
  );
};

export default React.memo(CreateWidgetModal);
