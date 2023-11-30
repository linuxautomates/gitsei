import React from "react";
import { AntIcon, AntTooltip } from "shared-resources/components";
import "./ReportHeaderInfo.scss";

interface ReportHeaderInfoProps {
  reportHeaderInfoData: Array<Record<string, any>>;
}

const ReportHeaderInfo: React.FC<ReportHeaderInfoProps> = ({ reportHeaderInfoData = [] }) => {
  return (
    <div className="report-header-container">
      {reportHeaderInfoData.map((item: any, index: number) => {
        return (
          <div key={index} className="breakdown-element">
            <div>
              <span className="text">{item.label}</span>{" "}
              {item.infoIcon && (
                <AntTooltip title={item.infoValue} trigger={["hover", "click"]}>
                  <AntIcon type="info-circle" theme="outlined" className={"description-icon"} />
                </AntTooltip>
              )}
            </div>
            <div className="label">
              <span className="value-text">{item.value}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default ReportHeaderInfo;
