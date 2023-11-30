import React, { useCallback, useMemo } from "react";
import { Form } from "antd";
import { RestVelocityConfigStage, TriggerEventType } from "classes/RestVelocityConfigs";
import { AntCol, AntForm, AntFormItem, AntInput, AntRadio, AntRadioGroup, AntRow, AntSwitch } from "shared-resources/components";

interface StageInfoProps {
  stage: RestVelocityConfigStage;
  onChange: (stage: any) => void;
  isFixedStage?: boolean;
}

const StageInfoComponent: React.FC<StageInfoProps> = props => {
  const { stage, onChange, isFixedStage } = props;

  const memoizedGutter = useMemo(() => [16, 16], []);
  const memoizedAutoSize = useMemo(() => ({ minRows: 1, maxRows: 5 }), []);

  const handleChange = useCallback(
    (key: string) => {
      return (e: any) => {
        (stage as any)[key] = e.target.value;
        onChange(stage.json);
      };
    },
    [onChange]
  );

  const handleEnableChange = useCallback(
    (value: boolean) => {
      (stage as any)["enabled"] = value;
      onChange(stage.json);
    },
    [onChange]
  );

  const handleRadioChange = useCallback(
    (value: boolean, key: string) => {
      (stage as any)["event"]['params'] = {
        ...(stage as any)["event"]['params'],
        [key]: [value]
      };
      onChange(stage.json);
    },
    [onChange]
  );

  const stageEnableStyle = useMemo(
    () => ({ color: "black", fontWeight: 600, marginRight: "8px", fontSize: "12px" }),
    []
  );

  const releaseCalculationOptions: any[] = [
    {
      label: "Consider the earliest released version",
      value: 'min',
    },
    {
      label: "Consider the latest released version",
      value: 'max',
    },
  ];

  const approvalTimeCalculationOptions: any[] = [
    {
      label: "Consider the first approval",
      value: 'min',
    },
    {
      label: "Consider the final approval",
      value: 'max',
    },
  ];

  const renderRadioButton = useCallback(
    (key: string) => {

      const renderValue = key === 'prefer_release' ? releaseCalculationOptions : approvalTimeCalculationOptions;
      const renderSelection = renderValue.map((option: any) => {
        return (
          <AntRadio value={option.value}>{option.label}</AntRadio>
        );
      })
      const selectedValue = stage.event?.params?.[key] ? stage.event?.params?.[key][0] : 'min'
      const noteMessage = key === 'prefer_release'
        ? 'How would you want us to calculate the release time in case of multiple versions associated with a single ticket?'
        : 'How would you want us to calculate the PR approval time?'

      return (
        <AntRadioGroup value={selectedValue} onChange={(e: any) => handleRadioChange(e.target.value, key)} className="radio-button-filed">
          <p>{noteMessage}</p>
          <div className="stage-description-wrapper-form-end-event-options">
            {renderSelection}
          </div>
        </AntRadioGroup>
      )
    },
    [handleRadioChange, stage.event?.params]
  );

  return (
    <AntRow gutter={memoizedGutter}>
      <AntCol span={24}>
        <AntForm layout="vertical">
          <AntFormItem label="Name" required colon={false}>
            <AntInput name="Name" defaultValue={stage?.name} onChange={handleChange("name")} />
          </AntFormItem>
          <Form.Item label="Description">
            <AntInput
              type="textarea"
              className="description-field"
              autoSize={memoizedAutoSize}
              defaultValue={stage?.description}
              onChange={handleChange("description")}
            />
          </Form.Item>
          {(isFixedStage || stage.event?.type === TriggerEventType.JIRA_RELEASE) && (
            <div className="flex align-center">
              <span style={stageEnableStyle}>{stage.enabled ? "ACTIVE" : "DISABLED"}</span>
              &nbsp;
              <AntSwitch checked={stage?.enabled} onChange={handleEnableChange} />
            </div>
          )}
          {stage.event?.type === TriggerEventType.JIRA_RELEASE && renderRadioButton('prefer_release')}
          {stage.event?.type === 'SCM_PR_APPROVED' && renderRadioButton('approval')}
        </AntForm>
      </AntCol>
    </AntRow>
  );
};

export default StageInfoComponent;
