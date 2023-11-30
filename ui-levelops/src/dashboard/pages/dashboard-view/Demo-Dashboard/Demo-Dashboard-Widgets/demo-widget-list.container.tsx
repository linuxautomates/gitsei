import { Row } from "antd";
import widgetConstants from "dashboard/constants/widgetConstants";
import { WidgetType } from "dashboard/helpers/helper";
import React, { useMemo, useCallback, useState, useEffect } from "react";
import DemoWidgetContainer from "./demo-widget-container";
import { SlideDown } from "react-slidedown";
import DemoDrillDownPreview from "./../../../dashboard-drill-down-preview/DemoDrillDownPreview";
import { get, isUndefined, cloneDeep } from "lodash";
import { getTableConfig } from "./helper";
import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import { transformDemoWidgetConfig } from "../helper";
import DemoDashboardNotesPreviewComponent from "../Widgets-Notes/demo-dashboard-notes.component";
import { PR_REVIEW_BY } from "./constant";
import {
  SCM_REPORTS,
  LEAD_TIME_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";
import { convertToDate } from "utils/dateUtils";

interface DemoWidgetListContainerProps {
  demoData: any[];
  widgetType: string;
  dashboardId: string;
}

const DemoWidgetListContainer: React.FC<DemoWidgetListContainerProps> = (props: DemoWidgetListContainerProps) => {
  const { demoData, widgetType, dashboardId } = props;
  const [drilldownIndex, setDrilldownIndex] = useState<number>(-1);
  const [drilldownWidgetId, setDrilldownWidgetId] = useState<string | undefined>(undefined);
  const [drillDownProps, setDrillDownProps] = useState<any | undefined>(undefined);
  const [selectedKey, setSelectedKey] = useState<any | undefined>(undefined);
  const [drilldownTitle, setDrilldownTitle] = useState<string>("");
  const [currentAllocation, setCurrentAllocation] = useState<boolean>(false);
  const [prReviewBy, setPrReviewBy] = useState<Array<string>>([PR_REVIEW_BY.COMMITTER]);
  const [columnsConfig, setColumnsConfig] = useState<Array<any>>([]);
  const [legend, setLegend] = useState<any>({});
  const [dataForDevRawStats, setDataForDevRawStats] = useState<any>({});
  const gutter: any = useMemo(() => [32, 32], []);
  const style: any = useMemo(() => ({ marginTop: "1rem" }), []);

  const demoDatakey: string | undefined = useDemoDashboardDataId(drilldownWidgetId);

  const handleDrillDownPreviewClose = useCallback(() => {
    setDrilldownIndex(-1);
    setDrilldownWidgetId(undefined);
  }, []);

  const handleShowDrillDownPreview = useCallback(
    (param: any) => {
      const { widgetId, name, phaseId, activeData, titlePrefix, columnName, interval, record, legend } = param;
      if (!param) {
        return;
      }

      let index = -1,
        drilldownPropsIdx = -1;
      const widgets = (demoData ?? []).reduce((acc: any, item: any) => {
        acc = [...acc, Object.values(item)[0]];
        return acc;
      }, []);
      const widgetIds = (demoData ?? []).reduce((acc: any, item: any) => {
        acc = [...acc, Object.keys(item)[0]];
        return acc;
      }, []);
      if (widgets.length) {
        let nextWidth = "";
        let col = 0;
        for (let i = 0; i < widgets.length; i++) {
          const cwidgetId = widgetIds[i];

          if (i > 0 && get(widgets[i - 1], ["width"], "half") !== "full") {
            col = col + 1;
          }

          if (i > 0 && get(widgets[i - 1], ["width"], "half") === "full") {
            col = 0;
          }

          if (col === 2) {
            col = 0;
          }

          if (col === 1 && get(widgets[i], ["width"], "half") === "full") {
            col = 0;
          }

          if (cwidgetId === widgetId) {
            index = i;
            drilldownPropsIdx = i;
            nextWidth = widgets[i + 1] ? get(widgets[i + 1], ["width"], "half") : "";
            break;
          }
        }
        const curWidth = get(widgets[index], ["width"], "half");
        if (col === 0 && widgetType !== WidgetType.STATS && curWidth === "half" && nextWidth === "half") {
          index = index + 1;
        }
      }

      const isClickEnabled = get(demoData[drilldownPropsIdx], [widgetId, "isClickEnabled"], true);
      if (isClickEnabled) {
        setCurrentAllocation(activeData);
        setDrilldownIndex(index);
        setDrillDownProps(demoData[drilldownPropsIdx]);
        setDrilldownWidgetId(widgetId);
        setSelectedKey(phaseId);
        setDrilldownTitle(name);
        setPrReviewBy(titlePrefix?.toLowerCase());
        setDataForDevRawStats({ record: record, interval: interval, columnName: columnName, widgetId: widgetId });

        const eleId = `${widgetId}-drilldown`;
        setTimeout(() => {
          const ele = document.getElementById(eleId);
          ele?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "start" });
        }, 700);
      }
    },
    [widgetType, dashboardId, drilldownTitle, selectedKey, demoData, legend]
  );

  const slideDownStyle = useMemo(() => ({ width: "100%" }), []);

  const getColumnConfig = (widgetId: any) => {
    const reportType = demoData[drilldownIndex]?.[drilldownWidgetId as string]?.type;
    let supported_columns = [];
    if (reportType === "individual_raw_stats_report") {
      supported_columns = get(
        drillDownProps,
        [widgetId, "drilldown_data", demoDatakey, dataForDevRawStats?.columnName, "supported_columns"],
        []
      );
    } else {
      supported_columns = get(drillDownProps, [widgetId, "drilldown_data", "supported_columns"], []);
    }
    return getTableConfig(supported_columns);
  };

  useEffect(() => {
    const reportType = demoData[drilldownIndex]?.[drilldownWidgetId as string]?.type;
    let colsConfig = getColumnConfig(drilldownWidgetId);
    if (reportType === "lead_time_by_stage_report") {
      colsConfig.forEach((item: any) => {
        if (item?.titleForCSV.toLowerCase() === drilldownTitle?.trim().toLowerCase())
          item.className = [item.className, "active-stage"].join(" ");
      });
    }
    setColumnsConfig(colsConfig);
  }, [drilldownTitle, dataForDevRawStats, drillDownProps]);

  const getWidgetData = useCallback(() => {
    let demoWidgetData = new Array();

    const reportType = demoData[drilldownIndex]?.[drilldownWidgetId as string]?.type;

    if ([SCM_REPORTS.SCM_REVIEW_COLLABORATION_REPORT].includes(reportType)) {
      demoWidgetData = get(
        drillDownProps[drilldownWidgetId as string]?.drilldown_data,
        [demoDatakey, selectedKey, prReviewBy ?? "", "data"],
        []
      );
    } else if (reportType === "individual_raw_stats_report") {
      demoWidgetData = get(
        drillDownProps[drilldownWidgetId as string]?.drilldown_data,
        [demoDatakey, dataForDevRawStats?.columnName, dataForDevRawStats?.record?.full_name, "tableData"],
        []
      );
    } else if (selectedKey) {
      demoWidgetData = get(
        drillDownProps[drilldownWidgetId as string]?.drilldown_data,
        [demoDatakey, selectedKey, "data"],
        []
      );
    } else {
      demoWidgetData = get(
        drillDownProps[drilldownWidgetId as string]?.drilldown_data,
        [demoDatakey || "", "data"],
        []
      );
    }
    let demoMetaData = cloneDeep(demoWidgetData);
    demoMetaData = demoMetaData?.reduce((acc: any, item: any) => {
      item["created_at"] = convertToDate(item?.created_at || "1662793200");
      item["committed_at"] = convertToDate(item?.committed_at || "1662793200");
      return [...acc, item];
    }, []);
    return demoMetaData;
  }, [drillDownProps, drilldownWidgetId, drilldownIndex, selectedKey, prReviewBy, demoDatakey, dataForDevRawStats]);

  const getGraphData = useCallback(() => {
    let demoGraphData = new Array();
    const reportType = demoData[drilldownIndex]?.[drilldownWidgetId as string]?.type;
    if (reportType === "individual_raw_stats_report") {
      demoGraphData = get(
        drillDownProps[drilldownWidgetId as string]?.drilldown_data,
        [demoDatakey, dataForDevRawStats?.columnName, dataForDevRawStats?.record?.full_name, "graphData"],
        []
      );
    }
    return demoGraphData;
  }, [drillDownProps, drilldownWidgetId, drilldownIndex, selectedKey, prReviewBy, demoDatakey, dataForDevRawStats]);

  const renderPreview = useMemo(() => {
    if (drilldownIndex === -1 || isUndefined(drillDownProps)) {
      return null;
    }

    const query = get(drillDownProps, [drilldownWidgetId as any, "query"], {});
    const reportType = demoData[drilldownIndex]?.[drilldownWidgetId as string]?.type;
    const demoWidgetData = getWidgetData();
    const graphData = getGraphData();
    const additionalDevRawStats =
      reportType === "individual_raw_stats_report"
        ? { devRawStatsAdditionalData: dataForDevRawStats, graphData: graphData }
        : {};
    let demoProps = {
      data: demoWidgetData,
      xAxis: get(query, ["x-axis"], null) ?? prReviewBy,
      currentAllocation: currentAllocation,
      drilldown_count: get(drillDownProps, [drilldownWidgetId as any, "drilldown_data", "drilldown_count"], 0),
      ...additionalDevRawStats
    };

    return (
      <SlideDown style={slideDownStyle}>
        <DemoDrillDownPreview
          drillDownProps={demoProps}
          widgetId={drilldownWidgetId}
          onDrilldownClose={handleDrillDownPreviewClose}
          title={drilldownTitle}
          reportType={reportType}
          columnsConfig={columnsConfig}
        />
      </SlideDown>
    ); // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drilldownIndex, drillDownProps, selectedKey, drilldownTitle, drilldownWidgetId, getColumnConfig, demoDatakey]);

  return (
    <Row gutter={gutter} type={"flex"} justify={"start"} style={style}>
      {(demoData as any).map((data: any, index: number) => {
        const widgetData: any = Object.values(data)[0];
        const id = Object.keys(data)[0];
        const widgetConfig = get(widgetConstants, [widgetData?.type], undefined);
        let widgetSpan = widgetData?.width === "full" ? 24 : 12;
        let minHeight = "350px";
        if (widgetType === WidgetType.STATS) {
          widgetSpan = 6;
          minHeight = "60px";
        }
        if (
          [
            SCM_REPORTS.SCM_REVIEW_COLLABORATION_REPORT,
            LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
            ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
            JIRA_MANAGEMENT_TICKET_REPORT.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
            ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT
          ].includes(widgetData?.type)
        ) {
          widgetSpan = 24;
        }
        return (
          <React.Fragment key={data.id}>
            {widgetData.type === "dashboard_notes" && (
              <DemoDashboardNotesPreviewComponent widgetId={widgetData} dashboardId={dashboardId} />
            )}
            {widgetData.type !== "dashboard_notes" && (
              <DemoWidgetContainer
                widgetType={widgetType}
                minHeight={minHeight}
                id={id}
                widgetSpan={widgetSpan}
                drilldownSelected={drilldownWidgetId === id}
                widgetConfig={transformDemoWidgetConfig(widgetConfig, widgetData)}
                dashboardId={dashboardId}
                handleShowDrillDownPreview={handleShowDrillDownPreview}
              />
            )}
            {index === drilldownIndex && renderPreview}
          </React.Fragment>
        );
      })}
    </Row>
  );
};

export default DemoWidgetListContainer;
