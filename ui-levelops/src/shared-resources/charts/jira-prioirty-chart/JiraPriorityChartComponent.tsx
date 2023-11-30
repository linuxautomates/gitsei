import { Alert, Icon, Popover } from "antd";
import React, { useCallback, useMemo, useState } from "react";
import { AntButton, AntTable } from "shared-resources/components";
import CustomCarousalComponent from "shared-resources/components/custom-carousal/CustomCarousalComponent";
import { JiraPriorityChartProps } from "../chart-types";
import { jiraPriorityChartTableConfig } from "./jiraPrioirtyChartTableConfig";
import "./jiraPriorityChart.styles.scss";
import { jiraPriorityChartMockData } from "./jiraPriorityChartMock";

const LIMIT_PER_PAGE = 4;

const JiraPriorityChartComponent: React.FC<JiraPriorityChartProps> = (props: JiraPriorityChartProps) => {
  const { data: apiData, onClick } = props;
  // const apiData = jiraPriorityChartMockData;
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const next = useCallback(() => setCurrentIndex(prev => prev + 1), []);
  const prev = useCallback(() => setCurrentIndex(prev => prev - 1), []);
  const range = useMemo(() => {
    if (!(apiData || []).length) return { high: 0, low: 0 };
    const page = currentIndex + 1;
    const high = LIMIT_PER_PAGE * page;
    const low = high - LIMIT_PER_PAGE + 1;

    return { high: Math.min(high, apiData.length), low };
  }, [currentIndex, apiData]);

  const getFiltereddata = useMemo(() => {
    const { high, low } = range;
    if (low === 0) return [];
    return (apiData || []).slice(low - 1, high);
  }, [currentIndex, range]);

  // const renderPopOverContent = useMemo(() => {
  //   const hiddenalerts = alerts.slice(1);
  //   return hiddenalerts.map(alert => {
  //     return (
  //       <div className="popover-content">
  //         <Icon className="popover-alert-icon" type="info-circle" />
  //         <p className="popover-alert-text">{alert}</p>
  //       </div>
  //     );
  //   });
  // }, []);

  const handleRowClick = useCallback(
    (key: string) => {
      onClick && onClick(key);
    },
    [onClick]
  );

  const setRowProps = useCallback(
    (record: any, index: number) => ({
      onClick: (e: any) => handleRowClick(record?.name)
    }),
    [handleRowClick]
  );

  // const renderAlertMessage = useMemo(() => {
  //   return (
  //     <div className="alert-container">
  //       <p className="visible-alert">{alerts[0]}</p>
  //       {alerts.length > 1 && (
  //         <Popover placement="bottomRight" content={renderPopOverContent} trigger="click">
  //           <AntButton className="alert-button">{`+ ${alerts.length - 1}`}</AntButton>
  //         </Popover>
  //       )}
  //     </div>
  //   );
  // }, []);

  return (
    <div className="jira-priority-chart-container">
      {/* {alerts.length > 0 && (
        <div>
          <Alert message={renderAlertMessage} type="error" />
        </div>
      )} */}
      <div className="jira-priority-chart-container-table">
        <AntTable
          dataSource={getFiltereddata}
          columns={jiraPriorityChartTableConfig(apiData)}
          pagination={false}
          onRow={setRowProps}
        />
      </div>
      <div className="carousal-container">
        <CustomCarousalComponent
          showActionButtons={true}
          range={range}
          totalCount={apiData.length}
          perPage={LIMIT_PER_PAGE} // showing hardcoded fornow
          dotLimitingFactor={Math.min(32, (apiData || []).length)}
          currentIndex={currentIndex}
          handleNext={next}
          handlePrev={prev}
        />
      </div>
    </div>
  );
};

export default JiraPriorityChartComponent;
