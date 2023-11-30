import { Tooltip } from "antd";
import { forEach, get } from "lodash";
import React from "react";
import { AntText, AntTooltip } from "shared-resources/components";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import { baseColumnConfig } from "utils/base-table-config";
import { truncateAndEllipsis } from "utils/stringUtils";
import { priorityMappping } from "./helper";
import JiraPriorityColumnComponent from "./jiraPriorityColumnComponent";

export const jiraPriorityChartTableConfig = (apiData: any[]) => {
  const sprints = apiData?.length > 0 ? get(apiData[0], ["sprints"], []) : [];
  let priorityChartTableColumnConfig: any[] = [
    {
      dataIndex: "name",
      width: "9%",
      render: (item: any, record: any) => (
        <div
          style={{
            height: "3rem",
            display: "flex",
            alignItems: "flex-start",
            flexDirection: "column",
            overflow: "hidden"
          }}>
          <AntTooltip title={`${record?.summary} (${item})`} placement="topLeft">
            <AntText style={{ color: "#2967dd" }}>{record?.summary || item || ""}</AntText>
          </AntTooltip>
        </div>
      )
    }
  ];

  forEach(sprints, (sprint: any, sIndex: number) => {
    const sprintColumn = {
      ...baseColumnConfig("", "sprints"),
      title: <Tooltip title={sprint?.name}>{truncateAndEllipsis(sprint?.name || "")}</Tooltip>,
      key: sIndex,
      render: (sprints: any[], record: any, index: number) => {
        const curSprint = (sprints || []).length ? sprints?.[sIndex] : {};
        return (
          <JiraPriorityColumnComponent
            priority={priorityMappping[curSprint?.priority !== Number.MAX_VALUE ? curSprint?.priority - 1 : 4]}
            teamAllocationTrend={curSprint?.teamAllocationTrend || 0}
            teamAllocations={curSprint?.team_allocations || []}
          />
        );
      }
    };
    priorityChartTableColumnConfig.push(sprintColumn);
  });

  return priorityChartTableColumnConfig;
};
