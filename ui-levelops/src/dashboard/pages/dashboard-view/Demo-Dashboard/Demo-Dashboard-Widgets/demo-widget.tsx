import React, { useMemo, useRef, useState } from "react";
import cx from "classnames";
import { AntCard, Button, SvgIcon, AntText } from "shared-resources/components";
import { Dropdown, Icon, Menu } from "antd";
import WidgetCardTitle from "dashboard/components/widgets/WidgetCardTitle";
import { AZURE_LEAD_TIME_ISSUE_REPORT, LEAD_TIME_REPORTS } from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { viewByOptions } from "./../../../../pages/scorecard/components/PRActivity/helpers";
import { get } from "lodash";
import "./demo-widget.style.scss";
import { DEV_PRODUCTIVITY_REPORTS } from "dashboard/constants/applications/names";
import DemoWidgetFilterPreviewWrapper from "../Demo-Dashboard-Widgets-Filter/DemoWidgetFilterPreviewWrapper";

export const WIDGET_HEIGHT = 5;
export const STAT_WIDGET_HEIGHT = 2;
export const ROW_HEIGHT = 80;
export const STATS_ROW_HEIGHT = 94;
export const ICON_FULL_SCREEN = "arrows-alt";
export const ICON_FULL_SCREEN_EXIT = "shrink";
export const DISABLED_FILTERS_TEXT = "Dashboard filters are disabled";

interface DemoWidgetContainerProps {
  id: string;
  width: string;
  title: string;
  description?: string;
  widgetType: string;
  reportType: string;
  height?: string;
  dashboardId: string;
  widgetData: any;
  drilldownSelected: boolean;
}

export const DemoWidget: React.FC<DemoWidgetContainerProps> = ({
  id,
  title,
  children,
  reportType,
  widgetType,
  height,
  description,
  widgetData,
  drilldownSelected
}) => {
  const [showTooltip, setShowTooltip] = useState(false);
  const isStat = widgetType?.includes("stats");
  const widgetHeight = isStat ? STAT_WIDGET_HEIGHT : WIDGET_HEIGHT;
  const rowHeight = isStat ? STATS_ROW_HEIGHT : ROW_HEIGHT;
  const minusHeight = isStat ? 36 : 0;
  const defaultDescription = get(widgetConstants, [reportType, "description"], undefined);
  const [viewBy, setViewBy] = useState<string>(viewByOptions[0].key);
  const widgetRef = useRef();
  const titleRef = useRef();

  const containerStyle = useMemo(() => {
    const defaultStyle = {
      height: "100%",
      userSelect: "none"
    };
    switch (reportType as any) {
      case LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT:
      case LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT:
      case AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT: {
        return {
          ...defaultStyle,
          minHeight: "15rem"
        };
      }
      case "review_collaboration_report": {
        return {
          ...defaultStyle,
          minHeight: "32rem"
        };
      }
      case "effort_investment_single_stat": {
        return {
          ...defaultStyle,
          minHeight: "11.9rem"
        };
      }
      default: {
        return {
          ...defaultStyle,
          minHeight: height ? height : `${widgetHeight * rowHeight - 50 - minusHeight}px`
        };
      }
    }
  }, [reportType, height, widgetType]);

  const titleStyle = useMemo(() => ({ overflow: "hidden", textOverflow: "ellipsis" }), []);

  const cardTitle = useMemo(
    () => (
      <>
        {
          <WidgetCardTitle
            widgetRef={widgetRef}
            title={(widgetData?.title || title)?.toUpperCase()}
            titleRef={titleRef}
            titleStyle={titleStyle}
            showTooltip={showTooltip}
            isStat={isStat}
            description={description ?? defaultDescription}
          />
        }
      </>
    ),
    [title, showTooltip, description, widgetData, defaultDescription, viewBy]
  );

  const renderFilterMenu = (
    <Menu>
      <Menu.ItemGroup
        key={"filters-popup"}
        onClick={({ domEvent }) => domEvent.preventDefault()}
        className="filters-item">
        <DemoWidgetFilterPreviewWrapper widgetData={widgetData} />
      </Menu.ItemGroup>
    </Menu>
  );

  const renderInterval = useMemo(() => {
    const interval = widgetData.query?.hasOwnProperty("interval") ? widgetData.query.interval : undefined;
    if (interval && Object.values(DEV_PRODUCTIVITY_REPORTS).includes(reportType as DEV_PRODUCTIVITY_REPORTS)) {
      return (
        <div style={{ marginRight: "80px", marginTop: "-10px" }}>
          <AntText style={{ fontSize: "16px" }}>Interval: </AntText>
          <AntText>{interval?.replace("_", " ")}</AntText>
        </div>
      );
    }
    return null;
  }, [reportType]);

  return (
    <div ref={widgetRef as any}>
      <AntCard
        style={{ width: "100%" }}
        className={cx({ widget: widgetType?.includes("stats"), drilldownSelected: drilldownSelected }, `widget`, id)}
        extra={<div className="widget-extra">{renderInterval}</div>}
        title={cardTitle}>
        <div>
          <Button className="widget-extras demo-moreIcon">
            <Icon type={"more"} />
          </Button>
        </div>
        {!isStat && (
          <div className="demo-filtericon">
            <Dropdown overlay={renderFilterMenu} trigger={["click"]} placement="bottomRight">
              <Button className="widget-extras">
                <SvgIcon icon={"widgetFiltersIcon"} />
              </Button>
            </Dropdown>
          </div>
        )}
        <div style={containerStyle as any}>{children}</div>
      </AntCard>
    </div>
  );
};

export default DemoWidget;
