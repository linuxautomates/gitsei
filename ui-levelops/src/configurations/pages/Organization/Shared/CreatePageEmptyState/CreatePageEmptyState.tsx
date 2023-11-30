import React from "react";
import { AntText } from "shared-resources/components";
import "./CreatePageEmptyState.scss";

interface CustomNoDataUiProps {
  heading: string;
  imgSrc: string;
  description: React.ReactNode;
  actionButton: React.ReactNode;
}

const CreatePageEmptyState: React.FC<CustomNoDataUiProps> = props => {
  return (
    <div className={"no-data-ui-container"}>
      <div className={"no-data-ui-container-content-container"}>
        <img src={props.imgSrc} className={"no-data-ui-container-content-container-img"} />
        <AntText className={"no-data-ui-container-content-container-heading"}>{props.heading}</AntText>
        {props.description}
        {props.actionButton}
      </div>
    </div>
  );
};

export default CreatePageEmptyState;
