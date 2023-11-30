import { RestStageConfig, RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { get } from "lodash";
import React from "react";
import { Sector } from "recharts";

export const renderActiveShape = (props: any) => {
  const RADIAN = Math.PI / 180;
  const { cx, cy, midAngle, innerRadius, outerRadius, startAngle, endAngle, fill, payload, percent, value } = props;
  const sin = Math.sin(-RADIAN * midAngle);
  const cos = Math.cos(-RADIAN * midAngle);
  const sx = cx + (outerRadius + 2) * cos;
  const sy = cy + (outerRadius + 2) * sin;
  const mx = cx + (outerRadius + 20) * cos;
  const my = cy + (outerRadius + 20) * sin;
  const textAnchor = cos >= 0 ? "start" : "end";

  return (
    <g>
      <Sector
        cx={cx}
        cy={cy}
        innerRadius={innerRadius}
        outerRadius={outerRadius}
        startAngle={startAngle}
        endAngle={endAngle}
        fill={fill}
        cornerRadius={5}
      />
      <Sector
        cx={cx}
        cy={cy}
        startAngle={startAngle}
        endAngle={endAngle}
        innerRadius={outerRadius + 1}
        outerRadius={outerRadius + 3}
        fill={fill}
        cornerRadius={5}
      />
      <svg id="custom-pie">
        <defs>
          <filter x="0" y="0" width="1" height="1" id="bg-text">
            <feFlood flood-color="white" />
            <feComposite in="SourceGraphic" operator="xor" />
          </filter>
        </defs>
        <path d={`M${sx},${sy}L${mx},${my}`} stroke={fill} fill="none" />
        <circle cx={mx} cy={my} r={2} fill={fill} stroke="none" />
        <svg filter="url(#bg-text)">
          <text x={mx + (cos >= 0 ? 1 : -1) * 5} y={my} textAnchor={textAnchor} fill="#333">{`${value} tickets`}</text>
          <text x={mx + (cos >= 0 ? 1 : -1) * 5} y={my} dy={18} textAnchor={textAnchor} fill="#999">
            {`(${(percent * 100).toFixed(2)}%)`}
          </text>
        </svg>
        <text x={mx + (cos >= 0 ? 1 : -1) * 5} y={my} textAnchor={textAnchor} fill="#333">{`${value} tickets`}</text>

        <text x={mx + (cos >= 0 ? 1 : -1) * 5} y={my} dy={18} textAnchor={textAnchor} fill="#999">
          {`(${(percent * 100).toFixed(2)}%)`}
        </text>
      </svg>
    </g>
  );
};

export const getAllStages = (workspaceOuProfilestate: RestWorkflowProfile) => {
  const leadTimeProfile = get(workspaceOuProfilestate, "lead_time_for_changes", {});
  const fixedStages = get(leadTimeProfile, "fixed_stages", []);
  const postDevelopmentStages = get(leadTimeProfile, "post_development_custom_stages", []);
  const preDevelopmentStages = get(leadTimeProfile, "pre_development_custom_stages", []);
  return [...fixedStages, ...postDevelopmentStages, ...preDevelopmentStages].map(
    (stage: RestStageConfig) => stage.name
  );
};
