import React from "react";
import { default as AntInputClearButton } from "../ant-input-clear-button/AntInputClearButton";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { default as FilterLabel } from "../filter-label/FilterLabel";
import "./BigTag.style.scss";

interface BigTagProps {
  label: string;
  value: string;
  onValueClick?: (...args: any) => any;
  onCloseClick?: (...args: any) => any;
}

const BigTag: React.FC<BigTagProps> = props => {
  const { label, value, onValueClick, onCloseClick } = props;

  return (
    <div className="BigTag">
      <FilterLabel label={label} />
      {typeof onValueClick === "function" ? (
        <AntButton type="link" onClick={onValueClick}>
          {value}
        </AntButton>
      ) : (
        <div>{value}</div>
      )}

      {/* Close Button */}
      {typeof onCloseClick === "function" && (
        <div
          style={{
            position: "absolute",
            top: "0px",
            right: "0px"
          }}>
          <AntInputClearButton onClick={onCloseClick} />
        </div>
      )}
    </div>
  );
};

export default BigTag;
