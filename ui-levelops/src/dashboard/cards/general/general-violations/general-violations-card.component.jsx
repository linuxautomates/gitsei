import React from "react";
import { Icon } from "antd";
import { AntCard } from "shared-resources/components";

const GeneralViolationsCard = props => {
  return (
    <AntCard
      {...props}
      type="inner"
      title={
        <span>
          {props.icontype && <Icon type={props.icontype} />}
          {props.title}
        </span>
      }>
      {props.description}
    </AntCard>
  );
};

export default GeneralViolationsCard;
