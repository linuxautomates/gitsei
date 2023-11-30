import React, { useCallback, useMemo, useState } from "react";
import { AntTable } from "shared-resources/components";
import "./demo-effort-progress.style.scss";
import CustomCarousalComponent from "shared-resources/components/custom-carousal/CustomCarousalComponent";
import { DemoEffortProgressChartProps } from "../Widget-Grapg-Types/demo-effort-progress.types";
import { JiraProgressTableConfig } from "shared-resources/charts/jira-charts/jira-progress.table-config";
import { DEMO_LIMIT_PER_PAGE } from "./constant";

const DemoEffortProgressChart: React.FC<DemoEffortProgressChartProps> = (props: DemoEffortProgressChartProps) => {
  let { data, across, onClick, id } = props;

  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const next = useCallback(() => setCurrentIndex(prev => prev + 1), []);
  const prev = useCallback(() => setCurrentIndex(prev => prev - 1), []);

  const range = useMemo(() => {
    if (!(data || []).length) return { high: 0, low: 0 };
    const page = currentIndex + 1;
    const high = DEMO_LIMIT_PER_PAGE * page;
    const low = high - DEMO_LIMIT_PER_PAGE + 1;
    return { high: Math.min(high, data.length), low };
  }, [currentIndex, data]);

  const getFiltereddata = useMemo(() => {
    const { high, low } = range;
    if (low === 0) return [];
    return data.slice(low - 1, high);
  }, [currentIndex, range]);

  const getColumns = useMemo(() => {
    if (across === "ticket_category") {
      return JiraProgressTableConfig.filter(config => config.dataIndex !== "priority");
    }
    return JiraProgressTableConfig;
  }, [across]);

  const handleRowClick = useCallback(
    (key: string) => {
      onClick({ widgetId: id, name: key, phaseId: key });
    },
    [onClick]
  );

  const setRowProps = useCallback(
    (record: any, index: number) => ({
      onClick: (e: any) => handleRowClick(record?.id)
    }),
    [handleRowClick]
  );

  return (
    <>
      <div className="demo_effort_progress_chart_container">
        <AntTable dataSource={getFiltereddata} columns={getColumns} pagination={false} onRow={setRowProps} />
      </div>
      <div className="demo-carousal-container">
        <CustomCarousalComponent
          totalCount={(data || []).length}
          range={range}
          showActionButtons={true}
          perPage={DEMO_LIMIT_PER_PAGE}
          dotLimitingFactor={Math.min(32, data.length)}
          currentIndex={currentIndex}
          handleNext={next}
          handlePrev={prev}
        />
      </div>
    </>
  );
};

export default DemoEffortProgressChart;
