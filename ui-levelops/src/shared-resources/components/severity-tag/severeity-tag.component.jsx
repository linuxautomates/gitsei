import React from "react";
import { AntTagComponent as AntTag } from "shared-resources/components/ant-tag/ant-tag.component";
import { PropTypes } from "prop-types";
import "./severity-tag.style.scss";

const SeverityTag = ({ severity }) => {
  const colorMap = {
    high: "red",
    highest: "red",
    medium: "gold",
    low: "blue"
  };

  return (
    <AntTag color={colorMap[severity]}>
      <span style={{ textTransform: "capitalize" }}>{severity}</span>
    </AntTag>
  );
};

SeverityTag.propTypes = {
  severity: PropTypes.oneOf(["high", "medium", "low", "highest"]).isRequired
};

export default SeverityTag;
