import React, { useContext, useState } from "react";
import { CustomizedTreeMapContent } from "../components/CustomizedTreeMapContent";
import { ResponsiveContainer, Treemap } from "recharts";
import { TreeMapChartProps } from "../chart-types";
import { FilterBreadcrumbs } from "../chart-filter-breadcrumb";
import { WidgetFilterContext } from "../../../dashboard/pages/context";
import { EmptyWidget } from "../../components";

const TreeMapChart: React.FC<TreeMapChartProps> = (props: TreeMapChartProps) => {
  const { filters, setFilters } = useContext(WidgetFilterContext);
  const [breadcrumbHeight, setBreadcrumbHeight] = useState(0);

  const { data, dataKey, hasClickEvents, onClick, total, reportType, previewOnly } = props;

  const onTreeMapClick = (x: any) => {
    hasClickEvents && onClick && onClick(x);
  };

  const widgetFilters = (filters as any)[props.id];
  const localFilters = widgetFilters && widgetFilters.localFilters;

  let localExtra;
  if (localFilters) {
    localExtra = (
      <FilterBreadcrumbs
        setHeight={setBreadcrumbHeight}
        widgetId={props.id}
        filters={localFilters}
        setFilters={setFilters}
      />
    );
  }

  const showBreadcrumbs = localFilters && localFilters.parents && localFilters.parents.length > 0;

  const getHeight = () => {
    switch (breadcrumbHeight) {
      case 22:
        return "93.5%";
      case 44:
        return "87%";
      case 66:
        return "81.5%";
      default:
        return "74%";
    }
  };
  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }
  return (
    <>
      {showBreadcrumbs && localExtra}
      <ResponsiveContainer height={showBreadcrumbs ? getHeight() : "100%"}>
        <Treemap
          data={data}
          dataKey={dataKey}
          isAnimationActive={false}
          stroke={"#fff"}
          fill="#fff"
          content={props => {
            return (
              <CustomizedTreeMapContent
                total={total}
                {...props}
                dataKey={dataKey}
                onClick={onTreeMapClick}
                reportType={reportType}
                previewOnly={previewOnly}
              />
            );
          }}
        />
      </ResponsiveContainer>
    </>
  );
};

export default TreeMapChart;
