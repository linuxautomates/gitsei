import React, { SyntheticEvent } from "react";
import { Button } from "antd";
import "./back-button.style.scss";

interface BackButtonProps {
  onClick: (event: SyntheticEvent) => void;
}

const BackButton: React.FC<BackButtonProps> = ({ onClick }) => {
  return (
    <div className="back-button-wrapper flex ant-btn-block">
      <Button className={"back-button"} size="default" type="primary" onClick={onClick}>
        Back
      </Button>
    </div>
  );
};

export default BackButton;
