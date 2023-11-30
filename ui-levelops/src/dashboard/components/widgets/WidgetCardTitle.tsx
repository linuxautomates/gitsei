import React from "react";
import { AntIcon, AntTooltip } from "shared-resources/components";

interface WidgetCardTitleProps {
  widgetRef: any;
  showTooltip?: boolean;
  title: string;
  titleRef: any;
  titleStyle: any;
  isStat?: boolean;
  description?: string;
}

const WidgetCardTitle = (props: WidgetCardTitleProps) => {
  const { widgetRef, showTooltip, title, titleRef, titleStyle, isStat, description } = props;
  return (
    <div className="flex align-center justify-start mr-6">
      <AntTooltip title={showTooltip ? title : undefined} trigger="hover" getTooltipContainer={() => widgetRef.current}>
        <span ref={titleRef as any} style={titleStyle}>
          {title}
        </span>
      </AntTooltip>
      {description && (
        // @ts-ignore
        <AntTooltip
          title={description}
          trigger={["hover", "click"]}
          getPopupContainer={(trigger: any) => widgetRef.current}>
          <AntIcon type="info-circle" theme="outlined" className={"description-icon"} />
        </AntTooltip>
      )}
    </div>
  );
};

export default WidgetCardTitle;
