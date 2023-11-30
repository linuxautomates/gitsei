import React from "react";
import { Tooltip } from "antd";
import { AntTag, AntTooltip, NameAvatar, AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { statusColumn, timeColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { DemoSupportedColumn, DrilldownShowValueIn } from "../Types/drilldown.types";
import { EnggCategoryColorMapping } from "./constant";
import { tableCell } from "utils/tableUtils";
import { totalLeadTimeColumn } from "dashboard/pages/dashboard-tickets/configs/leadTimeTableConfig";
import { renderVelocityStageDynamicColumns } from "custom-hooks/helpers/leadTime.helper";

export const getTableConfig = (supportedColumns: DemoSupportedColumn[]) => {
  return supportedColumns.map((column: DemoSupportedColumn) => {
    switch (column.show_value_in) {
      case DrilldownShowValueIn.TAGS:
        return {
          ...baseColumnConfig(column.name, column.key, { width: "20%" }),
          render: (value: any) => {
            const result = Array.isArray(value) ? (
              value.map((val, index) =>
                val ? (
                  <AntTag color={column.color ?? ""} key={index}>
                    {val}
                  </AntTag>
                ) : (
                  ""
                )
              )
            ) : value ? (
              (typeof value === "object" && typeof value !== null && typeof value !== "string") ||
              (Object.keys(value)?.length > 0 && typeof value !== "string") ? (
                value.hasOwnProperty("className") ? (
                  <div>
                    <AntText className={value.className}>{value?.value}</AntText>
                  </div>
                ) : (
                  <div>
                    <div className={`stage-circle stage-circle-${value?.rating}`}></div>
                    <AntText>{value?.value}</AntText>
                  </div>
                )
              ) : (
                <AntTag color={column?.color ?? ""} key={value}>
                  {value}
                </AntTag>
              )
            ) : (
              <AntTag color={column?.color ?? ""} key={value}>
                {value}
              </AntTag>
            );
            return (
              <div className={"ellipsis"}>
                <AntTooltip
                  title={
                    Array.isArray(value)
                      ? value.join(",")
                      : (typeof value === "object" && value !== null) ||
                        (value !== undefined && Object.keys(value)?.length > 0)
                      ? value?.value === "-"
                        ? "Stage is not applicable to issue or PR"
                        : ""
                      : value
                  }>
                  {result}
                </AntTooltip>
              </div>
            );
          }
        };
      case DrilldownShowValueIn.LINK:
        return {
          ...baseColumnConfig(column.name, column.key, column.width ? { width: `${column.width}` } : { width: "20%" }),
          render: (value: any) => {
            return (
              <div className="jira-issue-row">
                <a rel="noopener noreferrer">{value}</a>
              </div>
            );
          }
        };
      case DrilldownShowValueIn.JOB_STATUS:
        return {
          ...statusColumn(column.name, column.key, { width: "20%" })
        };
      case DrilldownShowValueIn.DURATION:
        return {
          ...baseColumnConfig(column.name, column.key, { width: "20%" }),
          render: (value: any, record: any, index: any) => {
            const duration = record?.duration || 0;
            if (duration <= 0) return 0;
            return (duration / 60).toFixed(2);
          }
        };
      case DrilldownShowValueIn.AVATAR:
        return {
          ...baseColumnConfig(column.name, column.key, { width: "20%" }),
          render: (value: string[], record: any) => {
            value = (value || []).filter((item: string) => item !== "NONE");
            const sliceAbleLength = Math.min(value.length, 3);
            const firstThree = value.slice(0, sliceAbleLength);
            const leftOutTickets = value.slice(sliceAbleLength) || [];
            return (
              <div className="flex">
                <div className="flex" style={{ height: "100%" }}>
                  {(firstThree || []).map(assignee => {
                    return (
                      <div style={{ paddingRight: "10px" }}>
                        <NameAvatar name={assignee} />
                      </div>
                    );
                  })}
                </div>
                {leftOutTickets.length > 0 && (
                  <div className="flex align-center justify-center pl-9">
                    <Tooltip placement="topLeft" title={leftOutTickets.join(", ")}>
                      <p
                        style={{
                          color: "var(--link-and-actions)",
                          marginBottom: "0"
                        }}>{`+ ${leftOutTickets.length}`}</p>
                    </Tooltip>
                  </div>
                )}
              </div>
            );
          }
        };
      case DrilldownShowValueIn.AVATARS_STRING:
        return {
          ...baseColumnConfig(column.name, column.key, column.width ? { width: `${column.width}` } : { width: "20%" }),
          render: (value: any) => {
            return (
              <div>
                <AntText>
                  <div>
                    <span className="avatar-reviewer">
                      <NameAvatar name={value} />
                    </span>
                    <span className={"ellipsis"} style={{ width: `${column?.width}` }}>
                      {value}
                    </span>
                  </div>
                </AntText>
              </div>
            );
          }
        };
      case DrilldownShowValueIn.COMPONENT:
        return {
          ...baseColumnConfig(column.name, column.key, {}),
          render: (value: any, record: any, index: number) =>
            tableCell("tags_withColor", value, undefined, { record_index: index })
        };
      case DrilldownShowValueIn.TAGS_BGCOLOR:
        return {
          ...baseColumnConfig(column.name, column.key, {}),
          render: (value: any, record: any, index: number) => {
            console.log("record", record);
            type ObjectKey = keyof typeof EnggCategoryColorMapping;
            return (
              <AntTag
                color={
                  record?.category_color
                    ? record?.category_color
                    : (EnggCategoryColorMapping[value as ObjectKey] as any)
                }
                key={value}>
                {value}
              </AntTag>
            );
          }
        };
      case DrilldownShowValueIn.ASSIGNEE_NAME:
        return {
          ...baseColumnConfig(column.name, column.key, {}),
          render: (value: any) => tableCell("user", value)
        };
      case DrilldownShowValueIn.TIME:
        return {
          ...timeColumn(column.name, column.key)
        }
      case DrilldownShowValueIn.LEAD_TOTAL:
        return {
          ...totalLeadTimeColumn
        }
      case DrilldownShowValueIn.STAGE_TIME:
      return {
        ...renderVelocityStageDynamicColumns(column.key) //stageTimeColumn(column.name, column.key, "seconds", { sorter: true } )
      }
      default: {
        return {
          ...baseColumnConfig(column.name, column.key, column.width ? { width: `${column.width}` } : { width: "20%" }),
          render: (value: any) => {
            const result = Array.isArray(value) ? value.join(", ") : value;
            return (
              <div className={"ellipsis"} style={{ width: `${column?.width}` }}>
                {result}
              </div>
            );
          }
        };
      }
    }
  });
};
