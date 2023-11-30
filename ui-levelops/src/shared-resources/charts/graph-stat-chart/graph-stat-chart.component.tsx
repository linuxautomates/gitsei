import React, { ReactNode, useMemo } from "react";
import { Empty, Icon, Statistic } from "antd";
import { GraphStatChartProps } from "../chart-types";
import "./graph-stat-chart.styles.scss";
import { getValueStyle, getSuffixValueStyle, getPrefixValueStyle } from "./helper";
import { get } from "lodash";
import cx from "classnames";
import { AntText, TooltipWithTruncatedText } from "shared-resources/components";

const GraphStatChart: React.FC<GraphStatChartProps> = (props: GraphStatChartProps) => {
  const { stat, unit, onClick, statTrend, categoryName, idealRange, isDemo } = props;

  const formatValue = useMemo(() => {
    return `${stat} %`;
  }, [stat]);

  const renderValue = useMemo(
    () => (value: ReactNode) => {
      const dataValue: string = get(value, ["props", "value"], "");
      if (dataValue === "NaN") return <Empty />;
      const symbolIndex = dataValue.indexOf("%");
      const prefixPart = dataValue.substring(0, symbolIndex);
      const suffixPart = `${dataValue.substring(symbolIndex)}`;
      const color = (statTrend as any) > 0 ? "#262626" : "#F5222D";
      return (
        <div className="value-style">
          <AntText className="value-style-prefix" style={getPrefixValueStyle(color)}>
            {prefixPart}
          </AntText>
          <AntText
            className="value-style-suffix"
            style={getSuffixValueStyle("percentage", color)}>{`${suffixPart}`}</AntText>
        </div>
      );
    },
    [unit]
  );

  const renderSuffix = useMemo(() => {
    if (!statTrend) return null;
    const statTrendIcon = statTrend > 0 ? "caret-up" : "caret-down";
    const color = statTrend > 0 ? "#595959" : "#CF1322";
    return (
      <div className="suffix-style">
        <Icon type={statTrendIcon} theme="outlined" style={{ color: color }} />
        <AntText className="suffix-style-value">{statTrend}%</AntText>
      </div>
    );
  }, [statTrend]);

  return (
    <div
      className={cx({ "graph-stat-cursor-pointer": props.hasClickEvents }, "graph-stat-container")}
      onClick={onClick}>
      <TooltipWithTruncatedText allowedTextLength={13} title={categoryName} textClassName={"category-text"} />
      <Statistic value={formatValue} valueStyle={getValueStyle()} valueRender={renderValue} suffix={renderSuffix} />
      <div className="ideal-text-container">
        {idealRange &&
          idealRange.hasOwnProperty("min") &&
          idealRange.hasOwnProperty("max") &&
          (isDemo ? (
            <AntText className="ideal-text">Goal {`${idealRange.min}% - ${idealRange.max}%`}</AntText>
          ) : (
            <AntText className="ideal-text">Ideal {`${idealRange.min}% - ${idealRange.max}%`}</AntText>
          ))}
      </div>
    </div>
  );
};

export default React.memo(GraphStatChart);
