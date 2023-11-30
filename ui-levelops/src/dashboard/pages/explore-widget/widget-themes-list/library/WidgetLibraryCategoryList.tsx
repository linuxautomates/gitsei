import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { forEach, get, upperCase } from "lodash";
import { v1 as uuid } from "uuid";
import "./WidgetLibraryCategoryList.style.scss";
import {
  libraryReportListByCategorySelector,
  showSupportedOnlyReportsSelector
} from "reduxConfigs/selectors/widgetLibrarySelectors";
import { CompactCategoryReports } from "../../reportHelper";
import CompactReport from "../../../../../model/report/CompactReport";
import Loader from "components/Loader/Loader";
import { loadReports, resetWidgetLibraryFilters } from "reduxConfigs/actions/widgetLibraryActions";
import { _selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import ReportRow from "./ReportRow";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import Widget from "model/widget/Widget";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  BITBUCKET_APPLICATIONS,
  jiraManagementTicketReport,
  leadTimeIssueReports,
  LEAD_TIME_ISSUE_REPORT,
  SCM_REPORTS,
  supportReports,
  LTFC_MTTR_REPORTS,
  DORA_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  doraReports,
  LEAD_TIME_MTTR_REPORTS
} from "dashboard/constants/applications/names";
import { getIssueManagementReportType, supportSystemToReportTypeMap } from "dashboard/graph-filters/components/helper";
import widgetConstants from "dashboard/constants/widgetConstants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { updateWidgetFiltersForReport } from "utils/widgetUtils";
import { RestWidget } from "classes/RestDashboards";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import { dashboardWidgetAdd, multiReportWidgetReportAdd, widgetDelete } from "reduxConfigs/actions/restapi";
import { WebRoutes } from "routes/WebRoutes";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import queryString from "query-string";
import { ProjectPathProps } from "classes/routeInterface";
import { Tabs } from "antd";
import { TAB_KEY } from "constants/fieldTypes";
import { ADVANCE_TAB_WIDGET_ARRAY } from "../../report.constant";

const { TabPane } = Tabs;

interface WidgetLibraryCategoryListProps {}

