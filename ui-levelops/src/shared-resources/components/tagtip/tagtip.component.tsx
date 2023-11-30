// An ant-tag that shows an ant-tooltip on hover.

import React from "react";
import { Tooltip, Tag } from "antd";

type TagtipType = {
  tagText: string;
  tooltipText: string;
  color?: string;
};

const TagtipComponent = (props: TagtipType) => {
  let { tagText, tooltipText, color } = props;
  return (
    <div>
      <Tooltip title={tooltipText}>
        <Tag color={color}>{tagText}</Tag>
      </Tooltip>
    </div>
  );
};

export default TagtipComponent;
