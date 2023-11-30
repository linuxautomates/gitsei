import { Button, Divider, Menu } from "antd";
import React from "react";
import { TREND_LINE_DIFFERENTIATER } from "shared-resources/charts/constant";
import { transformDataKey } from "shared-resources/charts/helper";
import { AntCheckbox, AntIcon, AntText } from "shared-resources/components";
import { FilterActionType } from "./chart-legend.types";
import { LegendPayload } from "recharts";

interface LegendMenuFilterItemsProps {
  handleFilterChange: (key: string, value: boolean, action?: FilterActionType) => void;
  legendFormatter: (value: any) => any;
  dropdownFilterKeys: LegendPayload[];
  allowLabelTransform?: boolean;
  readOnlyLegend?: boolean;
  filters: any;
  [key: string]: any;
}

export const LegendMenuFilterItems = React.memo(
  ({
    handleFilterChange,
    legendFormatter,
    allowLabelTransform,
    dropdownFilterKeys,
    filters,
    readOnlyLegend,
    ...restProps
  }: LegendMenuFilterItemsProps) => {
    return (
      <>
        <li className="filter-options-container">
          <Button
            type="link"
            className="select-all-btn"
            onClick={() => handleFilterChange("", true, FilterActionType.SELECT_ALL)}>
            Select All
          </Button>
          <Button
            type="link"
            className="clear-btn"
            onClick={() => handleFilterChange("", true, FilterActionType.CLEAR)}>
            Clear
          </Button>
        </li>
        <Divider className="filter-divider" />
        <li className="ant-dropdown-filter-menu">
          {dropdownFilterKeys
            .filter((item: any) => (typeof item.dataKey === "function" ? item.payload?.barKey || "" : item.dataKey))
            .map((entry: any) => {
              const { dataKey, color } = entry;
              if (dataKey.includes(TREND_LINE_DIFFERENTIATER)) {
                return (
                  <span className="mr-20 ml-10">
                    <AntIcon type="dash" className="mr-5 ml-5" style={{ color, fontSize: "1rem" }} />
                    {`${legendFormatter(transformDataKey(dataKey))} Line`}
                  </span>
                );
              }
              if (readOnlyLegend) {
                return (
                  <div className="indicator-item indicator-dropdown">
                    <div className="indicator-color" style={{ backgroundColor: color }} />
                    <AntText className="indicator-title">{dataKey}</AntText>
                  </div>
                );
              }
              return (
                <Menu.Item
                  {...restProps}
                  key={`filter-${dataKey}`}
                  onClick={({ domEvent }) => domEvent.stopPropagation()}>
                  <AntCheckbox
                    className={"legend-checkbox"}
                    style={{
                      textTransform: allowLabelTransform === false ? "" : "capitalize",
                      "--tick-color": color
                    }}
                    indeterminate={filters[dataKey]}
                    checked={filters[dataKey]}
                    onChange={(input: any) => handleFilterChange(dataKey, input.target.checked)}>
                    {legendFormatter(transformDataKey(dataKey))}
                  </AntCheckbox>
                </Menu.Item>
              );
            })}
        </li>
      </>
    );
  }
);
