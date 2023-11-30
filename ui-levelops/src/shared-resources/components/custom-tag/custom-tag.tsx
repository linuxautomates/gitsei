import { Tag } from "antd";
import React from "react";
import "./custom-tag.scss";

interface CustomTagProps {
  label: string;
  onRemove?: (event: any) => void;
}

const CustomTag: React.FC<CustomTagProps> = (props: CustomTagProps) => {
  return (
    <div className="custom_tag_div">
      <Tag color="#c6d3fb" closable onClose={(e: any) => props.onRemove?.(e)}>
        {props.label}
      </Tag>
    </div>
  );
};

export default React.memo(CustomTag);
