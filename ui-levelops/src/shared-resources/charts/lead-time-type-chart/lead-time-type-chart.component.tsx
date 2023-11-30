import React, { useCallback, useEffect, useMemo, useState } from "react";
import { get } from "lodash";
import {
  getTimeAndIndicator,
  leadTimeMetricsMapping,
  prVolumeIndicator,
  velocityIndicators
} from "custom-hooks/helpers/leadTime.helper";
import { AntTable, AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { velocityTableConfig, stageColumn } from "./tableConfig";
import { LeadTimeTypeChartProps } from "../chart-types";
import CarouselWrapper from "../components/carousel-wrapper/CarouselWrapper";
import "./lead-time-type-chart.style.scss";
import { Icon } from "antd";
import LeadTimeFilterDropdown from "../components/lead-time-filter-dropdown/LeadTimeFilterDropdown";

const LeadTimeTypeChartComponent: React.FC<LeadTimeTypeChartProps> = props => {
  const { data, onClick, hideKeys, dataKey } = props;

  const [filteredData, setFilteredData] = useState([]);
  const [filters, setFilters] = useState<any>({});
  const [activeCell, setActiveCell] = useState<string | undefined>(undefined);
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    if ((data || []).length) {
      setFilteredData(data as any);
      let filterKeys: string[] = [];
      if (data && data.length) {
        filterKeys = Object.keys(data[0] || {}).filter(key => key !== "name");
      }

      const initialFilters = filterKeys.reduce((acc: any, next: any) => {
        return {
          ...acc,
          [next]: hideKeys && hideKeys.length ? !hideKeys.includes(next) : true
        };
      }, {});
      setFilters(initialFilters as any);
    }
  }, [data, hideKeys]);

  useEffect(() => {
    if (filters) {
      const activeFilters = Object.keys(filters).filter(key => !!filters[key]);
      const newData = data.map((item: any) => {
        const keys = Object.keys(item).filter(key => key !== "name");
        const stageData = keys.reduce((acc: any, next: any) => {
          if (activeFilters.includes(next)) {
            return {
              ...acc,
              [next]: item[next]
            };
          }
          return acc;
        }, {});

        return {
          name: item.name,
          ...stageData
        };
      });
      setRefresh(prev => prev + 1);
      setFilteredData(newData as any);
    }
  }, [filters]);

  const headers = useMemo(
    () => (filteredData && filteredData.length ? Object.keys(filteredData[0] || {}).filter(key => key !== "name") : []),
    [filteredData]
  );

  const velocityData = useMemo(() => {
    return (filteredData || []).map((item: any) => {
      const stageKeys = Object.keys(item);
      const stageData = stageKeys
        .filter(key => key !== "name")
        .reduce(
          (acc: any, next: any) => ({
            duration: acc["duration"] + get(item, [next, "duration"], 0),
            lower_limit: acc["lower_limit"] + get(item, [next, "lower_limit"], 0),
            upper_limit: acc["upper_limit"] + get(item, [next, "upper_limit"], 0)
          }),
          { duration: 0, lower_limit: 0, upper_limit: 0 }
        );

      const duration = stageData.duration;
      const lower_limit = stageData.lower_limit;
      const upper_limit = stageData.upper_limit;

      return {
        name: item.name,
        avg_velocity: getTimeAndIndicator(duration, lower_limit, upper_limit)
      };
    });
  }, [filteredData]);

  const handleTaskClick = useCallback((taskType: string) => {
    setActiveCell(taskType);
    onClick &&
      onClick({
        taskType
      });
  }, []);

  const handleStageClick = useCallback((taskType: string, stageName: string) => {
    return () => {
      setActiveCell(`${taskType}_${stageName}`);
      onClick &&
        onClick({
          taskType,
          stageName
        });
    };
  }, []);

  const getRowClassName = useCallback(
    (record: any, index: number) => {
      const activeTask = activeCell?.split("_")[0];
      if (record.name === activeTask) {
        return "active-velocity-row";
      }
      return undefined;
    },
    [activeCell]
  );

  const stageTableColumns = useMemo(() => {
    const stageColumns = headers.map(stage => {
      return stageColumn(stage, handleStageClick, activeCell);
    });

    return [
      baseColumnConfig("", "padding_left", { width: 64 }),
      ...stageColumns,
      baseColumnConfig("", "padding_right", { width: 64 })
    ];
  }, [filteredData, activeCell]);

  const renderStages = useMemo(
    () => (
      <CarouselWrapper className="stage-container" refresh={refresh}>
        <AntTable
          className="stage-table"
          bordered={false}
          columns={stageTableColumns}
          dataSource={filteredData}
          pagination={false}
          rowClassName={getRowClassName}
        />
      </CarouselWrapper>
    ),
    [filteredData, refresh, activeCell]
  );

  const renderContent = useMemo(
    () => (
      <div className="content-container">
        <div className="velocity-container">
          <AntTable
            className="velocity-table"
            bordered={false}
            columns={velocityTableConfig(handleTaskClick)}
            dataSource={velocityData}
            pagination={false}
            rowClassName={getRowClassName}
          />
        </div>
        <div className="v-line" />
        {renderStages}
      </div>
    ),
    [filteredData, refresh, activeCell]
  );

  const renderIndicators = useMemo(() => {
    return (
      <div className="legend-container">
        <div className="indicators-container">
          <Icon type="info-circle" theme="outlined" style={{ fontSize: "14px" }} />
          <AntText className="metric-label">{get(leadTimeMetricsMapping, dataKey, "Average Time")}</AntText>
          {velocityIndicators.map(item => (
            <div className="indicator-item">
              <div className="indicator-color" style={{ backgroundColor: item.color }} />
              <AntText className="indicator-title">{item.title}</AntText>
            </div>
          ))}
        </div>
        <div className="indicators-container">
          <AntText className="indicator-label">PR Volume</AntText>
          {prVolumeIndicator.map(item => (
            <div className="indicator-item">
              <AntText className="indicator-title">
                {
                  <>
                    <b>{item.title}</b>
                    {item.hint}
                  </>
                }
              </AntText>
            </div>
          ))}
          <div className="separator" />
          <LeadTimeFilterDropdown filters={filters} setFilters={setFilters} />
        </div>
      </div>
    );
  }, [filters, dataKey]);

  return (
    <div className="lead-time-type-chart-container">
      {renderContent}
      {renderIndicators}
    </div>
  );
};

export default LeadTimeTypeChartComponent;
