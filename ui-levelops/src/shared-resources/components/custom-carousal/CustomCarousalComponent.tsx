import { Icon } from "antd";
import React, { ReactNode, useMemo } from "react";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import "./CustomCarousal.styles.scss";

interface CustomCarousalProps {
  range: { low: number; high: number };
  totalCount: number;
  perPage: number;
  currentIndex: number;
  dotLimitingFactor: number;
  showActionButtons?: boolean;
  handleNext: () => void;
  handlePrev: () => void;
}

const CustomCarousalComponent: React.FC<CustomCarousalProps> = ({
  range,
  dotLimitingFactor, // limits the count of dots
  totalCount,
  perPage,
  currentIndex,
  showActionButtons,
  handleNext,
  handlePrev
}) => {
  const getTotalDots = useMemo(() => {
    const totalRecords = dotLimitingFactor;
    return Math.ceil(totalRecords / perPage);
  }, [totalCount, range]);

  const getDots = useMemo(() => {
    const dotsArray: ReactNode[] = [];
    for (let index = 0; index < getTotalDots; index++) {
      dotsArray.push(
        <div
          className={
            currentIndex === index ||
            (index === getTotalDots - 1 && range?.high > dotLimitingFactor && range?.high <= totalCount)
              ? "custom-dot-active"
              : "custom-dot"
          }
        />
      );
    }
    return dotsArray;
  }, [getTotalDots, currentIndex]);

  return (
    <div className="custom-carousal">
      <div className="custom-carousal-pagination">
        <p className="carousal-text">{`Showing ${range?.low}-${range?.high} of ${totalCount}`}</p>
      </div>
      <div className="custom-carousal-dots">{getDots}</div>
      {showActionButtons && (
        <div className="custom-carousal-action_buttons">
          <AntButton onClick={handlePrev} disabled={currentIndex === 0} shape="circle" type="primary" size="small">
            <Icon type="left" />
          </AntButton>
          <AntButton
            disabled={range?.high === totalCount}
            shape="circle"
            type="primary"
            onClick={handleNext}
            size="small">
            <Icon type="right" />
          </AntButton>
        </div>
      )}
    </div>
  );
};

export default CustomCarousalComponent;
