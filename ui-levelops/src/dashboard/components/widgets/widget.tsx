import React, { ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { Dropdown, Icon, Menu, Tooltip, Select } from "antd";
import cx from "classnames";
import html2canvas from "html2canvas";
import {
  AntCard,
  Button,
  SvgIcon,
  AntPopover,
  AntParagraph,
  AntButton,
  AntBadge,
  AntText
} from "shared-resources/components";
import WidgetExtras from "./widget-extras";
import {
  WidgetsLoadingContext,
  WidgetSvgContext,
  WidgetLoadingContext,
  WidgetBGColorContext,
  WidgetIntervalContext
} from "../../pages/context";
import { getTextWidth } from "../../helpers/helper";
import {
  AZURE_LEAD_TIME_ISSUE_REPORT,
  ISSUE_MANAGEMENT_REPORTS,
  LEAD_TIME_REPORTS,
  DEV_PROD_TABLE_REPORTS,
  DEV_PRODUCTIVITY_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  RAW_STATS_REPORTS,
  DORA_REPORTS,
  LEAD_MTTR_DORA_REPORTS
} from "../../constants/applications/names";
import WidgetFilterPreviewWrapper from "../widget-filter-preview/WidgetFilterPreviewWrapper";
import "./widget.style.scss";
import { STAT_BG_WHITE } from "shared-resources/charts/stats-chart/ideal-range.helper";
import { WebRoutes } from "../../../routes/WebRoutes";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { useDispatch } from "react-redux";
import { csvDownloadDevProductivity, csvDownloadRawStats } from "reduxConfigs/actions/csvDownload.actions";
import { get } from "lodash";
import queryString from "query-string";
import WidgetCardTitle from "./WidgetCardTitle";
import { useLocation } from "react-router-dom";
import { getDashboard, isDashboardHasAccessSelector } from "reduxConfigs/selectors/dashboardSelector";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { toTitleCase, valueToTitle } from "utils/stringUtils";
import WidgetColumnSelecor from "./WidgetColumnSelector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import WidgetSelectComponent from "./widget-dropdown";
import { useWidgetTitle } from "./useWidgetTitle";
import { GetInfoIcon } from "dashboard/reports/dev-productivity/helper";
import {
  DEV_PRODUCTIVITY_INTERVAL_OPTIONS,
  OLD_INTERVAL
} from "dashboard/graph-filters/components/DevProductivityFilters/constants";
import { MISCELLENEOUS_REPORTS } from "dashboard/reports/miscellaneous/constant";
import { ProjectPathProps } from "classes/routeInterface";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";
export const WIDGET_HEIGHT = 5;
export const STAT_WIDGET_HEIGHT = 2;
export const ROW_HEIGHT = 80;
export const STATS_ROW_HEIGHT = 94;
export const ICON_FULL_SCREEN = "arrows-alt";
export const ICON_FULL_SCREEN_EXIT = "shrink";
export const DISABLED_FILTERS_TEXT = "Insights filters are disabled";

const { Option } = Select;

interface WidgetContainerProps {
  id: string;
  dashboardId: string;
  width: number;
  setWidth: (id: string, width: number) => void;
  title: string;
  description?: string;
  showActionButtons: boolean;
  setReload: (reload: boolean) => void;
  applicationType: string;
  icon_type?: string;
  globalFilters: any;
  reportType: string;
  widgetType: string;
  disableResize?: boolean;
  children?: any;
  drilldownSelected?: boolean;
  hidden: boolean;
  height?: string;
  disableGlobalFilters?: boolean;
  getCustomTitle?: (args: any) => ReactNode | string;
  interval?: string;
}

export const Widget: React.FC<WidgetContainerProps> = ({
  id,
  dashboardId,
  width,
  setWidth,
  title,
  description,
  children,
  showActionButtons,
  setReload,
  applicationType,
  reportType,
  widgetType,
  disableResize,
  icon_type,
  drilldownSelected,
  hidden,
  height,
  disableGlobalFilters,
  getCustomTitle,
  interval
}) => {
  const [loading, setLoading] = useState(true);
  const [showTooltip, setShowTooltip] = useState(false);
  const [widgetBGColor, setWidgetBGColor] = useState<Record<string, string>>({});
  const location = useLocation();
  const OU = queryString.parse(location.search).OU as string;
  const isStat = widgetType.includes("stats");
  const widgetHeight = isStat ? STAT_WIDGET_HEIGHT : WIDGET_HEIGHT;
  const rowHeight = isStat ? STATS_ROW_HEIGHT : ROW_HEIGHT;
  const minusHeight = isStat ? 36 : 0;
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: id });
  const { intervalValue, resultTime } = useWidgetTitle(id, reportType);
  const [tempWidgetInterval, setTempWidgetInterval] = useState<Record<string, string>>(widget?.query?.interval || {});
  const history = useHistory();
  const dispatch = useDispatch();
  const { haveToTakeSnapshot, setSvg } = useContext(WidgetSvgContext);
  const { setWidgetsLoading, widgetsLoadingDict } = useContext(WidgetsLoadingContext);
  const showNewInterval = useHasEntitlements(Entitlement.SHOW_TRELIS_NEW_INTERVAL, EntitlementCheckType.AND);
  const [widgetIntervalToggle, setWidgetIntervalToggle] = useState(false);
  const widgetRef = useRef();
  const titleRef = useRef();
  const intervalRef: any = useRef(null);
  const projectParams = useParams<ProjectPathProps>();

  const isDevProductivityTableReport = DEV_PROD_TABLE_REPORTS.includes(reportType as any);
  const isRawStatsReport = RAW_STATS_REPORTS.includes(reportType as any);

  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const { edit = true } = useParamSelector(isDashboardHasAccessSelector, dashboard);

  const handleClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
  }, []);

  const getImage = async (ref: any) => {
    // Fix: so that it do not block the ui rendering.
    setTimeout(async () => {
      const canvas = await html2canvas(ref.current);
      const img = canvas.toDataURL("image/png", 5.0);
      setSvg(id, img);
    }, 0);
  };
  useEffect(() => {
    if (
      title &&
      !showTooltip &&
      // @ts-ignore
      titleRef.current?.offsetWidth < getTextWidth(title, titleRef)
    ) {
      setShowTooltip(true);
    }
  }, [titleRef, title]);

  useEffect(() => {
    haveToTakeSnapshot && !loading && !hidden && getImage(widgetRef);
  }, [haveToTakeSnapshot, loading, hidden]);

  const containerStyle = useMemo(() => {
    const defaultStyle = {
      height: "100%",
      userSelect: "none"
    };
    const leadTimeStyle = {
      minHeight: "13rem"
      //margin: "-12px 4px" // negative used to override parent padding
    };
    switch (reportType as any) {
      case LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT:
      case AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_BY_TYPE_REPORT: {
        return {
          ...defaultStyle,
          minHeight: "21.9rem"
        };
      }
      case LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT:
      case LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT:
      case LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT:
      case AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT:
      case LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE:
      case LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE: {
        return {
          ...defaultStyle,
          ...leadTimeStyle
        };
      }
      case JIRA_MANAGEMENT_TICKET_REPORT.EFFORT_INVESTMENT_SINGLE_STAT_REPORT:
      case "azure_effort_investment_single_stat": {
        return {
          ...defaultStyle,
          minHeight: "11.9rem"
        };
      }
      case "review_collaboration_report": {
        return {
          ...defaultStyle,
          minHeight: "32rem"
        };
      }
      case jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT:
      case ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT: {
        return {
          ...defaultStyle,
          minHeight: "36rem",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center"
        };
      }
      case DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT: {
        return defaultStyle;
      }
      case MISCELLENEOUS_REPORTS.TABLE_REPORTS: {
        return {
          ...defaultStyle,
          minHeight: height,
          display: "inline-block",
          width: "100%"
        };
      }
      case JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT:
      case DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT:
      case DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS:
      case DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT:
      case DEV_PRODUCTIVITY_REPORTS.ORG_RAW_STATS:
      case JIRA_MANAGEMENT_TICKET_REPORT.PROGRESS_SINGLE_REPORT:
      case azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT:
      case ISSUE_MANAGEMENT_REPORTS.AZURE_ISSUES_PROGRESS_REPORT:
      case DORA_REPORTS.LEADTIME_CHANGES: {
        return {
          ...defaultStyle,
          minHeight: height
        };
      }
      default: {
        //TODO: we can change this to minHeight
        return {
          ...defaultStyle,
          height: height ? height : `${widgetHeight * rowHeight - 50 - minusHeight}px`
        };
      }
    }
  }, [reportType, height, widgetType]);
  const titleStyle = useMemo(() => ({ overflow: "hidden", textOverflow: "ellipsis" }), []);

  const displayColumnSelector = useMemo(() => {
    const displayColumnSelection = get(widgetConstants, [reportType, "displayColumnSelection"], false);
    return displayColumnSelection;
  }, [reportType]);

  const columnSelectorCheckbox = useMemo(() => {
    if (!displayColumnSelector) {
      return null;
    }
    return <WidgetColumnSelecor widget={widget} />;
  }, [widget]);

  const chartTitle = get(widgetConstants, [reportType, "chart_props", "chartTitle"], undefined);
  const cardTitle = useMemo(
    () => (
      <>
        {getCustomTitle ? (
          getCustomTitle({
            title,
            interval,
            widgetId: id,
            widgetRef,
            description,
            showTooltip,
            titleRef,
            titleStyle,
            chartTitle,
            resultTime
          })
        ) : (
          <WidgetCardTitle
            widgetRef={widgetRef}
            title={title}
            titleRef={titleRef}
            titleStyle={titleStyle}
            showTooltip={showTooltip}
            isStat={isStat}
            description={description}
          />
        )}
      </>
    ),
    [applicationType, icon_type, showActionButtons, title, showTooltip, resultTime]
  );

  const renderFilterMenu = (
    <Menu>
      <Menu.ItemGroup
        key={"filters-popup"}
        onClick={({ domEvent }) => domEvent.preventDefault()}
        className="filters-item">
        <WidgetFilterPreviewWrapper widgetId={id} dashboardId={dashboardId} />
      </Menu.ItemGroup>
    </Menu>
  );

  const handleExport = useCallback(() => {
    if (isDevProductivityTableReport) {
      dispatch(csvDownloadDevProductivity(dashboardId, id, { ou_ids: OU }));
    } else {
      dispatch(csvDownloadRawStats(dashboardId, id, { ou_ids: OU }));
    }
  }, [dashboardId, id]);

  const renderExportButton = useMemo(() => {
    if (!(isDevProductivityTableReport || isRawStatsReport)) {
      return null;
    }
    return (
      <Button onClick={handleExport} className="widget-extras dev-prod-export-btn">
        <Icon type="download" />
      </Button>
    );
  }, [reportType]);

  const oldAccess = getRBACPermission(PermeableMetrics.ADMIN_WIDGET_EXTRAS);
  const permissions = useDashboardPermissions();
  const hasEditAccess = window.isStandaloneApp ? oldAccess : permissions[1];

  const oldDoraAccess = getRBACPermission(PermeableMetrics.DORA_WIDGET_EXTRAS);
  const hasDoraAccess = window.isStandaloneApp ? oldDoraAccess : true;

  const renderInterval = useMemo(() => {
    const interval = widget.query?.hasOwnProperty("interval") ? widget.query.interval : undefined;
    if (
      interval &&
      Object.values(DEV_PRODUCTIVITY_REPORTS).includes(reportType as DEV_PRODUCTIVITY_REPORTS) &&
      hasDoraAccess
    ) {
      return (
        <>
          {!DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT.includes(
            reportType as DEV_PRODUCTIVITY_REPORTS
          ) && (
            <>
              <span
                ref={intervalRef}
                className="new-trellis-icon-view"
                onClick={e => {
                  e?.preventDefault();
                  setWidgetIntervalToggle(!widgetIntervalToggle);
                }}>
                <Icon type="calendar" />
                {intervalValue && (
                  <>
                    {intervalValue} <span>|</span>
                  </>
                )}
                {toTitleCase(tempWidgetInterval?.[id] || widget?.query?.interval)}
                <Select
                  open={widgetIntervalToggle}
                  onDropdownVisibleChange={() => setWidgetIntervalToggle(!widgetIntervalToggle)}
                  mode="default"
                  value={tempWidgetInterval?.[id] || widget?.query?.interval}
                  onChange={(e: any) => {
                    setTempWidgetInterval({ [id]: e });
                  }}
                  className="custom-select-container"
                  dropdownRender={menu => (
                    <div
                      className="custom-interval-select"
                      style={{
                        left: `-${intervalRef.current?.offsetWidth - 5 || "240"}px`,
                        width: intervalRef.current?.offsetWidth || "240"
                      }}>
                      {menu}
                    </div>
                  )}>
                  {(showNewInterval ? DEV_PRODUCTIVITY_INTERVAL_OPTIONS : OLD_INTERVAL).map(data => (
                    <Option value={data.value}>{data.label}</Option>
                  ))}
                </Select>
              </span>
              <GetInfoIcon />
            </>
          )}
        </>
      );
    }
    return null;
  }, [reportType, intervalValue, id, tempWidgetInterval, showNewInterval, widgetIntervalToggle, intervalRef]);

  const renderContent = useMemo(() => {
    return (
      <AntParagraph className="widget-or-filter-help">
        {DISABLED_FILTERS_TEXT}
        {
          <span
            className="edit-filters-btn"
            onClick={() => history.push(WebRoutes.dashboard.widgets.details(projectParams, dashboardId, id))}>
            Edit Filters
          </span>
        }
      </AntParagraph>
    );
  }, [id, disableGlobalFilters]);
  const widgetActionSelectFilter = get(widgetConstants, [reportType, "widgetActionSelectFilter"], undefined);

  const renderSelectFilter = useMemo(() => {
    if (!widgetActionSelectFilter) {
      return null;
    }
    const widgetSelectFilters = Object.keys(widgetActionSelectFilter || {});
    return widgetSelectFilters.map((filter: any) => {
      const filterConfig = widgetActionSelectFilter[filter];
      const value = get(widget, ["metadata", filterConfig.datakey], filterConfig.defaultValue);
      return (
        <WidgetSelectComponent
          dataKey={filterConfig.datakey}
          selectOptions={filterConfig.options}
          selectedValue={value}
          widgetId={id}
          prefixLabel={filterConfig.prefixLabel}
        />
      );
    });
  }, [widget, widgetActionSelectFilter, id]);

  const renderFilterDropdown = useMemo(() => {

    if (disableGlobalFilters) {
      return (
        <Dropdown overlay={renderFilterMenu} trigger={["click"]} placement="bottomRight">
          <AntPopover trigger="hover" overlayClassName="widget-or-filter-help-container" content={renderContent}>
            <AntBadge dot className="filters-badge">
              <AntButton className="widget-extras widget-extras-disabled">
                <SvgIcon icon={"widgetFiltersIcon"} />
              </AntButton>
            </AntBadge>
          </AntPopover>
        </Dropdown>
      );
    }

    return (
      <Dropdown overlay={renderFilterMenu} trigger={["click"]} placement="bottomRight">
        <Button className="widget-extras">
          <SvgIcon icon={"widgetFiltersIcon"} />
        </Button>
      </Dropdown>
    );
  }, [id, dashboardId, disableGlobalFilters]);

  const handleWidgetLoadingChange = (widgetId: string, _loading: boolean) => {
    if (hidden) {
      return;
    }
    setWidgetsLoading(widgetId, _loading);
    setLoading(_loading);
  };

  const widgetLoadingContextValue = useMemo(
    () => ({
      setWidgetLoading: handleWidgetLoadingChange,
      isThisWidgetLoading: get(widgetsLoadingDict, [id], false)
    }),
    [setWidgetsLoading, widgetsLoadingDict, id]
  );

  const statWidgetBGColorContextValue = useMemo(
    () => ({
      setWidgetBGColor: (widgetId: string, color: string) => setWidgetBGColor(prev => ({ ...prev, [widgetId]: color })),
      widgetBGColor
    }),
    [widgetBGColor]
  );

  const statWidgetIntervalValue = useMemo(
    () => ({
      setTempWidgetInterval: (widgetId: string, interval: any) =>
        setTempWidgetInterval(prev => ({ ...prev, [widgetId]: interval })),
      tempWidgetInterval
    }),
    [tempWidgetInterval]
  );

  const memoizedBGStyle = useMemo(() => ({ backgroundColor: widgetBGColor[id] || STAT_BG_WHITE }), [widgetBGColor, id]);

  return (
    <WidgetLoadingContext.Provider value={widgetLoadingContextValue}>
      <WidgetBGColorContext.Provider value={statWidgetBGColorContextValue}>
        <WidgetIntervalContext.Provider value={statWidgetIntervalValue}>
          <div ref={widgetRef as any}>
            <AntCard
              style={{ width: "100%", textAlign: "center" }}
              className={cx(
                { widget: widgetType.includes("stats"), drilldownSelected: drilldownSelected },
                `widget`,
                id
              )}
              title={cardTitle}
              onClickEvent={handleClick}
              extra={
                <div className="widget-extra">
                  {showActionButtons && (
                    <>
                      {columnSelectorCheckbox}
                      {renderInterval}
                      {renderSelectFilter}
                      {renderExportButton}
                      {renderFilterDropdown}
                    </>
                  )}
                  {edit && hasEditAccess && <WidgetExtras widgetId={id} dashboardId={dashboardId} />}
                </div>
              }>
              <div style={containerStyle as any}>{children}</div>
            </AntCard>
          </div>
        </WidgetIntervalContext.Provider>
      </WidgetBGColorContext.Provider>
    </WidgetLoadingContext.Provider>
  );
};

export default React.memo(Widget);
