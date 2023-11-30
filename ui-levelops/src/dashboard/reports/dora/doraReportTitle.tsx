import { WidgetFilterContext } from "dashboard/pages/context";
import { get } from "lodash";
import React, { useContext } from "react";
import { AntIcon, AntText, AntTooltip } from "shared-resources/components";
import "./doraReportTitle.scss";

interface DoraReportTitleProps {
  title: string;
  dashboardTimeRange?: any;
  widgetId: string;
  widgetRef?: any;
  showTooltip?: boolean;
  titleRef: any;
  titleStyle: any;
  isStat?: boolean;
  description?: string;
  chartTitle?: string;
}

const DoraReportTitle = (props: DoraReportTitleProps) => {
  const { widgetRef, showTooltip, title, widgetId, titleRef, titleStyle, description, chartTitle } = props;
  const { filters } = useContext(WidgetFilterContext);
  const doraTitle = get(filters, [widgetId, "doraTitle"], "");
  const doraInterval = get(filters, [widgetId, "doraInterval"], "");
  return (
    <div className="flex align-center justify-start mr-6 doraReportTitle">
      <div className="flex nameContainer">
        <AntTooltip title={title} trigger="hover" getTooltipContainer={() => widgetRef.current}>
          <div className="titleDoraClass">
            <span ref={titleRef as any} style={titleStyle}>
              {title}
            </span>
          </div>
        </AntTooltip>
        {description && (
          <span className="description-dora-icon">
            <AntTooltip
              title={description}
              trigger={["hover", "click"]}
              getPopupContainer={(trigger: any) => widgetRef.current}>
              <AntIcon type="info-circle" theme="outlined" />
            </AntTooltip>
          </span>
        )}
      </div>
      <div className="doraTitleWidth">
        {doraTitle && doraInterval && <AntText strong>{`${doraTitle} per ${doraInterval}`}</AntText>}
        {!(doraTitle && doraInterval) && chartTitle && <AntText strong>{chartTitle}</AntText>}
      </div>
    </div>
  );
};

export default DoraReportTitle;

export const getDoraReportsTitle = (args: any) => {
  return (
    <DoraReportTitle
      title={args.title}
      widgetId={args.widgetId}
      widgetRef={args.widgetRef}
      titleRef={args.titleRef}
      showTooltip={args.showTooltip}
      titleStyle={args.titleStyle}
      description={args.description}
      chartTitle={args.chartTitle}
    />
  );
};
