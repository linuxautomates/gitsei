import React, { useCallback, useMemo, useState } from "react";
import { Divider } from "antd";
import { AntSelectComponent as AntSelect } from "shared-resources/components/ant-select/ant-select.component";
import CustomCarousalComponent from "shared-resources/components/custom-carousal/CustomCarousalComponent";
import { EffortInvestmentTeamChartProps } from "../chart-types";
import EffortInvestmentChartContentContainer from "./components/EffortInvestmentChartContentContainer";
import "./effortInvestmentTeamChart.styles.scss";

const LIMIT_PER_PAGE = 2;

const sort_options = [
  { label: "Work on P1's", value: "work_on_P1" },
  { label: "Work on P2's", value: "work_on_P2" }
]; // temp options

const EffortInvestmentTeamChartContainer: React.FC<EffortInvestmentTeamChartProps> = (
  props: EffortInvestmentTeamChartProps
) => {
  const { onClick, data } = props;
  const { records, unit } = data;
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [sort, setSort] = useState<string>("work_on_P1");

  const next = useCallback(() => setCurrentIndex(prev => prev + 1), []);
  const prev = useCallback(() => setCurrentIndex(prev => prev - 1), []);

  const range = useMemo(() => {
    const page = currentIndex + 1;
    const high = Math.min(records.length, LIMIT_PER_PAGE * page);
    const low = Math.max(1, high - LIMIT_PER_PAGE + 1);
    return { high, low };
  }, [currentIndex]);

  const getFilteredSortedData = useMemo(() => {
    const { high, low } = range;
    const records = data?.records; //effortInvestmentTeamChartMock.records;
    if (sort) {
      records.sort((a: any, b: any) => a?.[sort] - b?.[sort]);
    }
    return records.slice(low - 1, high);
  }, [currentIndex, range, sort]);

  const handleSortChange = useCallback((value: string) => {
    setSort(value);
  }, []);

  return (
    <div className="effort-investment-chart-container">
      <div className="sort-container">
        <p className="sort-container-text">Sort Teams By :</p>
        <AntSelect
          showArrow={true}
          value={sort}
          options={sort_options}
          onChange={handleSortChange}
          className="sort-container-select"
        />
      </div>
      <Divider className="divider-style" />
      <div className="content-container">
        {getFilteredSortedData.map((data: any, index: number) => (
          <EffortInvestmentChartContentContainer
            unit={unit}
            onClick={onClick}
            team={data}
            showDivider={index + 1 !== getFilteredSortedData.length}
          />
        ))}
      </div>
      {getFilteredSortedData.length > 1 && (
        <div className="team-footer-container">
          <CustomCarousalComponent
            totalCount={(data?.records || []).length}
            showActionButtons={true}
            range={range}
            handleNext={next}
            handlePrev={prev}
            currentIndex={currentIndex}
            perPage={LIMIT_PER_PAGE}
            dotLimitingFactor={8}
          />
        </div>
      )}
    </div>
  );
};

export default EffortInvestmentTeamChartContainer;
