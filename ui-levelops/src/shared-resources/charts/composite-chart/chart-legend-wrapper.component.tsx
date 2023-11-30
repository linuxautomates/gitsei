import React from "react";
import { LegendProps } from "recharts";
import { transformKey } from "shared-resources/charts/helper";
import ChartLegendComponent from "shared-resources/charts/components/chart-legend/chart-legend.component";

interface ChartLegendWrapperProps extends LegendProps {
  filters: any;
  allowLabelTransform?: boolean;
  setFilters: (filters: any) => void;
  labelMapping?: { [x: string]: string };
}

const ChartLegendComponentWrapper: React.FC<any> = (props: ChartLegendWrapperProps) => {
  const { payload, filters, setFilters, width, allowLabelTransform, labelMapping } = props;
  console.log("ChartLegendComponentWrapper payload", payload);
  let newPayload: any = payload?.reduce((acc: any, item: any) => {
    if (
      item?.payload?.dataKey &&
      item?.payload?.dataKey?.includes("^__") &&
      item?.payload?.dataKey?.split("^__")?.length === 3
    ) {
      item.payload.dataKey = transformKey(item?.payload?.dataKey);
    }
    acc = [...acc, item?.payload];
    return acc;
  }, []);
  let payloadData = [newPayload];
  console.log("ChartLegendComponentWrapper newPayload", newPayload);
  return <ChartLegendComponent filters={filters} setFilters={setFilters} payload={payloadData} />;
};

export default ChartLegendComponentWrapper;
