import React from "react";
import { Button, Icon, Select, Switch, Tabs, Tooltip } from "antd";
import { ENABLE_FACTOR_MSG, FACTOR_NAME_TO_ICON_MAPPING } from "./constant";
import ScorePopoverLegends from "dashboard/pages/scorecard/components/ScorePopoverLegends";
import { AntCheckbox, AntTable, SvgIcon } from "shared-resources/components";

export const trellisTableColumns = (
  setShowConfigFactorModal: (param: any) => void,
  setShowMetricEditModal: (param: any) => void,
  featureMetricUpdate: (name: string, key: string, value: any) => void,
  setCurrentMetricEditFeature: (param: any) => void
) => {
  return [
    {
      title: "Metrics",
      dataIndex: "metric",
      key: "metric",
      render: (metric: any) => (
        <span className="metric-row">
          <Tooltip title={`${!metric?.sectionEnabled ? ENABLE_FACTOR_MSG : ""}`}>
            <Switch
              checked={metric?.enabled}
              onChange={() => featureMetricUpdate(metric?.name, "enabled", !metric?.enabled)}
              disabled={!metric?.sectionEnabled}
            />
          </Tooltip>
          <span className="label">{metric?.name}</span>
        </span>
      ),
      align: "left",
      sorter: (a: any, b: any) => a.metric?.name.localeCompare(b.metric?.name)
    },
    {
      title: "Factor",
      dataIndex: "factor",
      key: "factor",
      render: (factor: any) => (
        <div className="factor">
          <div className="factor-icon-name">
            <SvgIcon icon={FACTOR_NAME_TO_ICON_MAPPING?.[factor?.name] || ""} />
            <span className="label">{factor?.name}</span>
          </div>
          <div className="factor-weight">
            <span className="label">Weightage : {factor?.weight}</span>
            <Icon type="edit" onClick={setShowConfigFactorModal} />
          </div>
        </div>
      ),
      align: "left",
      sorter: (a: any, b: any) => a.factor?.name.localeCompare(b.factor?.name),
      width: 150
    },
    {
      title: "Range",
      dataIndex: "range",
      key: "range",
      render: (range: any) => (
        <ScorePopoverLegends
          asc={false}
          maxScore={range?.max_value}
          unit={range?.feature_unit}
          value={[range?.lower_limit_percentage, range?.upper_limit_percentage]}
        />
      ),
      align: "left"
    },
    {
      title: "",
      dataIndex: "edit",
      key: "edit",
      render: (feature: any) => (
        <Tooltip title={`${!feature?.sectionEnabled ? ENABLE_FACTOR_MSG : ""}`}>
          <Button
            type="link"
            icon="edit"
            onClick={() => {
              setCurrentMetricEditFeature(feature);
              setShowMetricEditModal(true);
            }}
            disabled={!feature?.sectionEnabled}>
            {" "}
            Edit Metrics
          </Button>
        </Tooltip>
      ),
      align: "left"
    }
  ];
};
