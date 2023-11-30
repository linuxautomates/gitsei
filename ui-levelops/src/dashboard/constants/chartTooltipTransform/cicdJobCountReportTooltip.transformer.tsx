import { isNumber } from "lodash";
import React from "react";
import { TooltipPayload } from "recharts";
import { TooltipListItemType } from "shared-resources/charts/components/GenericTooltipRenderer";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { findSumInArray } from "utils/commonUtils";

export const cicdJobCountReportTooltipTransformer = (
  payload: { [x: string]: any },
  currentLabel: string,
  extra?: { chartType: ChartType; chartProps: { stacks?: Array<string> } }
): { header: React.ReactNode; list?: TooltipListItemType[] } => {
  const items: TooltipListItemType[] = (payload || []).map((item: TooltipPayload, i: number) => {
    return {
      label: item.name,
      value: item.value,
      color: item.fill
    };
  });

  if (!extra?.chartProps?.stacks?.includes("triage_rule")) {
    let totalcount = findSumInArray(payload as any[], "value");
    if (isNumber(totalcount)) {
      totalcount = parseFloat(totalcount.toFixed(2));
    }
    items.push({ label: "Total", value: totalcount, color: "#000" });
  }

  return {
    header: (
      <div className={"flex direction-column"}>
        <span>{currentLabel || ""}</span>
      </div>
    ),
    list: items
  };
};
