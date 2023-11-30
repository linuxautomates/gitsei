import { Empty, Statistic } from "antd";
import cx from "classnames";
import { getAvgTimeAndIndicator, velocityIndicators } from "custom-hooks/helpers/leadTime.helper";
import { newLeadTimeMetricOptions } from "dashboard/graph-filters/components/Constants";
import { WidgetFilterContext } from "dashboard/pages/context";
import { capitalize, findIndex, get, isEqual } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { Tooltip } from "antd";
import ChartLegendComponent from "shared-resources/charts/components/chart-legend/chart-legend.component";
import LeadTimeLegend from "shared-resources/charts/components/lead-time-stage-legend/LeadTimeLegend";
import { AntButton, AntCheckbox, AntTag, AntText } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import { PhaseChartProps } from "../../chart-types";
import LeadTimeFilterDropdown from "../../components/lead-time-filter-dropdown/LeadTimeFilterDropdown";
import LeadTimeEventComponent from "../components/lead-time-event";
import LeadTimePhaseItemNew from "../components/lead-time-phase-item-new.component";
import LeadTimeStageDurationMetricFilterDropdown from "../LeadTimeStageDurationMetricFilterDropdown";
import "./lead-time-phase-chart-new.style.scss";

const LeadTimePhaseChartNew = (props: PhaseChartProps) => {
  const { id, data, onClick, dataKey, hideKeys, widgetMetaData } = props;
  const { setFilters: setContextFilters, filters: contextFilters } = useContext(WidgetFilterContext);
  const defaultLegendValue = useMemo(() => {
    const defaultValue = { good: true, needs_attention: true, slow: true, missing: false };
    const ratings = get(contextFilters, [id, "ratings"], []);
    if (ratings.length) {
      const leftValues = { good: false, needs_attention: false, slow: false, missing: false };
      return {
        ...leftValues,
        ...ratings.reduce((acc: any, next: any) => {
          return { ...acc, [next]: true };
        }, {})
      };
    }
    return defaultValue;
  }, [contextFilters]);
  const [filteredData, setFilteredData] = useState([]);
  const [activePhase, setActivePhase] = useState<string | undefined>(undefined);
  const [filters, setFilters] = useState<any>({});
  const [legend, setLegend] = useState<any>(defaultLegendValue);
  const [clientWidth, setClientWidth] = useState<number>();
  const [pivotIndex, setPivotIndex] = useState(0);
  const [showMore, setShowMore] = useState(false);
  const [lines, setLines] = useState<any[]>([]);

  const phaseContainerRef = React.createRef<HTMLDivElement>();

  const stageCount = useMemo(() => (showMore ? filteredData.length : pivotIndex), [showMore, pivotIndex, filteredData]);

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
  }, [filteredData, clientWidth]);

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      const filterKeys = data.map((item: any) => item.name);

      const initialFilters = filterKeys.reduce((acc: any, next: any) => {
        return {
          ...acc,
          [next]: hideKeys && hideKeys.length ? !hideKeys.includes(next) : true
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
    setActivePhase(undefined);
  }, [filteredData]);

  const handlePhaseClick = useCallback(
    (stageName: any) => {
      setActivePhase(stageName);
      onClick && onClick(stageName);
    },
    [filteredData, onClick]
  );

  const handleShowMoreClick = useCallback(() => {
    setShowMore(!showMore);
  }, [showMore]);

  const activePhaseIndex = useMemo(() => {
    return findIndex(filteredData, (item: any) => item.name === activePhase);
  }, [filteredData, activePhase]);

  const summaryHint = useMemo(() => {
    return dataKey === "mean" ? "Average" : capitalize(dataKey);
  }, [dataKey]);

  const renderSummary = useMemo(() => {
    const { duration, rating, unit, backgroudColor, color } = getAvgTimeAndIndicator(filteredData);
    const phaseItem: any = newLeadTimeMetricOptions.find((val: any) => val.value === dataKey);
    return (
      <>
        <div className={cx("summary-container", `summary-container-${rating}`)}>
          <div
            className="summary-total"
            onClick={e => {
              e.preventDefault();
              handlePhaseClick("Total");
            }}>
            <AntText className="duration-title">
              <Tooltip title={phaseItem.tooltip}>{phaseItem.headerText}</Tooltip>
            </AntText>
            <AntText className="duration-value">{duration}</AntText>
            <AntText className="duration-unit">{unit}</AntText>
            <AntTag className="duration-review" color={color}>
              {toTitleCase(rating)}
            </AntTag>
          </div>
          <div className="summary-dropdown">
            <LeadTimeStageDurationMetricFilterDropdown key={props.id} selectedMetrics={dataKey} widgetId={props.id} />
          </div>
        </div>
      </>
    );
  }, [filteredData, summaryHint]);

  const showMoreBtn = useMemo(() => {
    const additionalStagesCount = filteredData.length - pivotIndex;

    if (showMore || additionalStagesCount < 1) {
      return null;
    }

    return (
      <div className="more-btn" onClick={handleShowMoreClick}>
        {`+ ${additionalStagesCount} MORE`}
      </div>
    );
  }, [showMore, filteredData, pivotIndex]);

  const renderStages = useMemo(
    () =>
      filteredData.slice(0, stageCount).map((stage: any, index: number) => {
        const isEndEvent = index === filteredData.length - 1;
        const isPivot = !showMore && index === pivotIndex - 1;
        return (
          <div className="stage-wrapper">
            <LeadTimePhaseItemNew
              phase={stage}
              onClick={handlePhaseClick}
              isActivePhase={index === activePhaseIndex}
              isEndEvent={isEndEvent}
            />
            {isPivot && showMoreBtn}
          </div>
        );
      }),
    [filteredData, activePhase, showMore, pivotIndex]
  );

  const contextFilterHandler = (filters: any) => {
    setLegend(filters);
    const ratings = Object.keys(filters).filter((key: string) => filters[key]);
    setContextFilters(id as string, { ...(contextFilters as any)?.[id], ratings });
  };

  const renderIndicators = useMemo(() => {
    return (
      <div className="legend-container">
        <div className="indicators-container">
          {/* <LeadTimeLegend filters={legend} setFilters={contextFilterHandler} /> */}
          {/* <div className="separator" />
          <LeadTimeFilterDropdown filters={filters} setFilters={setFilters} /> */}
        </div>
        {showMore && (
          <AntButton className="less-btn" onClick={handleShowMoreClick}>
            View Less
          </AntButton>
        )}
      </div>
    );
  }, [dataKey, filters, showMore, legend]);

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
              className={`stage-connection`}
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
    <div className="phase-chart-wrapper-new">
      <div className="phase-chart-container">
        <div ref={phaseContainerRef} className="content-container-new">
          {renderSummary}
          {
            <LeadTimeEventComponent
              stageId={`start`}
              title={widgetMetaData?.commit_created ? "Commit Created" : "Ticket Created"}
              isStartEvent={true}
              isActive={activePhaseIndex === 0}
            />
          }
          {renderStages}
          {(showMore || filteredData.length - pivotIndex < 1) && (
            <LeadTimeEventComponent stageId={"end"} title={"end"} isEndEvent={true} isActive={false} />
          )}
        </div>
        {renderLines}
      </div>
      {renderIndicators}
    </div>
  );
};

export default React.memo(LeadTimePhaseChartNew);
