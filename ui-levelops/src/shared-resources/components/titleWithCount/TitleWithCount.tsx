import { Badge } from "antd";
import React from "react";

interface TitleWithCountProps {
  titleClass?: string;
  containerClassName?: string;
  title: string;
  count: number;
  showZero?: boolean;
}

const TitleWithCount: React.FC<TitleWithCountProps> = ({ title, count, showZero, titleClass, containerClassName }) => {
  return (
    <div className={`flex align-center ${containerClassName}`}>
      <div className={`mr-5 ${titleClass || ""}`}>{title} </div>
      <div className="mr-5">
        <Badge
          style={{ backgroundColor: "var(--harness-blue)" }}
          count={count || 0}
          overflowCount={count || 0}
          showZero={showZero}
        />
      </div>
    </div>
  );
};

export default React.memo(TitleWithCount);
