import React, { useEffect, useState } from "react";
import { Button, Drawer, Table } from "antd";
import { useDispatch } from "react-redux";
import { get } from "lodash";

import "./WidgetInfoDrawer.scss";
import { getReportDocs } from "reduxConfigs/actions/restapi/reportDocs.action";
import { b64DecodeUnicode } from "utils/stringUtils";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { reportDocsGetSelector } from "reduxConfigs/selectors/widgetReportDocsSelector";
import { AntCol, AntTag, AntText } from "../../../../shared-resources/components";
import Loader from "components/Loader/Loader";
import CompactReport from "../../../../model/report/CompactReport";
import AuthImage from "../../../../shared-resources/components/auth-image/AuthImage";
import sanitizeHtml from "sanitize-html";

export interface WidgetInfo {
  title: string;
  "report-categories": string[];
  variants: string[];
  "related-reports": string[];
  applications: string[];
  content: string;
  "image-url": string;
}

interface WidgetInfoDrawerProps {
  reportTheme: CompactReport | undefined;
  visible?: boolean;
  onClose: () => void;
}

const WidgetInfoDrawer: React.FC<WidgetInfoDrawerProps> = ({ visible, onClose, reportTheme }) => {
  const dispatch = useDispatch();
  const [loadingInfo, setLoadingInfo] = useState(false);
  const [widgetInfo, setWidgetInfo] = useState<WidgetInfo>();

  const widgetInfoState: { data: WidgetInfo } = useParamSelector(reportDocsGetSelector, {
    report_id: reportTheme?.key
  });

  useEffect(() => {
    const isDocLoaded = get(widgetInfoState, "isDocLoaded", true);
    if (reportTheme?.key) {
      if (!isDocLoaded) {
        dispatch(getReportDocs(reportTheme.key));
      }
      setLoadingInfo(true);
    }
  }, [reportTheme]);

  useEffect(() => {
    if (loadingInfo) {
      const loading = get(widgetInfoState, "loading", true);
      const error = get(widgetInfoState, "error", true);

      if (!loading && !error) {
        setWidgetInfo(widgetInfoState["data"]);
        setLoadingInfo(false);
      }
    }
  }, [widgetInfoState, loadingInfo]);

  let parsedContent: string[] = [];
  if (widgetInfo?.content) {
    // extracting the image to make auth calls for them
    parsedContent = b64DecodeUnicode(widgetInfo.content).split(/<img.*?src="(.*?)"[^\>]+>/g);
  }
  const body = (
    <div className="widget-info-drawer">
      <AntText className="mb-10 mt-30" strong>
        {widgetInfo?.title}
      </AntText>
      <div>
        {widgetInfo &&
          widgetInfo["report-categories"]?.map((category: string) => <AntTag color="purple">{category}</AntTag>)}
      </div>
      <AntText type="secondary">
        {widgetInfo?.content &&
          parsedContent.length > 0 &&
          parsedContent.map((content: string) => {
            if (content.startsWith("https")) {
              return <AuthImage className={"content-img-preview"} key={content} src={content} alt="report image" />;
            } else {
              // sanitizing data to prevent xss
              const sanitizedContent = sanitizeHtml(content);
              return <div key={content} className={"content"} dangerouslySetInnerHTML={{ __html: sanitizedContent }} />;
            }
          })}
      </AntText>
    </div>
  );

  const footer = (
    <div
      style={{
        position: "absolute",
        right: 0,
        bottom: 0,
        width: "100%",
        borderTop: "1px solid #e9e9e9",
        padding: "10px 16px",
        background: "#fff",
        textAlign: "right"
      }}>
      <Button onClick={onClose} type="primary">
        Close
      </Button>
    </div>
  );

  return (
    <Drawer className="widget-info-drawer-class" title="Learn More" width={660} visible={visible} onClose={onClose}>
      {loadingInfo && <Loader />}
      {!loadingInfo && body}
      {!loadingInfo && footer}
    </Drawer>
  );
};

export default WidgetInfoDrawer;
