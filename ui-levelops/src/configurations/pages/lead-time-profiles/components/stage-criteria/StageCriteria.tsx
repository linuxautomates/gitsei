import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Dropdown, Icon, InputNumber, Menu, Popover } from "antd";
import { AcceptanceTimeUnit, RestVelocityConfigStage } from "classes/RestVelocityConfigs";
import { AntButton, AntFormItem, AntText, AntTitle } from "shared-resources/components";
import { AcceptanceTimeSlider } from "../index";
import { velocityIndicators } from "custom-hooks/helpers/leadTime.helper";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
interface StageCriteriaProps {
  stage: RestVelocityConfigStage;
  onChange: (data: any) => void;
}

const StageCriteria: React.FC<StageCriteriaProps> = props => {
  const { stage, onChange } = props;
  const [min, setMin] = useState(1);
  const [max, setMax] = useState(14);
  const [showRangePopover, setShowRangePopover] = useState(false);

  useEffect(() => {
    if (stage.lower_limit_value && min > stage.lower_limit_value) {
      setMin(stage.lower_limit_value);
    }

    if (stage.upper_limit_value && max < stage.upper_limit_value) {
      setMax(stage.upper_limit_value);
    }
  }, [stage]);

  const isHrs = stage?.lower_limit_unit === AcceptanceTimeUnit.MINUTES;

  const handleVisibleChange = useCallback(visible => {
    setShowRangePopover(visible);
  }, []);

  const handleChange = useCallback(
    (key: string) => {
      return (e: any) => {
        switch (key) {
          case "unit":
            onChange({
              ...stage.json,
              lower_limit_unit: e.key,
              upper_limit_unit: e.key
            });
            break;
          case "lower_limit_value":
          case "upper_limit_value":
            onChange({
              ...stage.json,
              [key]: e
            });
            break;
          case "ideal_time":
            if (e[0] < e[1]) {
              onChange({
                ...stage.json,
                lower_limit_value: e[0],
                upper_limit_value: e[1]
              });
            }
            break;
          case "min":
            if (e < max) {
              setMin(e);
            }

            break;
          case "max":
            if (e > min) {
              setMax(e);
            }

            break;
        }
      };
    },
    [onChange, stage, min, max]
  );

  const formatter = useCallback(
    value => {
      return isHrs ? `${value || 0} hrs` : `${value || 0} days`;
    },
    [stage]
  );

  const parser = useCallback(
    value => {
      return value ? value.replace(/\D/g, "") : "";
    },
    [stage]
  );

  const renderMenu = useMemo(
    () => (
      <Menu className="time-unit-select" onClick={handleChange("unit")}>
        <Menu.Item key={AcceptanceTimeUnit.DAYS}>Days</Menu.Item>
        <Menu.Item key={AcceptanceTimeUnit.MINUTES}>Hours</Menu.Item>
      </Menu>
    ),
    [stage, onChange]
  );

  const renderUnitSelect = useMemo(
    () => (
      <div className="stage-criteria-unit">
        <AntText>Set targets time by</AntText>
        <Dropdown overlay={renderMenu}>
          <a className="stage-unit-trigger" onClick={e => e.preventDefault()}>
            {stage?.lower_limit_unit === AcceptanceTimeUnit.MINUTES ? "Hours" : "Days"}{" "}
            <Icon type={"down"} theme={"outlined"} />
          </a>
        </Dropdown>
      </div>
    ),
    [stage, onChange]
  );

  const renderTimeSlider = useMemo(
    () => (
      <div className="acceptable-time-container">
        <AntFormItem key={`ideal-time`} label={"Ideal Time"}>
          <InputNumber
            min={min}
            max={(stage?.upper_limit_value || 11) - 1}
            value={stage?.lower_limit_value}
            onChange={handleChange("lower_limit_value")}
            formatter={formatter}
            parser={parser}
          />
        </AntFormItem>
        <AcceptanceTimeSlider
          values={[stage?.lower_limit_value, stage?.upper_limit_value]}
          onChange={handleChange("ideal_time")}
          min={min}
          max={max}
          stepSize={1}
          formatter={formatter}
        />
        <AntFormItem key={`acceptable-time`} label={"Acceptable Time"}>
          <InputNumber
            min={(stage?.lower_limit_value || 4) + 1}
            max={max}
            value={stage?.upper_limit_value}
            onChange={handleChange("upper_limit_value")}
            formatter={formatter}
            parser={parser}
          />
        </AntFormItem>
      </div>
    ),
    [stage, onChange, min, max]
  );

  const renderRangeMenu = useMemo(() => {
    return (
      <div className="range-menu">
        <AntText className="range-popover-title">Change min/max time</AntText>
        <AntFormItem key={`min-value`} label={"Minimum Time"}>
          <InputNumber
            min={1}
            max={max - 1}
            value={min}
            onChange={handleChange("min")}
            formatter={formatter}
            parser={parser}
          />
        </AntFormItem>
        <AntFormItem key={`max-value`} label={"Maximum Time"}>
          <InputNumber min={min + 1} value={max} onChange={handleChange("max")} formatter={formatter} parser={parser} />
        </AntFormItem>
      </div>
    );
  }, [min, max, stage]);

  const renderFooter = useMemo(
    () => (
      <div className="stage-criteria-footer">
        <Popover
          className={"range-popover"}
          content={renderRangeMenu}
          trigger="click"
          visible={showRangePopover}
          getPopupContainer={(trigger: any) => trigger.parentNode}
          onVisibleChange={handleVisibleChange}>
          <AntButton type="link">
            Edit Min/Max <Icon type="down" />
          </AntButton>
        </Popover>
        <div className="stage-criteria-legend">
          {velocityIndicators.map(item => (
            <div className="velocity-indicator">
              <div className="indicator-color" style={{ backgroundColor: item.color }} />
              <AntText className="indicator-title">{item.title}</AntText>
            </div>
          ))}
        </div>
      </div>
    ),
    [showRangePopover, max, min]
  );

  return (
    <div className="stage-criteria-container">
      <div className="stage-criteria-header">
        <AntText className="acceptable-limit-label">Set acceptable time limits for this stage</AntText>
      </div>
      <div className="stage-criteria-content">
        {renderUnitSelect}
        {renderTimeSlider}
        {renderFooter}
      </div>
    </div>
  );
};

export default StageCriteria;
