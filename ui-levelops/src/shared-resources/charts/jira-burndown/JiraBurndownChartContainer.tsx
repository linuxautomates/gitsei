import React, { useCallback, useMemo, useState } from "react";
import { Icon } from "antd";
import { AntButton } from "shared-resources/components";
import { JiraBurndownChartProps } from "../chart-types";
import "./jiraBurndown.styles.scss";
import { jiraBurndownData } from "./jiraBurndownMock";
import BurndownLegendComponent from "./Components/BurndownLegendComponent";
import JiraBurndownCard from "./Components/JiraBurndownCard";
import CustomCarousalComponent from "shared-resources/components/custom-carousal/CustomCarousalComponent";
import { newUXColorMapping } from "../chart-themes";

const TOTAL_COUNT = jiraBurndownData.records.length;
const LIMIT_PER_PAGE = 4;
const JiraBurndownChartContainer: React.FC<JiraBurndownChartProps> = props => {
  const { onClick, data } = props;
  const { records, dataKeys } = data;
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const next = useCallback(() => setCurrentIndex(prev => prev + 1), []);
  const prev = useCallback(() => setCurrentIndex(prev => prev - 1), []);

  const range = useMemo(() => {
    if (!(records || []).length) return { high: 0, low: 0 };
    const page = currentIndex + 1;
    const high = LIMIT_PER_PAGE * page;
    const low = high - LIMIT_PER_PAGE + 1;
    return { high: Math.min(high, records.length), low };
  }, [currentIndex, records]);

  const getFiltereddata = useMemo(() => {
    const { high, low } = range;
    if (low === 0) return [];
    return records.slice(low - 1, high);
  }, [currentIndex, range]);

  return (
    <div className="jira-burndown-container">
      <div className="jira-burndown-container-body">
        <div className="jira-burndown-container-body-button-left">
          <AntButton onClick={prev} disabled={currentIndex === 0} shape="circle" type="primary">
            <Icon type="left" />
          </AntButton>
        </div>
        <div className="jira-burndown-container-body-content">
          {getFiltereddata.map((curData: any) => {
            return <JiraBurndownCard onChartClick={onClick} data={curData} dataKeys={dataKeys} />;
          })}
        </div>
        <div className="jira-burndown-container-body-button-right">
          <AntButton disabled={range.high === records.length} shape="circle" type="primary" onClick={next}>
            <Icon type="right" />
          </AntButton>
        </div>
      </div>
      <div>
        <div className="burndown-footer-container">
          <div className="burndown-footer">
            <CustomCarousalComponent
              totalCount={records.length}
              range={range}
              handleNext={next}
              handlePrev={prev}
              currentIndex={currentIndex}
              perPage={LIMIT_PER_PAGE}
              dotLimitingFactor={Math.min(20, records.length)}
            />
            <div className="burndown-legend">
              <BurndownLegendComponent statusList={jiraBurndownData.status_list} mapping={newUXColorMapping} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default JiraBurndownChartContainer;
