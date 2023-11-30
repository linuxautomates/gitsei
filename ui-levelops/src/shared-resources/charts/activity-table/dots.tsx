import React from "react";
import "./styles/active-table.style.scss";

const MAX_DOTS = 6;

export const dots = (value?: number, color?: string) => {
  if (value) {
    const dots = [];
    for (let index = 0; index < MAX_DOTS && index < value; index++) {
      dots.push(<span className="active-table-dot" style={{ backgroundColor: color ?? "#999" }} key={index} />);
    }
    return (
      <>
        {dots}
        {value > MAX_DOTS && <span className="active-table-value">{`+${value - MAX_DOTS}`}</span>}
      </>
    );
  }
  return null;
};
