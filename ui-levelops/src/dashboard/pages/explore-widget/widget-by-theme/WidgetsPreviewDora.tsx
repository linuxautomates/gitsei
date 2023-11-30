import React, { useEffect } from "react";
import { Empty } from "antd";
import "./WidgetsPreview.scss";
import { useDispatch, useSelector } from "react-redux";
import {
  libraryReportListSelector,
  showSupportedOnlyReportsSelector
} from "reduxConfigs/selectors/widgetLibrarySelectors";
import { resetWidgetLibraryState } from "reduxConfigs/actions/widgetLibraryActions";
import CompactReport from "../../../../model/report/CompactReport";
import WidgetPreviewDora from "./WidgetPreviewDora";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { LTFC_MTTR_REPORTS } from "dashboard/constants/applications/names";

interface WidgetsPreviewDoraProps {
  selectedCategory: string;
  setWidgetInfoData: (value: CompactReport) => void;
  isChecked: any;
  setIsChecked: any;
}

const WidgetsPreviewDora: React.FC<WidgetsPreviewDoraProps> = ({
  selectedCategory,
  setWidgetInfoData,
  isChecked,
  setIsChecked
}) => {
  const dispatch = useDispatch();

  let reportList = useSelector(libraryReportListSelector);
  const showSupportedOnly = useSelector(showSupportedOnlyReportsSelector);
  const LTFCAndMTTRSupport = useHasEntitlements(Entitlement.LTFC_MTTR_DORA_IMPROVEMENTS, EntitlementCheckType.AND);
  if (showSupportedOnly) {
    reportList = reportList.filter((report: CompactReport) => report.supported_by_integration);
  }
  if (!LTFCAndMTTRSupport) {
    reportList = reportList.filter((report: any) => !LTFC_MTTR_REPORTS.includes(report.key));
  }

  useEffect(() => {
    return () => {
      dispatch(resetWidgetLibraryState());
    };
  }, []);

  const renderList = () => {
    return reportList.map((theme: CompactReport, index: number) => (
      <WidgetPreviewDora
        key={`theme-${theme.key}`}
        theme={theme}
        selectedCategory={selectedCategory}
        setWidgetInfoData={setWidgetInfoData}
        isChecked={isChecked}
        setIsChecked={setIsChecked}
      />
    ));
  };

  if (!reportList.length) {
    return <Empty description="No available reports found" />;
  }

  return <div className="widget-previews-container">{renderList()}</div>;
};

export default React.memo(WidgetsPreviewDora);
