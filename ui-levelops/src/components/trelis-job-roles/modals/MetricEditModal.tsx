import React, { useEffect, useMemo, useState } from "react";
import { Button, Divider, Icon, Modal, Select, Switch } from "antd";
import "./MetricEditModal.scss";
import ScorePopoverLegends from "dashboard/pages/scorecard/components/ScorePopoverLegends";
import { ERROR_MSG_FOR_LOWER_METRIC, FACTOR_NAME_TO_ICON_MAPPING, METRIC_LABELS } from "../constant";
import { cloneDeep } from "lodash";
import { FEATURES_WITH_EFFORT_PROFILE } from "configurations/pages/TrellisProfile/constant";
import { AntCheckboxComponent } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { AntInput, AntSelect, AntTooltip, SvgIcon } from "shared-resources/components";

export interface MetricEditModalProps {
  visible: boolean;
  onClose: () => void;
  data: any;
  featureMetricUpdate: (feature: Record<any, any>, key: string, value: any, objectFlag: boolean) => void;
  ticketCategoryId: string;
  ticketCategorizationData: any;
}

const { Option } = Select;

export const MetricEditModal: React.FC<MetricEditModalProps> = ({
  data,
  visible = false,
  onClose,
  featureMetricUpdate,
  ticketCategoryId,
  ticketCategorizationData
}) => {
  const [currentMetrics, setCurrentMetrics] = useState<any>(undefined);

  useEffect(() => {
    setCurrentMetrics(cloneDeep(data));
  }, []);

  const updateMetrics = (key: string, value: any) => {
    const newMetrics = cloneDeep(currentMetrics);
    newMetrics[key] = value;
    setCurrentMetrics(newMetrics);
  };

  const resetToDefault = () => {
    const newMetrics = cloneDeep(currentMetrics);
    newMetrics.lower_limit_percentage = data?.lower_limit_percentage;
    newMetrics.upper_limit_percentage = data?.upper_limit_percentage;
    setCurrentMetrics(newMetrics);
  };

  const categoryOptions = useMemo(() => {
    if (!ticketCategoryId) {
      return [];
    }
    const config = ticketCategorizationData?.find((ticket: any) => ticket?.id === ticketCategoryId)?.config;
    return !config
      ? []
      : Object.values(config?.categories || {}).map((category: any) => ({
          value: category?.id,
          label: category?.name
        }));
  }, [ticketCategorizationData]);

  const metricValidation = useMemo(() => {
    return currentMetrics?.lower_limit_percentage > currentMetrics?.upper_limit_percentage;
  }, [currentMetrics]);

  return (
    <Modal
      title={""}
      visible={visible}
      className="edit-metric"
      cancelText="Cancel"
      okText="Save"
      width={"42rem"}
      maskClosable={false}
      onCancel={onClose}
      footer={[
        <Button
          type="primary"
          disabled={metricValidation}
          onClick={() => {
            delete currentMetrics?.sectionName;
            featureMetricUpdate(currentMetrics, "", false, true);
            onClose();
          }}>
          Save
        </Button>,
        <Button
          key="submit"
          onClick={() => {
            featureMetricUpdate(data, "", false, true);
            onClose();
          }}>
          Cancel
        </Button>
      ]}>
      <div className="flex metric-title">
        <div className="label-info">
          <SvgIcon icon={FACTOR_NAME_TO_ICON_MAPPING[currentMetrics?.sectionName]} />
          <span className="label">{currentMetrics?.sectionName}</span>
        </div>
        <div className="title">
          <div>
            <Switch
              checked={currentMetrics?.enabled}
              onChange={() => updateMetrics("enabled", !currentMetrics?.enabled)}
            />
          </div>
          <div className="label">{currentMetrics?.name} </div>
        </div>
      </div>
      <div className="metric-row">
        <label>{METRIC_LABELS?.[currentMetrics?.name]?.maxLabel}</label>
        <div className="input">
          <AntInput
            value={currentMetrics?.max_value}
            min={0}
            type="number"
            onChange={(e: any) => updateMetrics("max_value", e || 0)}
          />
          <Button type="link" icon="undo" onClick={() => updateMetrics("max_value", data?.max_value)}>
            Reset to Default
          </Button>
        </div>
      </div>
      <div className="metric-row">
        <label>What is the acceptable range?</label>
        <div className="input">
          <AntInput
            value={currentMetrics?.lower_limit_percentage}
            min={0}
            type="number"
            onChange={(e: any) => updateMetrics("lower_limit_percentage", e || 0)}
            className={`${metricValidation ? "error-msg" : ""}`}
          />
          <span className="to">to</span>{" "}
          <AntInput
            value={currentMetrics?.upper_limit_percentage}
            min={0}
            type="number"
            onChange={(e: any) => updateMetrics("upper_limit_percentage", e || 0)}
          />
          <Button type="link" icon="undo" onClick={() => resetToDefault()}>
            Reset to Default
          </Button>
        </div>
        {metricValidation && <div className="error-msg">{ERROR_MSG_FOR_LOWER_METRIC} </div>}
      </div>
      <div className="metric-row">
        <label>Preview</label>
        <ScorePopoverLegends
          asc={false}
          maxScore={currentMetrics?.max_value}
          unit={currentMetrics?.feature_unit}
          value={[currentMetrics?.lower_limit_percentage, currentMetrics?.upper_limit_percentage]}
        />
        <div className="note">
          <Divider type="vertical" />
          <div>💡 Note: {METRIC_LABELS?.[currentMetrics?.name]?.note}</div>
        </div>
      </div>
      {FEATURES_WITH_EFFORT_PROFILE.includes(currentMetrics?.type!) && (
        <div className="metric-row">
          <label>Categories</label>
          <div className="select">
            <AntTooltip
              placement="topRight"
              title={!ticketCategoryId ? "Please select effort investment profile from profile Settings" : ""}>
              <AntSelect
                className="effort-investment-category-selector"
                mode="multiple"
                value={currentMetrics?.ticket_categories}
                optionLabelProp="label"
                disabled={!ticketCategoryId}
                placeholder={"Select Categories"}
                onChange={(value: any) => {
                  updateMetrics("ticket_categories", value);
                }}>
                {(categoryOptions || []).map((option: { value: string; label: string }) => (
                  <Option value={option.value} label={option.label}>
                    <AntCheckboxComponent
                      className="effort-investment-category-selector-check"
                      onClick={(e: any) => e.preventDefault()}
                      checked={currentMetrics.ticket_categories?.includes(option.value)}
                    />
                    {option.label}
                  </Option>
                ))}
              </AntSelect>
            </AntTooltip>
          </div>
        </div>
      )}
    </Modal>
  );
};
