import React, { useCallback, useMemo } from "react";
import { Form } from "antd";
import { AntCol, AntForm, AntFormItem, AntInput, AntRow, AntSwitch } from "shared-resources/components";
import { RestStageConfig } from "classes/RestWorkflowProfile";

interface StageInfoProps {
  stage: RestStageConfig;
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
    [onChange, stage]
  );

  const handleEnableChange = useCallback(
    (value: boolean) => {
      (stage as any)["enabled"] = value;
      onChange(stage.json);
    },
    [onChange, stage]
  );

  const formItemLayout = {
    labelCol: {
      xs: { span: 24 },
      sm: { span: 2 }
    },
    wrapperCol: {
      xs: { span: 24 },
      sm: { span: 8 }
    }
  };
  return (
    <AntRow gutter={memoizedGutter}>
      <AntCol span={24}>
        <AntForm {...formItemLayout}>
          <AntFormItem label="Name" required>
            <AntInput name="Name" value={stage?.name} defaultValue={stage?.name} onChange={handleChange("name")} />
          </AntFormItem>
          <Form.Item label="Description">
            <AntInput
              type="textarea"
              className="description-field"
              autoSize={memoizedAutoSize}
              value={stage?.description}
              defaultValue={stage?.description}
              onChange={handleChange("description")}
            />
          </Form.Item>
          {isFixedStage && (
            <div className="flex align-center">
              <span className="active-switch">{stage.enabled ? "ACTIVE" : "DISABLED"}</span>
              &nbsp;
              <AntSwitch checked={stage?.enabled} onChange={handleEnableChange} />
            </div>
          )}
        </AntForm>
      </AntCol>
    </AntRow>
  );
};

export default StageInfoComponent;
