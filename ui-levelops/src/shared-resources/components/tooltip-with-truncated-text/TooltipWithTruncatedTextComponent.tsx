import React from "react";
import { truncateAndEllipsis } from "utils/stringUtils";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { AntTooltipComponent as AntTooltip } from "../ant-tooltip/ant-tooltip.component";

const TooltipWithTruncatedTextComponent: React.FC<{
  title: string;
  allowedTextLength: number;
  textClassName?: string;
  leftEllipsis?: boolean;
  hideTooltip?: boolean;
}> = ({ title, allowedTextLength, textClassName, leftEllipsis, hideTooltip }) => {
  return (
    <AntTooltip title={!hideTooltip && (title || "").length > allowedTextLength ? title || "" : null}>
      <AntText className={textClassName}>{truncateAndEllipsis(title || "", allowedTextLength, leftEllipsis)}</AntText>
    </AntTooltip>
  );
};

export default React.memo(TooltipWithTruncatedTextComponent);
