import React from "react";
import { capitalize, forEach, unset } from "lodash";
import StackedProgressBar from "shared-resources/charts/jira-burndown/Components/StackedProgressBar";
import { AntTooltip, NameAvatar } from "shared-resources/components";
import EffortInvestmentPopoverContent from "./EffortInvestmentPopoverContent";
import { truncateAndEllipsis } from "utils/stringUtils";

export const getTableDynamicConfig = (
  data: any,
  mapping: any,
  ids: string[],
  unit?: string,
  onClick?: (e: any) => void
) => {
  let config: any[] = [
    {
      dataIndex: "name",
      width: "16%",
      render: (item: any) => {
        return (
          <div className="name-column">
            <div className="name-column-avatar">
              <NameAvatar name={item} />
            </div>
            <p className="name-column-text">{capitalize(item)}</p>
          </div>
        );
      }
    }
  ];
  const sprintsList = data["sprints"] || [];

  const sprints = (sprintsList || []).map((sprint: any, sIndex: number) => {
    return {
      title: <AntTooltip title={sprint?.name}>{truncateAndEllipsis(sprint?.name || "")}</AntTooltip>,
      dataIndex: "sprints",
      className: sIndex + 1 === sprintsList.length ? "column-border" : "",
      render: (item: any[], record: any) => {
        const curSprint = item[sIndex] || {};
        let popoverPayload: any = {};
        forEach(Object.keys(curSprint?.percentage_sprintdata || []), key => {
          popoverPayload = {
            ...popoverPayload,
            [key]: curSprint[key]
          };
        });
        return (
          <StackedProgressBar
            PopoverContent={EffortInvestmentPopoverContent}
            popOverContentProps={{
              unit,
              assigneeName: record?.name,
              payload: popoverPayload,
              onClick
            }}
            mapping={mapping}
            records={[curSprint?.percentage_sprintdata]}
            dataKeys={ids}
            metaData={{
              radiusFactor: 4,
              width: 130,
              height: 50,
              orientation: "vertical",
              xAxisType: "number",
              yAxisType: "category",
              xHide: true,
              yHide: true,
              rectangleHeight: 32
            }}
          />
        );
      }
    };
  });
  config.push(...sprints, {
    title: "AVG.",
    dataIndex: "average",
    className: "avg-column",
    render: (item: any, record: any) => {
      const avgPayload = { ...item };
      unset(avgPayload, ["percentage_avg"]);
      return (
        <StackedProgressBar
          PopoverContent={EffortInvestmentPopoverContent as any}
          popOverContentProps={{
            unit,
            assigneeName: record?.name,
            payload: avgPayload,
            onClick
          }}
          mapping={mapping}
          records={[item?.percentage_avg]}
          dataKeys={ids}
          metaData={{
            radiusFactor: 4,
            width: 180,
            height: 50,
            orientation: "vertical",
            xAxisType: "number",
            yAxisType: "category",
            xHide: true,
            yHide: true,
            rectangleHeight: 32
          }}
        />
      );
    }
  });
  return config;
};
