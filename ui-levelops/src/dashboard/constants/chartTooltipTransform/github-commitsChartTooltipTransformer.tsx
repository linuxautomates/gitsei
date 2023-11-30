import React from "react";
import { TooltipPayload } from "recharts";
import { TooltipListItemType } from "shared-resources/charts/components/GenericTooltipRenderer";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { githubCommitsmetricsChartMapping } from "../FE-BASED/github-commit.FEbased";
import { ScmCommitsMetricTypes } from "../typeConstants";

export const githubCommitsChartTooltipTransformer = (
  payload: { [x: string]: any },
  currentLabel: string,
  extra?: { chartType: ChartType }
): { header: React.ReactNode; list?: TooltipListItemType[] } => {
  let items: TooltipListItemType[] = [];
  let label: string = currentLabel;
  if (extra?.chartType === ChartType.CIRCLE) {
    const item = payload?.[0] || {};
    label = item?.name;
    items = [
      {
        label: githubCommitsmetricsChartMapping[item?.dataKey as ScmCommitsMetricTypes],
        color: item?.payload?.fill,
        value: item?.value
      }
    ];
  } else {
    items = (payload || []).map((item: TooltipPayload, i: number) => {
      return {
        label: githubCommitsmetricsChartMapping[item.name as ScmCommitsMetricTypes],
        value: item.value,
        color: item.fill
      };
    });
  }

  return {
    header: (
      <div className={"flex direction-column"}>
        <span>{label || ""}</span>
      </div>
    ),
    list: items
  };
};
