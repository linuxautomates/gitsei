import React, { useMemo } from "react";
import { get } from "lodash";
import queryString from "query-string";
import CompactReport from "model/report/CompactReport";
import { AntIcon, AntTag, AntText, AntTooltip } from "shared-resources/components";
import widgetConstants from "../../../../constants/widgetConstants";
import {
  DEPRECATED_MESSAGE,
  DEPRECATED_NOT_ALLOWED,
  DEPRECATED_REPORT,
  doraReports,
  LEAD_TIME_MTTR_REPORTS
} from "../../../../constants/applications/names";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { useLocation } from "react-router-dom";
import { useParamSelector } from "reduxConfigs/selectors/selector";

const ReportRow: React.FC<{ report: CompactReport; onClick: (report: CompactReport) => void }> = ({
  report,
  onClick
}) => {
  const isDeprecated = get(widgetConstants, [report.report_type, DEPRECATED_REPORT], false);
  const isDeprecatedAndNotAllowed = get(widgetConstants, [report.report_type, DEPRECATED_NOT_ALLOWED], false);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  function handleReportClick() {
    report.supported_by_integration && onClick(report);
  }
  const tooltipMessage = "Insight doesn't have the required integrations or Collection";
  const tooltipMessageForWorkflowProfileOU = "The associated profile for this widget is incomplete.";
  const title = useMemo(() => {
    if (isDeprecated && (report.name || "").length > 28) {
      return report.name || "";
    } else if ((report.name || "").length > 36) {
      return report.name || "";
    }
    return null;
  }, [report]);

  if (isDeprecatedAndNotAllowed) {
    const InfoMessage = get(widgetConstants, [report.report_type, DEPRECATED_MESSAGE], "");
    return (
      <div className="lib-category-list__list--disabled-row">
        <AntTooltip title={title}>
          <div className="disabled-row-text">{report.name}</div>
        </AntTooltip>
        <AntTag color="purple" className="report-deprecated">
          Deprecated
        </AntTag>
        <AntTooltip placement="right" title={InfoMessage}>
          <AntIcon type="info-circle"></AntIcon>
        </AntTooltip>
      </div>
    );
  }

  if (!workspaceProfile && [...doraReports, ...LEAD_TIME_MTTR_REPORTS].includes(report.key as any)) {
    return (
      <div className="lib-category-list__list--disabled-row">
        <div className="disabled-row-text">{report.name}</div>
        <AntTooltip placement="right" title={tooltipMessageForWorkflowProfileOU}>
          <AntIcon type="info-circle"></AntIcon>
        </AntTooltip>
      </div>
    );
  }

  if (report.supported_by_integration) {
    return (
      <div className="lib-category-list__list--name" onClick={handleReportClick}>
        <AntTooltip title={title}>
          <AntText className="report-name">{report.name}</AntText>
        </AntTooltip>
        {isDeprecated && (
          <AntTag color="purple" className="report-deprecated">
            Deprecated
          </AntTag>
        )}
      </div>
    );
  }

  return (
    <div className="lib-category-list__list--disabled-row" onClick={handleReportClick}>
      <div className="disabled-row-text">{report.name}</div>
      <AntTooltip placement="right" title={tooltipMessage}>
        <AntIcon type="info-circle"></AntIcon>
      </AntTooltip>
    </div>
  );
};

export default React.memo(ReportRow);
