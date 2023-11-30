import { Statistic, Tag, Icon } from "antd";
import React, { useContext, useEffect, useMemo } from "react";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import cx from "classnames";
import "./doraStatChart.scss";
import { WidgetDrilldownHandlerContext } from "dashboard/pages/context";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";

const BAND: any = {
  ELITE: {
    color: "#61BA14"
  },
  MEDIUM: {
    color: "#EF9830"
  },
  HIGH: {
    color: "#789FE9"
  },
  LOW: {
    color: "#D4380D"
  },
  SLOW: {
    color: "#D4380D"
  },
  GOOD: {
    color: "#61BA14"
  }
};

export interface DoraStatChartProps {
  value: number;
  unit: string;
  unitSymbol: string;
  band: string | undefined;
  count: number;
  descInterval?: string;
  descStringValue?: string;
  isRelative: boolean;
  showDoraGrading: boolean;
  onClick?: () => void;
  clicked?: boolean;
  setClicked?: React.Dispatch<React.SetStateAction<boolean>>;
  reportType?: string;
}

const DoraStatChart: React.FC<DoraStatChartProps> = ({
  value,
  unitSymbol,
  band,
  count,
  descInterval,
  descStringValue,
  unit,
  isRelative,
  showDoraGrading,
  onClick,
  clicked,
  setClicked,
  reportType
}) => {
  const { isDrilldownOpen } = useContext(WidgetDrilldownHandlerContext);

  useEffect(() => {
    if (!isDrilldownOpen) {
      setClicked && setClicked(false);
    }
  }, [isDrilldownOpen]);

  const statDetails = useMemo(() => {
    let countValue = `${count} `;
    let realValue;
    if (isRelative) {
      realValue = Math.round(count * (value / 100));
    }
    const getDoraSingleStateValue = get(widgetConstants, [reportType as string, "getDoraSingleStateValue"], undefined);
    let getDoraSingleStateValueDisplay;
    if (getDoraSingleStateValue) {
      getDoraSingleStateValueDisplay = getDoraSingleStateValue({
        isRelative,
        count,
        realValue,
        descStringValue
      });
      countValue = getDoraSingleStateValueDisplay;
    }

    return (
      <div className="stats-extra-info">
        <div className="info-description">
          <AntText className="count-value">{`${countValue}`}</AntText>
        </div>
        <div className="interval-description">
          <span className="mr-8">
            <Icon type="calendar"></Icon>
          </span>
          <AntText>{descInterval}</AntText>
        </div>
      </div>
    );
  }, [count, value, descInterval, reportType]);

  return (
    <div className="dora-stats-chart-component">
      <Statistic
        className={cx({ "hightlight-value": clicked && isDrilldownOpen })}
        title={""}
        value={value}
        valueRender={value => (
          <div className={cx("statistical-value", { pointer: !!onClick })} onClick={onClick}>
            {value}
            {unitSymbol}
          </div>
        )}
        suffix={<>{unit}</>}
        precision={Number.isInteger(value) ? undefined : 2}
      />
      {band && showDoraGrading && (
        <Tag className="band-value" color={BAND[band]?.color}>
          {band}
        </Tag>
      )}
      {statDetails}
    </div>
  );
};

export default DoraStatChart;
