import React, { useEffect } from "react";
import { Empty } from "antd";

import "./WidgetsPreview.scss";
import WidgetPreview from "./WidgetPreview";
import { useDispatch, useSelector } from "react-redux";
import {
  libraryReportListSelector,
  showSupportedOnlyReportsSelector
} from "reduxConfigs/selectors/widgetLibrarySelectors";
import { resetWidgetLibraryState } from "reduxConfigs/actions/widgetLibraryActions";
import CompactReport from "../../../../model/report/CompactReport";

interface WidgetsPreviewProps {
  selectedCategory: string;
  setWidgetInfoData: (value: CompactReport) => void;
}

const WidgetsPreview: React.FC<WidgetsPreviewProps> = ({ selectedCategory, setWidgetInfoData }) => {
  const dispatch = useDispatch();

  let reportList = useSelector(libraryReportListSelector);
  const showSupportedOnly = useSelector(showSupportedOnlyReportsSelector);
  if (showSupportedOnly) {
    reportList = reportList.filter((report: CompactReport) => report.supported_by_integration);
  }

  useEffect(() => {
    return () => {
      dispatch(resetWidgetLibraryState());
    };
  }, []);

  const renderList = () => {
    return reportList.map((theme: CompactReport, index: number) => (
      <WidgetPreview
        key={`theme-${theme.key}`}
        theme={theme}
        selectedCategory={selectedCategory}
        setWidgetInfoData={setWidgetInfoData}
      />
    ));
  };

  if (!reportList.length) {
    return <Empty description="No available reports found" />;
  }

  return <div className="widget-previews-container">{renderList()}</div>;
};

export default React.memo(WidgetsPreview);
