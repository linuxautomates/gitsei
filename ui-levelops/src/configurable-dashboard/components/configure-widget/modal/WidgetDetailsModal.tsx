import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Input } from "antd";
import { AntCol, AntForm, AntFormItem, AntInput, AntModal, AntRow } from "shared-resources/components";

import "./EditWidgetModal.scss";
import GraphTypeComponent from "../../GraphTypeComponents";
import { RestWidget } from "../../../../classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";
import { REPORT_TYPE_HINT } from "configurable-dashboard/helpers/helper";

const { TextArea } = Input;

export const MAX_WIDGET_NAME_LENGTH = 100;

interface WidgetDetailsModalProps {
  widget: RestWidget;
  title: string;
  onCancel: () => void;
  onSave: (name: string, description: string, graphType?: string) => void;
  hideGraphConfiguration?: boolean;
}

const WidgetDetailsModal: React.FC<WidgetDetailsModalProps> = ({
  widget,
  title,
  onSave,
  onCancel,
  hideGraphConfiguration
}) => {
  const [name, setName] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [showNameValidationError, setShowNameValidationError] = useState<boolean>(false);

  //graph or stat
  const widgetType = useMemo(() => widget?.widget_type || WidgetType.GRAPH, [widget]);

  const [graphType, setGraphType] = useState<string>(widgetType);

  const handleWidgetNameChange = useCallback((e: any) => {
    const widgetName = e.target?.value;
    setName(widgetName);
    widgetName?.trim()?.length > MAX_WIDGET_NAME_LENGTH
      ? setShowNameValidationError(true)
      : setShowNameValidationError(false);
  }, []);

  const handleWidgetDescriptionChange = useCallback((e: any) => setDescription(e.target?.value), []);

  const handleWidgetTypeSelection = useCallback((value: string) => setGraphType(value), []);

  const handleCancel = useCallback(() => onCancel?.(), []);

  useEffect(() => {
    setName(widget?.name || "");
    setDescription(widget?.description || "");
  }, [widget]);

  const memoizedGutter = useMemo(() => [16, 16], []);

  const handleSave = useCallback(() => {
    onSave(name, description, graphType);
  }, [widget, name, description, graphType]);

  return (
    <AntModal
      visible
      title={title}
      destroyOnClose
      width={700}
      centered
      mask={true}
      maskClosable={false}
      okText="Save and Configure Graph"
      onOk={handleSave}
      okButtonProps={{ disabled: !name?.length || !widgetType }}
      onCancel={handleCancel}>
      <AntRow gutter={memoizedGutter}>
        <AntCol span={24}>
          <AntForm layout="vertical">
            <AntFormItem label="Name" required colon={false}>
              <AntInput
                maxLength={100}
                name="Name"
                placeholder="My First Widget"
                value={name}
                onChange={handleWidgetNameChange}
              />
              {showNameValidationError && (
                <span className="error-msg">Maximum character limit ({MAX_WIDGET_NAME_LENGTH}) reached.</span>
              )}
            </AntFormItem>
            <AntFormItem
              label="Description"
              colon={false}
              placeholder="Widget Description"
              onChange={handleWidgetDescriptionChange}>
              <TextArea rows={2} name={"Description"} value={description} placeholder={"Widget Description"} />
            </AntFormItem>
            {!hideGraphConfiguration && (
              <GraphTypeComponent
                graphType={graphType}
                widgetType={widgetType}
                onWidgetTypeSelection={handleWidgetTypeSelection}
              />
            )}
          </AntForm>
          {!hideGraphConfiguration && <p className="widget-config-hint">{REPORT_TYPE_HINT}</p>}
        </AntCol>
      </AntRow>
    </AntModal>
  );
};

export default React.memo(WidgetDetailsModal);