const WidgetLibraryCategoryList: React.FC<WidgetLibraryCategoryListProps> = () => {
  const widget: any = { name: "", description: "" };
  const dashboardIntegrations = useSelector(_selectedDashboardIntegrations);
  const loading = !dashboardIntegrations.loaded;
  const dashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });
  const projectParams = useParams<ProjectPathProps>();
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const reportListByCategories = useSelector(libraryReportListByCategorySelector);
  const showSupportedOnly = useSelector(showSupportedOnlyReportsSelector);

  const LTFCAndMTTRSupport = useHasEntitlements(Entitlement.LTFC_MTTR_DORA_IMPROVEMENTS, EntitlementCheckType.AND);
  // ENTITLEMENT FOR JIRA RELESE PROFILE
  const velocityJiraReleaseProfile = useHasEntitlements(
    Entitlement.VELOCITY_JIRA_RELEASE_PROFILE,
    EntitlementCheckType.AND
  );

  const [activekey, setActiveKey] = useState<any>("most_used");

  useEffect(() => {
    const active_key = queryString.parse(location?.search)?.[TAB_KEY];
    if (active_key !== activekey) {
      setActiveKey(active_key || "most_used");
    }
  }, [location]);

  const onTabChangeHandler = (key: any) => {
    const active_key = queryString.parse(location?.search)?.[TAB_KEY];
    const updatedUrlSearch = location.search.replace(`&tab=${active_key}`, "");
    history.push(`${location.pathname}${updatedUrlSearch}&${TAB_KEY}=${key}`);
  };

  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: integrationIds
  });

  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  useEffect(() => {
    dispatch(loadReports());
    return () => {
      dispatch(resetWidgetLibraryFilters());
    };
  }, []);

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

  const handleSave = useCallback(
    (report: CompactReport) => {
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

        widget.name = report?.name?.toUpperCase() || "";
        widget.description = widget.description ?? "";
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

        const getdoraProfileType = get(
          widgetConstants,
          [widget.type as string, "getDoraProfileIntegrationType"],
          undefined
        );
        let doraProfileIntegrationType;
        if (getdoraProfileType) {
          doraProfileIntegrationType = getdoraProfileType({
            integrations,
            workspaceOuProfilestate
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
          search: location?.search
        });
      } else {
        console.error("Error: No dashboard selected");
      }
    },
    [widget, dashboardIntegrations]
  );

  if (loading) {
    return <Loader />;
  }

  const tabArray = [
    { tabKey: "most_used", tabName: "Frequently Used" },
    { tabKey: "advanced", tabName: "Advanced" }
  ];

  // ADVANCE
  // 1) IN THIS WE HAVE WIDGTE THAT ADDED IN ADVANCE_TAB_WIDGET_ARRAY
  // 2) REQUIRED INTEGRATION IS NOT CREATED LIKE FOR PAGERDUTY IF INTEGRATION IS NOT THERE THEN ALL OF THAT WIDGET WILL GO IN ADVANCE
  // 3) FOR DORA IF PROFILE IS NOT CREATE THEN IT WILL GO TO ADVANCE

  // MOST USED - APART FROM ADVANCE ALL WILL GO TO MOST USED
  const renderReportListData = useMemo(() => {
    return tabArray.map(tab => {
      return (
        <TabPane key={tab.tabKey} tab={tab.tabName}>
          {reportListByCategories.map((category: CompactCategoryReports, index: number) => {
            const key = Object.keys(category)[0] || "";
            let list =
              category[key]?.sort((a: any, b: any) => b?.supported_by_integration - a?.supported_by_integration) || [];

            if (tab.tabKey === "advanced") {
              if (ADVANCE_TAB_WIDGET_ARRAY.length > 0) {
                list = list.filter(
                  (report: CompactReport) =>
                    ADVANCE_TAB_WIDGET_ARRAY.includes(report.key as any) ||
                    !report.supported_by_integration ||
                    (!workspaceProfile && [...doraReports, ...LEAD_TIME_MTTR_REPORTS].includes(report.key as any))
                );
              }
            } else {
              list = list.filter(
                (report: CompactReport) =>
                  report.supported_by_integration && !ADVANCE_TAB_WIDGET_ARRAY.includes(report.key as any)
              );

              if (!LTFCAndMTTRSupport) {
                list = list.filter((report: CompactReport) => !LTFC_MTTR_REPORTS.includes(report.key as DORA_REPORTS));
              }
              if (!velocityJiraReleaseProfile) {
                list = list.filter(
                  (report: CompactReport) => JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT !== report.key
                );
              }
              if (!workspaceProfile) {
                list = list.filter(
                  (report: CompactReport) => ![...doraReports, ...LEAD_TIME_MTTR_REPORTS].includes(report.key as any)
                );
              }
            }

            if (!list.length) {
              return null;
            }
            return (
              <div key={`${key}-${index}`} style={{ marginBottom: "11px" }}>
                <div className="lib-category-list__heading">{upperCase(key)}</div>
                <div className="lib-category-list__list">
                  {list.map((report: CompactReport) => (
                    <ReportRow key={uuid()} report={report} onClick={handleSave} />
                  ))}
                </div>
              </div>
            );
          })}
        </TabPane>
      );
    });
  }, [
    tabArray,
    reportListByCategories,
    ADVANCE_TAB_WIDGET_ARRAY,
    showSupportedOnly,
    LTFCAndMTTRSupport,
    velocityJiraReleaseProfile,
    workspaceProfile
  ]);

  return (
    <>
      <div className="lib-category-list">
        {reportListByCategories.length === 0 && <div className="lib-category-list__no-data">No widgets found.</div>}
        <Tabs size={"small"} activeKey={activekey} onChange={onTabChangeHandler}>
          {renderReportListData}
        </Tabs>
      </div>
    </>
  );
};

export default React.memo(WidgetLibraryCategoryList);
