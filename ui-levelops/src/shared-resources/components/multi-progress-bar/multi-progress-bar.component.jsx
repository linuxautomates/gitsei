import React from "react";
import * as PropTypes from "prop-types";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";

const colorMap = {
  low: "#2b4fa3",
  medium: "#f19a3d",
  high: "#eb603f"
};

export class MultiProgressBarComponent extends React.PureComponent {
  render() {
    const { high, medium, low } = this.props;
    const total = high + medium + low;
    const progressReport = ["high", "medium", "low"].map(status => ({
      status: status,
      progress: Math.round((this.props[status] / total) * 100),
      color: colorMap[status]
    }));
    return (
      <div>
        <div className="multi-color-progressbar flex mb-20">
          {progressReport.map((p, index) => {
            return <div key={index} className={`w-${p.progress} h-100`} style={{ backgroundColor: p.color }}></div>;
          })}
        </div>
        <div className="flex progressbar-legend">
          {progressReport.map((p, index) => {
            return (
              <div key={index} className="flex align-center mr-15">
                <span className="progressbar-legend-color mr-5" style={{ backgroundColor: p.color }}></span>
                <AntText className="progressbar-legend-text">{p.status}</AntText>
              </div>
            );
          })}
        </div>
      </div>
    );
  }
}

MultiProgressBarComponent.propTypes = {
  low: PropTypes.number.isRequired,
  medium: PropTypes.number.isRequired,
  high: PropTypes.number.isRequired
};

MultiProgressBarComponent.defaultProps = {
  low: 0,
  medium: 0,
  high: 0
};
