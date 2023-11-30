import { FILTER_TYPE } from "constants/filters";
import { forEach, map, uniqBy, find } from "lodash";
import { runsLogsTableConfig } from "workflow/components/runs-logs/node-detail.config";
import React from "react";

export enum runslogFilterType {
  NODE_ID = "node_id",
  STATE = "state"
}

export enum runsLogStatusType {
  FAILURE = "failure",
  RUNNING = "running",
  WAITING = "waiting",
  SUCCESS = "success"
}

const runsLogStatusOptions = [
  { value: runsLogStatusType.FAILURE, label: runsLogStatusType.FAILURE.toUpperCase() },
  { value: runsLogStatusType.RUNNING, label: runsLogStatusType.RUNNING.toUpperCase() },
  { value: runsLogStatusType.SUCCESS, label: runsLogStatusType.SUCCESS.toUpperCase() },
  { value: runsLogStatusType.WAITING, label: runsLogStatusType.WAITING.toUpperCase() }
];

const getRunslogFilterSelectOptions = (data: any, key: string, getNodeName: any) => {
  const options: any = [];
  forEach(data, (record: any) => {
    options.push({
      value: record?.[key],
      label: key === runslogFilterType.NODE_ID ? getNodeName(record?.[key]) || "" : (record?.[key] || "").toUpperCase()
    });
  });
  return uniqBy(options, "value");
};

export const runsLogMappedColumns = (nodes: any) =>
  map(runsLogsTableConfig, (column: any) => {
    if (column.key === "node_name") {
      return {
        ...(column || {}),
        filterLabel: "Node",
        filterType: FILTER_TYPE.MULTI_SELECT,
        filterField: "node_ids",
        options: uniqBy(
          map(Object.keys(nodes || {}) || [], node => ({ label: nodes[node].name, value: nodes[node].id })),
          "value"
        ),
        render: (item: any, record: any, index: number) => {
          return (
            <div>
              <p style={{ margin: "0" }}>
                {nodes[find(Object.keys(nodes || {}) || [], node => nodes[node].id === record.node_id) || ""]?.name}
              </p>
            </div>
          );
        }
      };
    }
    if (column.key === "state") {
      return {
        ...(column || {}),
        options: runsLogStatusOptions,
        filterLabel: "Status",
        filterType: FILTER_TYPE.MULTI_SELECT,
        filterField: "states"
      };
    }
    return column;
  });
