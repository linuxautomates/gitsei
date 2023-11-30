import { Empty } from "antd";
import cx from "classnames";
import { findIndex, isEqual } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { SteppedLineTo } from "react-lineto";
import { DoraLeadTimeStageChartProps } from "shared-resources/charts/chart-types";
import { velocityIndicators } from "custom-hooks/helpers/leadTime.helper";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import DoraStageFilterDropdown from "shared-resources/charts/dora-chart/dora-stage-stat-chart/components/DoraStageLegend/DoraStageLegendComponent";
import LeadTimeEventComponent from "shared-resources/charts/lead-time-phase-chart/components/lead-time-event";
import { WidgetDrilldownHandlerContext, WidgetFilterContext } from "dashboard/pages/context";
import "./DoraStageChart.style.scss";
import DoraStageComponent from "../components/DoraStageComponent";
import { getAllStages } from "../components/helper";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";

const DoraStageChart: React.FC<DoraLeadTimeStageChartProps> = (props: DoraLeadTimeStageChartProps) => {
  const { id, data, onDoraClick, dataKey, widgetMetaData, metrics, dateRangeValue, workflowProfile, activePhase } =
    props;
  const { setFilters: setContextFilters, filters: contextFilters } = useContext(WidgetFilterContext);
  const { isDrilldownOpen, setDrilldown, drilldownWidgetId } = useContext(WidgetDrilldownHandlerContext);

  const [filters, setFilters] = useState<any>({});
  const [clientWidth, setClientWidth] = useState<number>();
  const [pivotIndex, setPivotIndex] = useState(0);
  const [showMore, setShowMore] = useState(false);
  const [lines, setLines] = useState<any[]>([]);

  const phaseContainerRef = React.createRef<HTMLDivElement>();

  const stageCount = useMemo(() => (showMore ? data.length : pivotIndex), [showMore, pivotIndex, data]);

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
      for (let i = 0; i < data.length; i++) {
        if (i === 0) {
          // 200 is the width of a individual stage
          // 105 is the width of the +more
          // 85 is the width of the start
          stagesWidth += 200 + 85 + 105;
        } else {
          stagesWidth += 200 + 105;
        }
        if (stagesWidth > containerWidth) {
          if (!pivot) {
            pivot = i;
          }
        } else {
          stagesWidth = stagesWidth - 105;
        }
        if (i !== data.length && data[i + 1]) {
          const nextStage: any = data[i + 1];
          const currentStage: any = data[i];
          connections.push({
            from: cx("right-line", currentStage?.id),
            to: cx("left-line", nextStage?.id)
          });
        }
      }
      if (!pivot) {
        setPivotIndex(data.length);
      } else {
        setPivotIndex(pivot);
      }
      setLines(connections);
    }
  }, [data, clientWidth]);

  useEffect(() => {
    const allStages = getAllStages(workflowProfile as RestWorkflowProfile);
    if ((allStages || []).length) {
      const initialFilters = allStages.reduce(
        (acc: any, next: any) => ({
          ...acc,
          [next]: true
        }),
        {}
      );
      setFilters(initialFilters);
    }
  }, [workflowProfile]);

  const handleShowMoreClick = useCallback(() => {
    setShowMore(!showMore);
  }, [showMore]);

  const activePhaseIndex = useMemo(() => {
    return findIndex(data, (item: any) => item.key === activePhase);
  }, [data, activePhase]);

  const showMoreBtn = useMemo(() => {
    const additionalStagesCount = data.length - pivotIndex;

    if (showMore || additionalStagesCount < 1) {
      return null;
    }

    return (
      <div className="more-btn" onClick={handleShowMoreClick}>
        <div className="end-upper-node-arrow"></div>
        <div className="end-lower-node-arrow"></div>
        {`+ ${additionalStagesCount} MORE`}
      </div>
    );
  }, [showMore, data, pivotIndex]);

  const renderStages = useMemo(
    () =>
      data.slice(0, stageCount).map((stage: any, index: number) => {
        const isEndEvent = index === data.length - 1;
        const isPivot = !showMore && index === pivotIndex - 1;

        return (
          <div className="stage-wrapper">
            <DoraStageComponent
              stage={stage}
              onClick={onDoraClick}
              isActivePhase={index === activePhaseIndex && isDrilldownOpen}
              isEndEvent={isEndEvent}
              buckets={stage?.pieData || []}
              dataKey="count"
              metrics={metrics}
            />
            {isPivot && showMoreBtn}
          </div>
        );
      }),
    [data, activePhase, showMore, pivotIndex, metrics, isDrilldownOpen]
  );

  const setLegendFilters = (value: any) => {
    if (id === drilldownWidgetId) {
      setDrilldown(undefined);
    }
    setFilters(value);
    const exclude_stages = Object.keys(value).filter((item: string) => !value[item]);
    setContextFilters(id as string, { ...(contextFilters as any)?.[id], exclude_stages });
  };

  const renderIndicators = useMemo(() => {
    return (
      <div className="legend-container">
        <div className="indicators-container">
          {
            <div className="indicators-container">
              {velocityIndicators.map(item => (
                <div className="indicator-item">
                  <div className="indicator-color" style={{ backgroundColor: item.color }} />
                  <AntText className="indicator-title">{item.title}</AntText>
                </div>
              ))}
            </div>
          }
          <div className="separator" />
          <DoraStageFilterDropdown filters={filters} setFilters={setLegendFilters} />
        </div>
        {showMore && (
          <AntButton className="less-btn" onClick={handleShowMoreClick}>
            View Less
          </AntButton>
        )}
      </div>
    );
  }, [dataKey, filters, showMore, drilldownWidgetId]);

  const renderLines = useMemo(
    () =>
      showMore
        ? lines?.map((line: any) => (
            <SteppedLineTo
              from={line.from}
              to={line.to}
              within={cx("widget", id)}
              borderColor="#DCDFE4"
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
    <div className="dora-stage-chart-wrapper">
      <div className="dora-stage-chart-container">
        <div ref={phaseContainerRef} className="content-container-new">
          {
            <LeadTimeEventComponent
              stageId={`start`}
              title={widgetMetaData?.commit_created ? "Commit Created" : "Ticket Created"}
              isStartEvent={true}
              isActive={activePhaseIndex === 0}
            />
          }
          {renderStages}
          {(showMore || data.length - pivotIndex < 1) && (
            <LeadTimeEventComponent stageId={"end"} title={"end"} isEndEvent={true} isActive={false} />
          )}
        </div>
        {renderLines}
      </div>
      {renderIndicators}
    </div>
  );
};

export default DoraStageChart;
