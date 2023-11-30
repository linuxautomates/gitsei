import React, { useContext, useEffect, useState, useMemo } from "react";
import { useSelector, useDispatch } from "react-redux";
import { RouteComponentProps, useParams } from "react-router-dom";
import { capitalize, forEach, get, sortBy } from "lodash";
import "./WidgetsByTheme.scss";
import { useHeader } from "../../../../custom-hooks/useHeader";
import { generateExploreWidgetByThemeCustomCategoryPageBreadcrumbs } from "../../../../utils/dashboardUtils";
import { DashboardWidgetResolverContext } from "../../context";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import WidgetInfoDrawer from "../widget-info-drawer/WidgetInfoDrawer";
import { allReportsDocsListSelector, reportDocsListSelector } from "reduxConfigs/selectors/widgetReportDocsSelector";
import { getAllCompactReports } from "../reportHelper";
import { setWidgetLibraryList } from "reduxConfigs/actions/widgetLibraryActions";
import CompactReport from "../../../../model/report/CompactReport";
import {
  libraryReportListSelector,
  showSupportedOnlyReportsSelector
} from "reduxConfigs/selectors/widgetLibrarySelectors";
import WidgetsPreviewDora from "./WidgetsPreviewDora";
import { WebRoutes } from "routes/WebRoutes";
import Widget from "model/widget/Widget";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { updateWidgetFiltersForReport } from "utils/widgetUtils";
import { RestWidget } from "classes/RestDashboards";
import { dashboardWidgetAdd, widgetDelete } from "reduxConfigs/actions/restapi";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import queryString from "query-string";
import { ProjectPathProps } from "classes/routeInterface";

interface WidgetsByThemeCustomCategoryProps extends RouteComponentProps {}

const WidgetsByThemeCustomCategory: React.FC<WidgetsByThemeCustomCategoryProps> = ({ match, history, location }) => {
  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();

  const [showWidgetInfoDrawer, setWidgetInfoVisibility] = useState(false);
  const [category, setCategory] = useState<string>("");
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [selectedReportTheme, setSelectedReportTheme] = useState<undefined | CompactReport>();
  const [isChecked, setIsChecked] = useState<any[]>([]);

  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const dashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });
  const reportDocsState = useSelector(reportDocsListSelector);
  const allReportsDocsState = useSelector(allReportsDocsListSelector);
  let reportList = useSelector(libraryReportListSelector);
  const showSupportedOnly = useSelector(showSupportedOnlyReportsSelector);
  if (showSupportedOnly) {
    reportList = reportList.filter((report: CompactReport) => report.supported_by_integration);
  }
  const CANCEL_ACTION_KEY = "action_cancel";
  const ADD_WIDGET_KEY = "add_widget";

  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: integrationIds
  });

  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  useEffect(() => {
    return () => {
      if (history.action === "POP") {
        const draftWidgets: RestWidget[] = widgets.filter((widget: RestWidget) => widget.draft === true);
        if (draftWidgets.length) {
          forEach(draftWidgets, draftWidget => {
            dispatch(widgetDelete(draftWidget.id));
          });
        }
      }
    };
  }, []);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          history.replace({
            pathname: WebRoutes.dashboard.widgets.widgetsExplorer(projectParams, dashboardId),
            search: location?.search || undefined
          });
          return {
            hasClicked: false
          };
        case ADD_WIDGET_KEY:
          handleSave();
          history.push({
            pathname: WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboardId),
            search: location?.search || undefined
          });
          return {
            hasClicked: false
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  useEffect(() => {
    const _category = (match.params as any).typeId;
    const title = _category
      .split("_")
      .map((str: string) => capitalize(str))
      .join(" ");
    const pageSettings = {
      title,
      action_buttons: {
        [CANCEL_ACTION_KEY]: {
          type: "secondary",
          label: "Cancel",
          hasClicked: false,
          disabled: false,
          showProgress: false
        },
        [ADD_WIDGET_KEY]: {
          type: "primary",
          label: "Add Widget",
          hasClicked: false,
          disabled: isChecked.length === 0,
          showProgress: false
        }
      },
      bread_crumbs: generateExploreWidgetByThemeCustomCategoryPageBreadcrumbs(
        match.params as ProjectPathProps,
        dashboardId,
        dashboard?.name,
        "Explore Widgets",
        _category,
        location?.search
      ),
      showDivider: true,
      newWidgetExplorerHeader: true
    };
    setupHeader(pageSettings);
    setCategory(_category);
    const filter = { categories: [title] };
    // dispatch(listReportDocs({ filter }));
  }, [dashboard, isChecked]);

  const handleSave = () => {
    if (dashboard) {
      const draftWidgets: RestWidget[] = widgets.filter((widget: RestWidget) => widget.draft === true);
      if (draftWidgets.length) {
        forEach(draftWidgets, draftWidget => {
          dispatch(widgetDelete(draftWidget.id));
        });
      }
      forEach(isChecked, (report: CompactReport) => {
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
          dispatch(dashboardWidgetAdd(dashboard.id, updatedWidget.json));
        }
      });
    }
  };

  useEffect(() => {
    // if (loadingCategories && !allReportsDocsState?.loading) {
    //   const loading = get(reportDocsState, "loading", true);
    //   const error = get(reportDocsState, "error", true);
    //   if (!loading && !error) {
    const _category = (match.params as any).typeId;
    const data = get(reportDocsState, ["data", "records"]);
    const allReports = getAllCompactReports();
    // only showing the reports from BE
    let finalReports = allReports
      .filter((_report: any) => _report?.categories.includes(_category))
      .map((item: any) => {
        return {
          ...item,
          name: item.name,
          key: item.key,
          applications: item.applications,
          report_type: item.report_type,
          categories: item?.categories.map((category: any) => category.toLowerCase().replace(/ /g, "_")),
          supported_widget_types: item.supported_widget_types
        };
      })
      .filter((item: any) => item !== undefined);
    finalReports = sortBy(finalReports, ["name"]);
    dispatch(setWidgetLibraryList(finalReports));
    // }
    // }
  }, [reportDocsState, allReportsDocsState]);

  const handleWidgetInfoDataChange = (theme: CompactReport) => {
    setSelectedReportTheme(theme);
    setWidgetInfoVisibility(true);
  };

  const handleOnDrawerClose = () => {
    setSelectedReportTheme(undefined);
    setWidgetInfoVisibility(false);
  };

  const handleChange = (props: any) => {
    if (props.checked && !isChecked.includes(props.value)) {
      setIsChecked(prevState => [...prevState, props.value]);
    }
    if (!props.checked) {
      setIsChecked(prevState => prevState.filter(v => v?.key !== props?.value?.key));
    }
  };
  return (
    <>
      <div className="widgets-by-theme h-100 pb-0">
        <div className="content">
          {/* TODO: will comment this once we enable the reports/list call from BE */}
          {/* {loadingCategories && <Loader />} */}
          {/* {!loadingCategories && ( */}
          <WidgetsPreviewDora
            selectedCategory={category}
            setWidgetInfoData={handleWidgetInfoDataChange}
            isChecked={isChecked}
            setIsChecked={handleChange}
          />
          {/* )} */}
        </div>
      </div>
      <WidgetInfoDrawer
        visible={showWidgetInfoDrawer}
        onClose={handleOnDrawerClose}
        reportTheme={selectedReportTheme}
      />
    </>
  );
};

export default WidgetsByThemeCustomCategory;
