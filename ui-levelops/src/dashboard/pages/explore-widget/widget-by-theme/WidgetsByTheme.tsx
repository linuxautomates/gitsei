import React, { useContext, useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { capitalize, get, sortBy } from "lodash";
import "./WidgetsByTheme.scss";
import { useHeader } from "../../../../custom-hooks/useHeader";
import { generateExploreWidgetByThemePageBreadcrumbs } from "../../../../utils/dashboardUtils";
import { DashboardWidgetResolverContext } from "../../context";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { AntText } from "../../../../shared-resources/components";
import WidgetActionBar from "../action-bar/WidgetActionBar";
import WidgetsPreview from "./WidgetsPreview";
import WidgetInfoDrawer from "../widget-info-drawer/WidgetInfoDrawer";
import Loader from "components/Loader/Loader";
import { listReportDocs } from "reduxConfigs/actions/restapi/reportDocs.action";
import { allReportsDocsListSelector, reportDocsListSelector } from "reduxConfigs/selectors/widgetReportDocsSelector";
import { getAllCompactReports, mapWidgetLibraryApiList, removeFEMiscReportPresentInAPIData } from "../reportHelper";
import { setWidgetLibraryList } from "reduxConfigs/actions/widgetLibraryActions";
import CompactReport from "../../../../model/report/CompactReport";
import {
  libraryReportListSelector,
  showSupportedOnlyReportsSelector
} from "reduxConfigs/selectors/widgetLibrarySelectors";
import { Badge } from "antd";
import { IS_FRONTEND_REPORT } from "../../../constants/filter-key.mapping";
import { SET_REPORTS } from "reduxConfigs/actions/actionTypes";
import { ALL_REPORTS } from "../report.constant";
import { ProjectPathProps } from "classes/routeInterface";

interface WidgetByThemeProps extends RouteComponentProps {}

const WidgetsByTheme: React.FC<WidgetByThemeProps> = ({ match, history, location }) => {
  const dispatch = useDispatch();

  const [showWidgetInfoDrawer, setWidgetInfoVisibility] = useState(false);
  const [category, setCategory] = useState<string>("");
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [selectedReportTheme, setSelectedReportTheme] = useState<undefined | CompactReport>();

  const { setupHeader } = useHeader(location.pathname);
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const dashboard = useSelector(selectedDashboard);
  const reportDocsState = useSelector(reportDocsListSelector);
  const allReportsDocsState = useSelector(allReportsDocsListSelector);
  let reportList = useSelector(libraryReportListSelector);
  const showSupportedOnly = useSelector(showSupportedOnlyReportsSelector);
  if (showSupportedOnly) {
    reportList = reportList.filter((report: CompactReport) => report.supported_by_integration);
  }

  useEffect(() => {
    const _category = (match.params as any).typeId;
    if (_category !== "miscellaneous") {
      return;
    }
    dispatch(listReportDocs({ filter: {} }, SET_REPORTS, ALL_REPORTS));
  }, []);

  useEffect(() => {
    const _category = (match.params as any).typeId;
    const title = _category
      .split("_")
      .map((str: string) => capitalize(str))
      .join(" ");
    const pageSettings = {
      title,
      action_buttons: {},
      bread_crumbs: generateExploreWidgetByThemePageBreadcrumbs(
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
    dispatch(listReportDocs({ filter }));
  }, [dashboard]); // eslint-disable-line react-hooks/exhaustive-deps

  const getAdditionalMiscReports = () => {
    const loading = get(allReportsDocsState, "loading", true);
    const error = get(allReportsDocsState, "error", true);
    if (!loading && !error) {
      const data = get(allReportsDocsState, ["data", "records"]);
      const allFrontendReports = getAllCompactReports();
      let additionalMiscReports: any = allFrontendReports
        .filter(report => !!report?.[IS_FRONTEND_REPORT])
        .map(report => ({
          ...report,
          key: report.key,
          categories: report?.categories.map(category => category.toLowerCase().replace(/ /g, "_")),
          hide_learn_more_button: true,
          supported_by_integration: false
        }));

      // FRISKING POINT: TO MAKE SURE THAT THE FRONT END REPORTS ARE NOT ALREADY UPDATED AT BACKEND
      additionalMiscReports = removeFEMiscReportPresentInAPIData(additionalMiscReports, data);

      return additionalMiscReports;
    }
    return [];
  };

  useEffect(() => {
    if (loadingCategories && !allReportsDocsState?.loading) {
      const loading = get(reportDocsState, "loading", true);
      const error = get(reportDocsState, "error", true);
      if (!loading && !error) {
        const _category = (match.params as any).typeId;
        const data = get(reportDocsState, ["data", "records"]);
        const mappedData = mapWidgetLibraryApiList(data);
        const allReports = getAllCompactReports();
        // only showing the reports from BE
        let finalReports = mappedData
          .map((item: any) => {
            const report = allReports.find((_report: any) => item.key === _report.key);
            if (report) {
              return {
                ...item,
                name: report.name,
                key: item.key,
                applications: report.applications,
                report_type: report.report_type,
                categories: report?.categories.map(category => category.toLowerCase().replace(/ /g, "_")),
                supported_widget_types: report.supported_widget_types
              };
            }
          })
          .filter((item: any) => item !== undefined);

        if (_category === "miscellaneous") {
          const additionalMiscReports = getAdditionalMiscReports();
          finalReports = [...finalReports, ...additionalMiscReports];
        }

        finalReports = sortBy(finalReports, ["name"]);
        dispatch(setWidgetLibraryList(finalReports));
        setLoadingCategories(false);
      }
    }
  }, [reportDocsState, allReportsDocsState]);

  const handleWidgetInfoDataChange = (theme: CompactReport) => {
    setSelectedReportTheme(theme);
    setWidgetInfoVisibility(true);
  };

  const handleOnDrawerClose = () => {
    setSelectedReportTheme(undefined);
    setWidgetInfoVisibility(false);
  };

  return (
    <>
      <div className="widgets-by-theme h-100 pb-0">
        <div className="header">
          <div className="info">
            <div className="d-flex align-center">
              <AntText className="info__title">Explore Widgets </AntText>
              <Badge
                style={{
                  margin: "0 0.5rem",
                  backgroundColor: "var(--harness-blue)",
                  fontSize: "14px",
                  marginLeft: "3px"
                }}
                count={reportList.length}
                overflowCount={1000}
              />
            </div>
            <AntText className="info__sub-title">
              Select and configure a widget - browse widgets list or by their categories{" "}
            </AntText>
          </div>
          <WidgetActionBar hideCategoryFilter={true} />
        </div>
        <div className="content">
          {loadingCategories && <Loader />}
          {!loadingCategories && (
            <WidgetsPreview selectedCategory={category} setWidgetInfoData={handleWidgetInfoDataChange} />
          )}
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

export default WidgetsByTheme;
