import React, { useState } from "react";
import { Icon } from "antd";

interface ActionComponentProps {
  className: string;
  index: string;
  onClick: () => void;
  style?: React.CSSProperties;
}

const ActionComponent: React.FC<ActionComponentProps> = (props: ActionComponentProps) => {
  const { className, index, onClick } = props;
  const [displayIcon, setDisplayIcon] = useState<{ [x: string]: string }>();

  const getDisplayIcon = (index: string) => {
    if (!displayIcon || !displayIcon[index]) {
      return "dot";
    } else return displayIcon[index];
  };

  return (
    <div
      key={index}
      className={`${className}`}
      style={{ ...props.style }}
      onClick={onClick}
      onMouseEnter={() => {
        setDisplayIcon({
          ...(displayIcon || {}),
          [index]: "plus"
        });
      }}
      onMouseLeave={() => {
        setDisplayIcon({
          ...(displayIcon || {}),
          [index]: "dot"
        });
      }}>
      {getDisplayIcon(index) === "dot" ? (
        <div className={"dot"} />
      ) : (
        <Icon style={{ cursor: "pointer" }} type={"plus"} />
      )}
    </div>
  );
};

export default ActionComponent;
