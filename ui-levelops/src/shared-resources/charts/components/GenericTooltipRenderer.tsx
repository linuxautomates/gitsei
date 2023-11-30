import {
  ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM,
  ALLOW_ZERO_LABELS,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM
} from "dashboard/constants/applications/names";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { TOTAL_NUMBER_OF_TICKETS, TOTAL_SUM_OF_STORY_POINTS } from "dashboard/reports/azure/issues-report/constant";
import { get, isNumber } from "lodash";
import React, { useMemo } from "react";
import { TooltipPayload, TooltipProps } from "recharts";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { EntityIdentifier } from "types/entityIdentifier";
import { findSumInArray } from "utils/commonUtils";
import { toTitleCase } from "utils/stringUtils";
import "../bar-chart/bar-chart.style.scss";
import { sortTooltipListItems } from "../helper";

export type TooltipListItemType = {
  label: string;
  value: EntityIdentifier;
  color: string;
};

interface GenericTooltipRendererProps {
  tooltipProps: TooltipProps;
  chartProps: any; //* add more chart props type here
  extraProps?: {
    tooltipLabelKey?: EntityIdentifier[];
    chartType?: ChartType;
  };
}

const GenericTooltipRenderer: React.FC<GenericTooltipRendererProps> = (props: GenericTooltipRendererProps) => {
  const { tooltipProps, chartProps, extraProps } = props;
  let { active, payload, label } = tooltipProps;
  const {
    reportType,
    showNegativeValuesAsPositive,
    tooltipTitle,
    stacked,
    hideTotalInTooltip,
    transformFn,
    totalCountTransformFn,
    digitsAfterDecimal
  } = chartProps;
  if (showNegativeValuesAsPositive && payload) {
    payload = payload.map((p: any) => ({ ...p, value: Math.abs(p.value) }));
  }

  let toolTipLabel = label;

  if (extraProps?.tooltipLabelKey) {
    toolTipLabel = get(payload, extraProps.tooltipLabelKey) ?? label;
  }

  toolTipLabel = tooltipTitle ? toTitleCase(`${tooltipTitle} of ${toolTipLabel}`) : toolTipLabel;

  const getValue = (data: any) => {
    if (transformFn) {
      return transformFn(data);
    }
    return data;
  };

  const getListItems = (): TooltipListItemType[] => {
    if ((payload || []).length > 0) {
      let items = (payload || []).map((item: TooltipPayload, i: number) => {
        return {
          label: item.name,
          value: getValue(item.value),
          color: item.fill
        };
      });

      if (!!stacked && !hideTotalInTooltip) {
        if (payload?.length && typeof payload[0].value === "string") {
          payload = payload.map(record => ({
            ...(record || {}),
            value: (record?.value as string).includes(".")
              ? parseFloat(record?.value as string)
              : parseInt(record?.value as string)
          }));
        }
        let totalcount = findSumInArray(payload as any[], "value");

        if (totalCountTransformFn) {
          totalcount = totalCountTransformFn(totalcount);
        }

        if (isNumber(totalcount)) {
          totalcount = parseFloat(totalcount.toFixed(2));
        }

        if (digitsAfterDecimal && typeof totalcount === "string" && totalCountTransformFn) {
          totalcount = totalCountTransformFn(parseFloat(totalcount).toFixed(digitsAfterDecimal));
        }

        items.push({ label: "Total", value: totalcount, color: "#000" });
      }

      // Removing zero labels
      const allowZeroLabels = getWidgetConstant(reportType, [CHART_DATA_TRANSFORMERS, ALLOW_ZERO_LABELS]);
      if (allowZeroLabels === false) {
        items = items.filter(item => item.value !== "0.0%");
        if (!(items || []).length) {
          items = [{ label: "No Data", value: "", color: "#000" }];
        }
      }

      return items as TooltipListItemType[];
    }
    return [];
  };

  const tooltipRenderTransform = getWidgetConstant(reportType, [
    CHART_DATA_TRANSFORMERS,
    CHART_TOOLTIP_RENDER_TRANSFORM
  ]);

  const allowLabelTransform = getWidgetConstant(reportType, [
    CHART_DATA_TRANSFORMERS,
    ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM
  ]);

  const tooltipRenderData = useMemo(
    () => tooltipRenderTransform?.(payload, toolTipLabel, { chartType: props.extraProps?.chartType, chartProps }),
    [props, tooltipRenderTransform]
  );

  const renderListItems = () => {
    let items = tooltipRenderData?.list ?? getListItems() ?? [];
    const hasStcks = getWidgetConstant(reportType, ["stack_filters"], []);
    if (hasStcks.length !== 0) {
      items = items.filter((item: Record<string, string>) => {
        return ![TOTAL_SUM_OF_STORY_POINTS, TOTAL_NUMBER_OF_TICKETS].some((label: string) => {
          return label?.toLocaleLowerCase() === item?.label?.toLocaleLowerCase();
        });
      });
    }
    return sortTooltipListItems(items, !(allowLabelTransform === false));
  };

  const renderTooltip = () => {
    const header = tooltipRenderData?.header ?? toolTipLabel ?? null;
    return (
      <div className="custom-tooltip">
        <p>{header}</p>
        <ul>{renderListItems()}</ul>
      </div>
    );
  };

  return active ? renderTooltip() : null;
};

export default GenericTooltipRenderer;
