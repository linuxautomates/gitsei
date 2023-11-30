import React, { useEffect, useMemo, useState } from "react";
import { renderTooltip } from "./bar-chart.tooltip";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { TooltipProps } from "recharts";
import GenericTooltipRenderer from "../components/GenericTooltipRenderer";

interface BarChartTooltipWrapperProps {
  tooltipProps: TooltipProps;
  props: any;
  setIsHoverOnTooltip: any;
  isHoverOnTooltip: boolean;
  tooltipRenderTransform: any;
}

export const BarChartTooltipWrapper = ({
  tooltipProps,
  props,
  isHoverOnTooltip,
  setIsHoverOnTooltip,
  tooltipRenderTransform
}: BarChartTooltipWrapperProps) => {
  const [staleTooltipProps, setstaleTooltipProps] = useState<TooltipProps>(tooltipProps);

  useEffect(() => {
    if (!isHoverOnTooltip) {
      setstaleTooltipProps(tooltipProps);
    }
  }, [isHoverOnTooltip, tooltipProps]);

  const tooltipRenderer = useMemo(() => {
    return tooltipRenderTransform ? (
      <GenericTooltipRenderer
        tooltipProps={staleTooltipProps}
        chartProps={props}
        extraProps={{ chartType: ChartType.BAR }}
      />
    ) : (
      renderTooltip(staleTooltipProps, props, ChartType.BAR)
    );
  }, [staleTooltipProps, tooltipRenderTransform]);

  return (
    <div onMouseEnter={() => setIsHoverOnTooltip(true)} onMouseLeave={() => setIsHoverOnTooltip(false)}>
      {tooltipRenderer}
    </div>
  );
};
