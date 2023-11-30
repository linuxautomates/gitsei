import { Empty } from "antd";
import cx from "classnames";
import { getAvgTimeAndIndicator } from "custom-hooks/helpers/leadTime.helper";
import { capitalize, findIndex, isEqual } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { AntButton, AntTag, AntText } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import { DemoLeadTimePhaseChartProps } from "../Widget-Grapg-Types/demo-lead-time-phase-chart.types";
import DemoLeadTimeEventComponent from "./Components/demo-lead-time-event";
import DemoLeadTimeStageDurationMetric from "./Components/demo-lead-time-metrics";
import DemoLeadTimePhaseItem from "./Components/demo-lead-time-phase-item";
import LeadTimeLegend from "shared-resources/charts/components/lead-time-stage-legend/LeadTimeLegend";
import LeadTimeFilterDropdown from "shared-resources/charts/components/lead-time-filter-dropdown/LeadTimeFilterDropdown";
import "./demo-lead-time-chart.style.scss";

const DemoLeadTimePhaseChart: React.FC<DemoLeadTimePhaseChartProps> = (props: DemoLeadTimePhaseChartProps) => {
  const { id, data, onClick } = props;
  const dataKey = "mean";
  const [activePhase, setActivePhase] = useState<string | undefined>(undefined);
  const [clientWidth, setClientWidth] = useState<number>();
  const [pivotIndex, setPivotIndex] = useState(0);
  const [showMore, setShowMore] = useState(false);
  const [lines, setLines] = useState<any[]>([]);
  const [filters, setFilters] = useState<any>({});
  const [filteredData, setFilteredData] = useState([]);
  const [legend, setLegend] = useState<any>({ good: true, needs_attention: true, slow: true, missing: false });

  const phaseContainerRef = React.createRef<HTMLDivElement>();

  const stageCount = useMemo(() => (showMore ? data.length : pivotIndex), [showMore, pivotIndex, data]);

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const filterKeys = data.map((item: any) => item.name);
      const initialFilters = filterKeys.reduce((acc: any, next: any) => {
        return {
          ...acc,
          [next]: true
        };
      }, {});
      setFilters(initialFilters);
    }
  }, [data]);

  useEffect(() => {
    if (filters) {
      const activeFilters = Object.keys(filters).filter(key => !!filters[key]);
      const newData = data.filter((item: any) => activeFilters.includes(item.name));
      setFilteredData(newData as any);
    }
  }, [filters]);

  useEffect(() => {
    if (phaseContainerRef && phaseContainerRef.current) {
      const containerWidth = phaseContainerRef.current.clientWidth;
      if (!isEqual(containerWidth, clientWidth)) {
        setClientWidth(containerWidth);
      }
    }
  });

  useEffect(() => {
    if (phaseContainerRef && phaseContainerRef.current) {
      let stagesWidth = 0;
      const connections: any[] = [];
      let pivot = 0;
      const containerWidth = phaseContainerRef.current.clientWidth;
      for (let i = 0; i < filteredData.length; i++) {
        if (i === 0) {
          stagesWidth += 340 + 160 + 32;
        } else {
          stagesWidth += 200;
        }
        if (stagesWidth > containerWidth - 64) {
          const prevStage: any = filteredData[i - 1];
          const currentStage: any = filteredData[i];
          if (!pivot) {
            pivot = i;
          }
          stagesWidth = 200;
          connections.push({
            from: cx("right-line", prevStage?.id),
            to: cx("left-line", currentStage?.id)
          });
        }
      }

      if (!pivot) {
        setPivotIndex(filteredData.length);
      } else {
        setPivotIndex(pivot);
      }
      setLines(connections);
    }
  }, [filteredData, clientWidth, showMore, stageCount]);

  useEffect(() => {
    setActivePhase(undefined);
  }, [data]);

  const handleShowMoreClick = useCallback(() => {
    setShowMore(!showMore);
  }, [showMore]);

  const activePhaseIndex = useMemo(() => {
    return findIndex(data, (item: any) => item.name === activePhase);
  }, [data, activePhase]);

  const summaryHint = useMemo(() => {
    return dataKey === "mean" ? "Average" : capitalize(dataKey);
  }, [dataKey]);

  const renderSummary = useMemo(() => {
    const { duration, rating, unit, backgroudColor, color } = getAvgTimeAndIndicator(filteredData);
    return (
      <div className={cx("demo-summary-container", `demo-summary-container-${rating}`)}>
        <AntText className="duration-title">Total Time</AntText>
        <AntText className="duration-value">{duration}</AntText>
        <AntText className="duration-unit">{unit}</AntText>
        <AntTag className="duration-review" color={color}>
          {toTitleCase(rating)}
        </AntTag>
        <div>
          <DemoLeadTimeStageDurationMetric />
        </div>
      </div>
    );
  }, [filteredData, summaryHint]);

  const showMoreBtn = useMemo(() => {
    const additionalStagesCount = filteredData.length - pivotIndex;

    if (showMore || additionalStagesCount < 1) {
      return null;
    }

    return (
      <div className="demo-more-btn" onClick={handleShowMoreClick}>
        {`+ ${additionalStagesCount}`}
      </div>
    );
  }, [showMore, filteredData, pivotIndex, stageCount]);

  const contextFilterHandler = (filters: any) => {
    setLegend(filters);
  };

  const handlePhaseClick = useCallback(
    (stage: any) => {
      const { phaseId, name } = stage;
      setActivePhase(name);
      onClick && onClick({ phaseId, name, widgetId: id, legend });
    },
    [filteredData, onClick]
  );

  const renderStages = useMemo(
    () =>
      filteredData.slice(0, stageCount).map((stage: any, index: number) => {
        const isEndEvent = index === data.length - 1;
        const isPivot = !showMore && index === pivotIndex - 1;

        return (
          <div className="demo-stage-wrapper">
            <DemoLeadTimePhaseItem
              phase={stage}
              isActivePhase={index === activePhaseIndex}
              isEndEvent={isEndEvent}
              onClick={handlePhaseClick}
              showMore={showMore}
            />
            {isPivot && showMoreBtn}
          </div>
        );
      }),
    [activePhase, showMore, pivotIndex, filteredData, stageCount]
  );

  const renderIndicators = useMemo(() => {
    return (
      <div className="legend-container">
        <div className="indicators-container">
          <LeadTimeLegend filters={legend} setFilters={contextFilterHandler} />
          <div className="separator" />
          <LeadTimeFilterDropdown filters={filters} setFilters={setFilters} />
        </div>
        {showMore && (
          <AntButton className="less-btn" onClick={handleShowMoreClick}>
            View Less
          </AntButton>
        )}
      </div>
    );
  }, [dataKey, filters, showMore, legend, filteredData, stageCount]);

  const renderLines = useMemo(
    () =>
      showMore
        ? lines.map((line: any) => (
            <SteppedLineTo
              from={line.from}
              to={line.to}
              within={cx("widget", id)}
              borderColor="#BFBFBF"
              borderStyle="solid"
              className="stage-connection"
              toAnchor={"left"}
              fromAnchor={"right"}
              borderWidth={2}
              delay={0}
            />
          ))
        : [],
    [lines, showMore]
  );

  if (!data.length) {
    return <Empty></Empty>;
  }

  return (
    <div className="demo-phase-chart-wrapper">
      <div className="demo-phase-chart-container">
        <div ref={phaseContainerRef} className="demo-content-container">
          {renderSummary}
          {
            <DemoLeadTimeEventComponent
              stageId={`start`}
              title={"Ticket Created"}
              isStartEvent={true}
              isActive={activePhaseIndex === 0}
            />
          }
          {renderStages}
          {(showMore || data.length - pivotIndex < 1) && (
            <DemoLeadTimeEventComponent
              stageId={"end"}
              title={"end"}
              isEndEvent={true}
              isActive={false}
              showMore={showMore}
            />
          )}
        </div>
        {renderLines}
      </div>
      {renderIndicators}
    </div>
  );
};

export default DemoLeadTimePhaseChart;
