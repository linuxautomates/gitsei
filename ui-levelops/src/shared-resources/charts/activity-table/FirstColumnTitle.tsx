import { Icon } from "antd";
import React from "react";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";

interface FirstColumnTitleProps {
  viewBy?: string;
  showName: boolean;
  setShowName: (displayName: boolean) => void;
}

const FirstColumnTitle = (props: FirstColumnTitleProps) => {
  const { viewBy, showName, setShowName } = props;
  if (viewBy?.toUpperCase().includes("REPO")) {
    setShowName(false);
    return <AntText className="pr-activity-first-title-hide-show">REPO</AntText>;
  } else {
    return showName ? (
      <AntText className="pr-activity-first-title-hide-show" onClick={() => setShowName(false)}>
        <Icon className="invisible-eye-icon" type="eye-invisible" /> Hide Name
      </AntText>
    ) : (
      <AntText className="pr-activity-first-title-hide-show" onClick={() => setShowName(true)}>
        <Icon type="eye" title="Show Name" /> Show Name
      </AntText>
    );
  }
};

export default FirstColumnTitle;
