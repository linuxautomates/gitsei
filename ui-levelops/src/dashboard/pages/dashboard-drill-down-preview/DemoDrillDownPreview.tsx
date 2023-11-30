import { Col, Row } from "antd";
import React, { useMemo } from "react";
import { AntCard, AntText, AntPagination } from "shared-resources/components";
import { SvgIcon } from "shared-resources/components";
import { Button, Tooltip, Badge } from "antd";
import DemoLeadTimeByStageFooter from "./DemoLeadTimeBStageFooter";
import { Table } from "antd";
import "./DashboardDrillDownPreview.style.scss";
import "./DemoDrillDownPreview.style.scss";
import { DemoDrillDownProps } from "../dashboard-view/Demo-Dashboard/Types/drilldown.types";
import DemoDevRawStatsDrilldownWrapper from "./components/demo-dev-raw-stats-drilldown-component/DemoDevRawStatsDrilldownWrapper";

interface DemoDrillDownPreviewProps {
  drillDownProps: DemoDrillDownProps;
  onDrilldownClose: () => void;
  widgetId?: string;
  title?: string;
  columnsConfig?: any;
  reportType?: string;
}

const DemoDrillDownPreview: React.FC<DemoDrillDownPreviewProps> = (props: DemoDrillDownPreviewProps) => {
  const { drillDownProps, onDrilldownClose, widgetId, title, columnsConfig, reportType } = props;
  const { data, xAxis, currentAllocation } = drillDownProps;
  const getDrilldownFooter = () => {
    return React.createElement(DemoLeadTimeByStageFooter, null, null);
  };
  const unsupportedDrilldown = useMemo(() => {
    return currentAllocation;
  }, [currentAllocation]);

  const cardTitle = useMemo(() => {
    return (
      <div className="flex align-center">
        <div style={{ marginRight: ".3rem" }}>Drilldown Preview</div>
        <div style={{ marginRight: ".3rem" }}>
          <Badge
            style={{ backgroundColor: "var(--harness-blue)" }}
            count={drillDownProps?.drilldown_count ? drillDownProps?.drilldown_count || 0 : data?.length || 0}
          />
        </div>
      </div>
    );
  }, [data, drillDownProps]);

  if (unsupportedDrilldown) {
    return (
      <div className="w-100" style={{ padding: 16, minHeight: "150px" }} id={`${widgetId}-drilldown`}>
        <AntCard className="demoDrilldownPreview" title={cardTitle}>
          <Row style={{ marginTop: "-59px" }}>
            <div className={"demo-drilldown-action"}>
              <Tooltip title="Close">
                <Button onClick={onDrilldownClose} className="drilldown-icon close-button">
                  <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                    <SvgIcon className="reports-btn-icon" icon="closeNew" />
                  </div>
                </Button>
              </Tooltip>
            </div>
            <div className="demo-no-drilldown">Drilldown is not available</div>
          </Row>
        </AntCard>
      </div>
    );
  }

  return (
    <div className="w-100" style={{ padding: 16, minHeight: "300px" }} id={`${widgetId}-drilldown`}>
      {reportType === "individual_raw_stats_report" ? (
        <DemoDevRawStatsDrilldownWrapper
          onDrilldownClose={onDrilldownClose}
          drillDownProps={drillDownProps}
          columns={columnsConfig}
          widgetId={widgetId}
        />
      ) : (
        <AntCard className="demoDrilldownPreview" title={cardTitle}>
          <Row style={{ marginTop: "-59px" }}>
            <Col span={24} className={"demo-drilldown-action"}>
              <Tooltip title="Close">
                <Button onClick={onDrilldownClose} className="drilldown-icon close-button">
                  <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                    <SvgIcon className="reports-btn-icon" icon="closeNew" />
                  </div>
                </Button>
              </Tooltip>
              <div className="flex align-center">
                <AntText className="drilldown-xAxis">{`${(xAxis ?? "").toUpperCase()} `}</AntText>
                <span className="circle-separator">{`● `}</span>
                <AntText className="drilldown-title">{`${title}`}</AntText>
              </div>
            </Col>
          </Row>
          <div>
            <Table
              dataSource={data}
              columns={columnsConfig}
              pagination={false}
              size={"middle"}
              // rowKey={"id"} // getting some weird error due to this (data is appending in the same table for different clicks)
              scroll={{ x: true }}
              style={{ padding: "12px 16px", overflow: "auto" }}
            />
            <AntPagination pageSize={10} current={1} total={45} showPageSizeOptions={true} disabled />
            {reportType === "lead_time_by_stage_report" && (
              <Row type={"flex"} align={"bottom"} className={"demo-drilldown-footer"}>
                {getDrilldownFooter()}
              </Row>
            )}
          </div>
        </AntCard>
      )}
    </div>
  );
};

export default DemoDrillDownPreview;
