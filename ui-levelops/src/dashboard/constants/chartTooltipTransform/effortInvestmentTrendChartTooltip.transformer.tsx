import React from "react";
import { TooltipListItemType } from "shared-resources/charts/components/GenericTooltipRenderer";
import { toTitleCase } from "utils/stringUtils";

export const effortInvestmentTrendChartTooltipTransformer = (
  payload: { [x: string]: any },
  currentLabel: string
): { header: React.ReactNode; list?: TooltipListItemType[] } => {
  const selectedInterval = payload?.[0]?.payload?.selected_interval ?? "";
  if (selectedInterval.length) {
    return {
      header: (
        <div className={"flex direction-column"}>
          <span>{currentLabel || ""}</span>
          <span>{`Interval: ${toTitleCase(selectedInterval.replace(/_/g, " "))}`}</span>
        </div>
      )
    };
  }
  return { header: currentLabel };
};
