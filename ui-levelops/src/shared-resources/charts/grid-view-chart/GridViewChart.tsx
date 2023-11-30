import React, { useCallback, useEffect, useMemo, useState } from "react";
import { GridViewChartProps } from "../chart-types";
import { AntButton, EmptyWidget } from "../../components";
import "./GridViewChart.style.scss";
import { BreadCrumbFilters } from "./breadcrumb-filters";
import { colorTint } from "./util";
import { FileReports } from "../../../dashboard/constants/helper";
import { ResponsiveContainer, Treemap } from "recharts";
import { GridViewContent } from "../components/GridViewContent";

const GridViewChart: React.FC<GridViewChartProps> = (props: GridViewChartProps) => {
  const { data, dataKey, id, onClick, hasClickEvents, defaultPath, reportType, total, previewOnly } = props;
  const isDirectorySelectionEnabled = id.endsWith("preview") || hasClickEvents;
  const filterKey = reportType === FileReports.SCM_JIRA_FILES_REPORT ? "scm_module" : "module";

  const [breadcrumbHeight, setBreadcrumbHeight] = useState(0);
  const [containerHeight, setContainerHeight] = useState(0);
  const ref = React.createRef<HTMLDivElement>();
  const containerRef = React.createRef<HTMLDivElement>();

  let colorToWidthMapping: any = {};
  let baseColor = "#ff6b6a";

  const sortedData = data.sort((a: any, b: any) => b.count - a.count);

  useEffect(() => {
    if (ref && ref.current) {
      setBreadcrumbHeight(ref.current.clientHeight);
    }
  }, [ref]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (containerRef && containerRef.current) {
      setContainerHeight(containerRef.current.clientHeight);
    }
  }, [containerRef]); // eslint-disable-line react-hooks/exhaustive-deps

  const getHeight = useMemo(() => {
    const height = containerHeight - breadcrumbHeight - 8;
    return height > 0 ? height : 0;
  }, [containerHeight, breadcrumbHeight]);

  const handleDirectoryChange = useCallback(
    (path: string) => {
      onClick &&
        onClick({
          type: "change_directory",
          [filterKey]: path
        });
    },
    [onClick, reportType]
  );

  const localFilterClicked = useCallback(
    (dir: any) => {
      isDirectorySelectionEnabled &&
        onClick &&
        onClick({
          type: "change_directory",
          [filterKey]: defaultPath ? `${defaultPath}/${dir.value}` : dir.value,
          repo_id: dir.repo_id
        });
    },
    [id, defaultPath, hasClickEvents, onClick, reportType]
  );

  const onOpenReportClicked = useCallback(() => {
    onClick &&
      onClick({
        type: "open_report",
        [filterKey]: defaultPath
      });
  }, [onClick, defaultPath, reportType]);

  const reportBtn = useMemo(() => {
    let directory = defaultPath || "Root";

    if (directory.length > 30) {
      directory = `.../${directory.split("/").pop()}`;
    }

    if (!hasClickEvents) {
      return null;
    }

    return (
      <AntButton className="report-btn" onClick={onOpenReportClicked}>
        {`Open Report for '${directory}'`}
      </AntButton>
    );
  }, [defaultPath, hasClickEvents, onClick, reportType]);

  const renderTreeMapChild = useCallback(
    (props: any) => {
      let mappedValue = colorToWidthMapping[props.value] || undefined;
      if (!mappedValue) {
        colorToWidthMapping = {
          ...colorToWidthMapping,
          [props.value]: baseColor
        };
        const percent = total && props.value ? (20 * (total - props.value)) / total : 0;
        mappedValue = baseColor;
        baseColor = colorTint(baseColor, percent);
      }
      return (
        <GridViewContent
          total={total}
          {...props}
          color={mappedValue}
          dataKey={dataKey}
          onClick={localFilterClicked}
          previewOnly={previewOnly}
        />
      );
    },
    [colorToWidthMapping, baseColor, id, defaultPath, hasClickEvents, onClick, reportType]
  );

  if ((data || []).length === 0) {
    return (
      <div className="grid-view-chart">
        <div className="report-btn-container">
          <BreadCrumbFilters
            defaultPath={defaultPath}
            onClick={handleDirectoryChange}
            disabled={!isDirectorySelectionEnabled}
          />
        </div>
        <div className="empty-placeholder">
          <EmptyWidget />
        </div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="grid-view-chart">
      <div ref={ref} className="report-btn-container">
        <BreadCrumbFilters
          defaultPath={defaultPath}
          onClick={handleDirectoryChange}
          disabled={!isDirectorySelectionEnabled}
        />
        {reportBtn}
      </div>
      <ResponsiveContainer minWidth="0" height={getHeight}>
        <Treemap
          data={sortedData}
          dataKey={dataKey}
          isAnimationActive={false}
          stroke={"#fff"}
          fill="#fff"
          content={renderTreeMapChild}
        />
      </ResponsiveContainer>
    </div>
  );
};

export default GridViewChart;
