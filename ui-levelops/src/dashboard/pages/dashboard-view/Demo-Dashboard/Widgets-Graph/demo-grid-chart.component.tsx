import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ResponsiveContainer, Treemap } from "recharts";
import { GridViewContent } from "shared-resources/charts/components/GridViewContent";
import { BreadCrumbFilters } from "shared-resources/charts/grid-view-chart/breadcrumb-filters";
import { colorTint } from "shared-resources/charts/grid-view-chart/util";
import { AntButton, EmptyWidget } from "shared-resources/components";
import { DemoGridViewChartProps } from "./Widget-Grapg-Types/demo-grid-view-chart.types";

const DemoGridViewChart: React.FC<DemoGridViewChartProps> = (props: DemoGridViewChartProps) => {
  const { data, id, dataKey, total } = props;
  const isDirectorySelectionEnabled = id?.endsWith("preview");
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

  const reportBtn = useMemo(() => {
    let directory = "Root";

    if (directory.length > 30) {
      directory = `.../${directory.split("/").pop()}`;
    }

    return <AntButton className="report-btn">{`Open Report for '${directory}'`}</AntButton>;
  }, []);

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
      return <GridViewContent total={total} {...props} color={mappedValue} dataKey={dataKey} previewOnly={true} />;
    },
    [colorToWidthMapping, baseColor, id]
  );

  if ((data || []).length === 0) {
    return (
      <div className="grid-view-chart">
        <div className="report-btn-container">
          <BreadCrumbFilters disabled={!isDirectorySelectionEnabled} />
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
        <BreadCrumbFilters disabled={!isDirectorySelectionEnabled} />
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

export default DemoGridViewChart;
