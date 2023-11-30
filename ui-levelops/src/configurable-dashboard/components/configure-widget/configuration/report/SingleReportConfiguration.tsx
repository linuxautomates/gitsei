import React, { useState, useEffect, useMemo } from "react";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";

import "./SingleReportConfiguration.scss";
import { RestWidget } from "classes/RestDashboards";
import ReportConfigurationTabs from "./../tabs/ReportConfigurationTabs";
import { AntIcon, AntText } from "shared-resources/components";
import { getSelectedChildWidget } from "reduxConfigs/selectors/widgetSelector";
import { setSelectedChildId } from "reduxConfigs/actions/restapi/restapiActions";
import widgetConstants from "dashboard/constants/widgetConstants";
import WidgetInfoDrawer, { WidgetInfo } from "dashboard/pages/explore-widget/widget-info-drawer/WidgetInfoDrawer";
import { getIssueManagementReportType } from "dashboard/graph-filters/components/helper";
import { getReportDocs } from "reduxConfigs/actions/restapi/reportDocs.action";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { reportDocsGetSelector } from "reduxConfigs/selectors/widgetReportDocsSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface SingleReportConfigurationProps {
  dashboardId: string;
  widgetId: string;
}

const SingleReportConfiguration: React.FC<SingleReportConfigurationProps> = ({ dashboardId, widgetId }) => {
  const dispatch = useDispatch();

  const widget: RestWidget | null = useSelector(getSelectedChildWidget);

  const reportType = widget?.type || "";

  const reportName = useMemo(() => get(widgetConstants, [reportType, "name"], ""), [reportType]);
  const reportKey = useMemo(
    () => getIssueManagementReportType(widget?.reportType, IntegrationTypes.JIRA),
    [widget?.reportType]
  );
  const reportDocState: { data: WidgetInfo } = useParamSelector(reportDocsGetSelector, { report_id: reportKey });

  const [showWidgetInfoDrawer, setShowWidgetInfoDrawer] = useState<boolean>(false);
  const [isDocsAvailable, setIsDocsAvailable] = useState<boolean>(false);

  useEffect(() => {
    const isDocLoaded = get(reportDocState, "isDocLoaded", true);
    if (reportKey && !isDocLoaded) {
      dispatch(getReportDocs(reportKey));
    }
  }, [reportKey]);

  useEffect(() => {
    const isDocLoaded = get(reportDocState, "isDocLoaded", true);
    if (isDocLoaded) {
      const loading = get(reportDocState, "loading", true);
      const error = get(reportDocState, "error", true);
      if (!loading) {
        setIsDocsAvailable(!error);
      }
    }
  }, [reportDocState]);

  const widgetInfoDrawer = useMemo(() => {
    if (!showWidgetInfoDrawer) {
      return null;
    }
    // TODO: it will be widget.compactReport, which will return a Object of CompactReport type.
    const compactInfo: any = {
      key: reportKey,
      name: widget?.reportName
    };
    return (
      <WidgetInfoDrawer
        reportTheme={compactInfo}
        visible
        onClose={() => {
          setShowWidgetInfoDrawer(false);
        }}
      />
    );
  }, [showWidgetInfoDrawer, widget]);

  useEffect(() => {
    dispatch(setSelectedChildId(widgetId));
  }, [widgetId]);

  return (
    <div className="h-100 overflow-hidden">
      <div className="report-details-container">
        <div className="report-label">
          <span>
            <AntText strong className="label">
              Report
            </AntText>
          </span>
          <AntText className="report-name">{reportName}</AntText>
        </div>
        {isDocsAvailable && (
          <AntIcon type="question-circle" className="question-icon" onClick={() => setShowWidgetInfoDrawer(true)} />
        )}
      </div>
      <ReportConfigurationTabs dashboardId={dashboardId} widgetId={widgetId} />
      {widgetInfoDrawer}
    </div>
  );
};

export default React.memo(SingleReportConfiguration);
