import { Menu } from "antd";
import React, { useMemo, useState } from "react";
import { AntCheckbox } from "shared-resources/components";

const CustomLegendComponent = (props: any) => {
  const { legends } = props;
  const allLegends = useMemo(() => {
    return (
      <>
        {legends.map((legend: any) => {
          const { color, label } = legend;
          return (
            <span key={`flex filter-${label}`}>
              <AntCheckbox
                className={"legend-checkbox no-events"}
                style={{ "--tick-color": color }}
                indeterminate={label}
                checked={true}
                onChange={(e: any) => e.preventDefault()}>
                {label}
              </AntCheckbox>
            </span>
          );
        })}
      </>
    );
  }, []);
  return (
    <>
      <div className="chart-legend-container">
        <div className="legend-filters-container">{allLegends}</div>
      </div>
    </>
  );
};

export default CustomLegendComponent;
