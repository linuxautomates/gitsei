import React, { useMemo } from "react";
import { AntButton, AntText, SvgIcon } from "shared-resources/components";
import "./dashboard.components.scss";
import { AntBadgeComponent } from "shared-resources/components/ant-badge/ant-badge.component";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { Icon } from "antd";

interface DrilldownHeaderProps {
  uri: string;
  method: string;
  uuid?: string;
}

const DrilldownHeader: React.FC<DrilldownHeaderProps> = (props: DrilldownHeaderProps) => {
  const { uri, method, uuid = "0" } = props;

  const recordApiState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: method,
    uuid: uuid
  });

  const recordCount = useMemo(() => (recordApiState?.data?.records || []).length, [recordApiState]);

  const externalLinkStyle = useMemo(
    () => ({
      width: 16,
      height: 16,
      align: "center"
    }),
    []
  );

  const handleCSVDownload = () => {};

  return (
    <div className="drilldown-header">
      <div className="drilldown-title-container">
        <AntText className="drilldown-title">Drilldown Preview</AntText>
        <AntBadgeComponent count={recordCount} overflowCount={1000} className="drilldown-record-count" />
      </div>
      <div className="drilldown-actions-container">
        <AntButton onClick={handleCSVDownload} type="default" className="csv-download-btn">
          <Icon type="download" />
        </AntButton>
        <AntButton onClick={handleCSVDownload} type="default" className="extra-filter-content__download-icon">
          <SvgIcon icon="externalLink" style={externalLinkStyle} />
        </AntButton>
      </div>
    </div>
  );
};

export default React.memo(DrilldownHeader);
