import React, { useMemo } from "react";
import { Table, Badge } from "antd";
import { AntCard, AntText, AntPagination, SvgIcon } from "shared-resources/components";
import { Button, Tooltip } from "antd";

interface DemoFeatureDrillDownComponentProps {
  selectedFeature?: string;
  columns?: any;
  data?: any;
  isDevRawStatsDrilldown?: boolean;
  onDrilldownClose?: () => void;
  dashboardTimeRange?: string;
  nameForTitle?: string;
  extraPropsForGraph?: any;
  isDashboardDrilldown?: boolean;
  drilldown_count?: number;
}

const DemoFeatureDrillDownComponent: React.FC<DemoFeatureDrillDownComponentProps> = ({
  columns,
  selectedFeature,
  data,
  onDrilldownClose,
  nameForTitle,
  extraPropsForGraph,
  isDashboardDrilldown,
  drilldown_count
}) => {
  const devOverviewStyle = {
    padding: "25px",
    marginTop: "20px",
    border: "1px solid #d9d9d9"
  };

  const cardTitle = useMemo(() => {
    return (
      <div className="flex justify-space-between">
        <div className="flex">
          <div style={{ marginRight: ".3rem" }}>Drilldown Preview</div>
          <div style={{ marginRight: ".3rem" }}>
            <Badge
              style={{ backgroundColor: "var(--harness-blue)" }}
              count={drilldown_count ? drilldown_count || 0 : data?.length || 0}
            />
          </div>
        </div>
        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          <AntText className="drilldown-xAxis">Name</AntText>
          <span className="circle-dot-separator">{`● `}</span>
          <AntText className="drilldown-raw-title">{`${nameForTitle}`}</AntText>
          <Tooltip title="Close">
            <Button onClick={onDrilldownClose} className="drilldown-icon close-button">
              <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                <SvgIcon className="reports-btn-icon" icon="closeNew" />
              </div>
            </Button>
          </Tooltip>
        </div>
      </div>
    );
  }, [data]);

  const GraphComponent = extraPropsForGraph;
  return (
    <div className="feature-drilldown-container" style={isDashboardDrilldown ? undefined : devOverviewStyle}>
      <AntCard className="demoDrilldownPreview" title={cardTitle}>
        {extraPropsForGraph && GraphComponent}

        <Table
          dataSource={data}
          columns={columns}
          pagination={false}
          size={"middle"}
          // rowKey={"id"} // getting some weird error due to this (data is appending in the same table for different clicks)
          scroll={{ x: true }}
          style={{ padding: "12px 16px", overflow: "auto" }}
        />
        <AntPagination pageSize={10} current={1} total={45} showPageSizeOptions={true} disabled />
      </AntCard>
    </div>
  );
};

export default DemoFeatureDrillDownComponent;
