import { Icon, Tag } from "antd";

import { IPosition } from "@mrblenny/react-flow-chart";
import React from "react";
// @ts-ignore
import { ICustomLinkDefaultProps } from "shared-resources/helpers/FlowChartWrapper/FlowChartWrapper";
import { get } from "lodash";
import "./custom-link.scss";

export const generateRightAnglePath = (startPos: IPosition, endPos: IPosition) => {
  // @ts-ignore
  //startPos.y = startPos.y + 10;
  const width = Math.abs(startPos.x - endPos.x);
  const height = Math.abs(startPos.y - endPos.y);
  const leftToRight = startPos.x < endPos.x;
  const topToBottom = startPos.y < endPos.y;
  const isHorizontal = width > height;

  let start: IPosition;
  let end: IPosition;
  if (isHorizontal) {
    start = leftToRight ? startPos : endPos;
    end = leftToRight ? endPos : startPos;
    start = endPos;
    end = startPos;
  } else {
    start = topToBottom ? startPos : endPos;
    end = topToBottom ? endPos : startPos;
  }

  const vertex = isHorizontal ? `${start.x},${end.y}` : `${end.x},${start.y}`;

  return `M${start.x},${start.y} L ${vertex} ${end.x},${end.y}`;
};

export const CustomLink = ({
  config,
  link,
  startPos,
  endPos,
  onLinkMouseEnter,
  onLinkMouseLeave,
  onLinkClick,
  isHovered,
  isSelected,
  onDeleteClick,
  onEditClick
}: ICustomLinkDefaultProps) => {
  //const points = generateCurvePath(startPos, endPos);
  const points = generateRightAnglePath(startPos, endPos);
  const option = get(link, ["properties", "option"], undefined);
  const optionLength = option ? option.length : 0;

  // @ts-ignore
  return (
    <>
      <div>
        <svg
          className={"custom-link"}
          style={{ overflow: "visible", position: "absolute", cursor: "pointer", left: 0, right: 0 }}>
          <path d={points} strokeWidth="2" fill="none" />
          {/* Thick line to make selection easier */}
          <path
            d={points}
            //className={"custom-link"}
            //stroke={getStrokeColor()}
            //stroke={strokeColor}
            strokeWidth="15"
            fill="none"
            strokeLinecap="round"
            strokeOpacity={isHovered || isSelected ? 0.1 : 0}
            onClick={e => {
              console.log("link got clicked");
              // @ts-ignore
              onEditClick(link);
              //onLinkClick({ config, linkId: link.id });
              e.stopPropagation();
            }}
          />
        </svg>
      </div>
      <div style={{ left: endPos.x - 8, top: endPos.y - 22, position: "absolute" }}>
        <Icon type="down" style={{ color: "#8A94A5", fontSize: "16px" }} />
      </div>
      {option !== undefined && (
        <div style={{ left: endPos.x - (30 + Math.round(optionLength)), top: endPos.y - 50, position: "absolute" }}>
          <Tag
            style={{ cursor: "pointer" }}
            onClick={e => {
              e.preventDefault();
              e.stopPropagation();
              // @ts-ignore
              onEditClick(link);
            }}>
            {option}
          </Tag>
        </div>
      )}
    </>
  );
};
