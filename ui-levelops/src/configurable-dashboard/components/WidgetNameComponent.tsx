import React, { useState, useCallback } from "react";
import { Form } from "antd";
import "./ModalSettings.scss";
import { AntInput } from "../../shared-resources/components";

export const MAX_WIDGET_NAME_LENGTH = 100;

interface WidgetNameComponentProps {
  name: string;
  onNameChange: (value: string) => void;
}

const WidgetNameComponent: React.FC<WidgetNameComponentProps> = ({ name, onNameChange }) => {
  const [showNameValidationError, setShowNameValidationError] = useState<boolean>(false);

  const handleWidgetNameChange = useCallback((e: any) => {
    const widgetName = e.target?.value;
    widgetName?.trim()?.length > MAX_WIDGET_NAME_LENGTH
      ? setShowNameValidationError(true)
      : setShowNameValidationError(false);
    onNameChange(widgetName);
  }, []);

  return (
    <Form.Item label="Name" required className={"modal_settings"}>
      <AntInput value={name} onChange={handleWidgetNameChange} />
      {showNameValidationError && (
        <span className="error-msg">Maximum character limit ({MAX_WIDGET_NAME_LENGTH}) reached.</span>
      )}
    </Form.Item>
  );
};

export default React.memo(WidgetNameComponent);
