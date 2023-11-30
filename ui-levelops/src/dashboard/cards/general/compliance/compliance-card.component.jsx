import React from "react";
import { Progress } from "antd";
import { AntCard } from "shared-resources/components";
import "./compliance-card.style.scss";

const ComplianceCard = props => {
  return (
    <AntCard
      className="compliance-card"
      type={"inner"}
      title={<span>{props.title}</span>}
      extra={<img src={props.imgSrc} alt={""} />}>
      <span className="progress-bar--success">{props.successPercent}%</span>
      <Progress
        showInfo={false}
        successPercent={props.successPercent}
        percent={100}
        // strokeColor="red" unable pass to css vars over here so.. applying stroke color from scss file.
        strokeLinecap="square"
        strokeWidth={10}
      />
      <div className="progress-bar--report">
        <span>{props.pass} pass</span>
        <span>{props.fail} fails</span>
      </div>
    </AntCard>
  );
};

export default ComplianceCard;
