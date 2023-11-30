import React from "react";
import { get, isNumber } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { toTitleCase } from "utils/stringUtils";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

import { TooltipPayload, TooltipProps } from "recharts";
import { findSumInArray } from "utils/commonUtils";
import { sortTooltipListItems, formatTooltipValue } from "../helper";

// @ts-ignore
export const renderTooltip = (tooltipProps: TooltipProps, props, chart_type) => {
  let { active, payload, label } = tooltipProps;
  if (props?.trendLineKey) {
    payload = payload?.filter(data => data?.dataKey !== props?.trendLineKey);
  }
  if (props?.showNegativeValuesAsPositive && payload) {
    payload = payload.map((p: any) => ({ ...p, value: Math.abs(p.value) }));
  }
  const { tooltipTitle, useOrderedStacks, stacked, barPropsMap, hideTotalInTooltip } = props;
  let toolTipLabel =
    chart_type === ChartType.CIRCLE ? get(payload, [0, "name"]) : get(payload, [0, "payload", "toolTip"], label);
  toolTipLabel = tooltipTitle ? toTitleCase(`${tooltipTitle} of ${toolTipLabel}`) : toolTipLabel;
  const getValue = (data: any) => {
    let _value = props.transformFn ? props.transformFn(data.value, data?.dataKey) : data.value;
    _value = props.percentIncludeFn ? props.percentIncludeFn(data, props.all_data) : _value;
    return formatTooltipValue(_value);
  };

  if (props?.useCustomToolTipHeader && typeof props?.useCustomToolTipHeader === "function") {
    toolTipLabel = props.useCustomToolTipHeader(props.data, label);
  }
  const extraInfo = get(widgetConstants, [props.reportType || "", "showExtraInfoOnToolTip"], []);

  if (!!payload && payload.length > 0) {
    let listitems = payload
      .map((item: TooltipPayload, i: number) => {
        const itemName =
          chart_type === ChartType.CIRCLE
            ? item.dataKey
            : typeof item.name === "function"
            ? // @ts-ignore
              item.name(item.payload)
            : props.hygieneMapping
            ? get(props.hygieneMapping, item.name, item.name)
            : item.name;
        const itemFill =
          chart_type === ChartType.CIRCLE
            ? item.payload.fill
            : stacked && useOrderedStacks && typeof item.name === "function"
            ? // @ts-ignore
              barPropsMap?.[item.name(item.payload)]?.fill || item.fill
            : item.fill;
        const itemValue = getValue(item);

        if ((!itemValue && !itemName && typeof itemName === "function") || itemName === props?.trendLineKey) {
          return {};
        }

        return {
          label: toTitleCase(itemName),
          value: itemValue,
          color: itemFill
        };
      })
      .filter(item => !!Object.keys(item).length);

    if (extraInfo.length) {
      extraInfo.forEach((inf: string, index: number) => {
        const value = payload?.[0]?.payload?.[inf] || "";
        listitems.push({ label: toTitleCase(inf), value, color: "#000" });
      });
    }

    const hasStcks = get(widgetConstants, [props.reportType, "stack_filters"], []);
    if (!!props.stacked && chart_type === ChartType.BAR && !hideTotalInTooltip && hasStcks.length === 0) {
      let totalcount = findSumInArray(payload as any[], "value");
      if (props.totalCountTransformFn) {
        totalcount = props.totalCountTransformFn(totalcount, {
          all_data: props.all_data,
          payload: payload[0]?.payload
        });
      }
      if (isNumber(totalcount)) {
        totalcount = parseFloat(totalcount.toFixed(2));
      }
      let totalLabel = "Total";
      const getTotalLabel = get(widgetConstants, [props.reportType, "getTotalLabel"], undefined);
      if (getTotalLabel) {
        totalLabel = getTotalLabel(props);
      }
      listitems.push({ label: totalLabel, value: totalcount, color: "#000" });
    }

    let newItems = sortTooltipListItems(listitems);

    if (active) {
      return (
        <div className="custom-tooltip">
          <p>{toolTipLabel}</p>
          <ul>{newItems}</ul>
        </div>
      );
    }
  }
  return null;
};
