import * as React from "react";
import { Col, Row } from "antd";
import { AntInput, AntText } from "../../../../shared-resources/components";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../../../../constants/formWarnings";
import { Form } from "antd";

interface TemplateNameValidatorProps {
  templateNames: {
    id: string;
    initialName: string;
    currentName: string;
    valid: boolean;
    blur: boolean;
    validating: boolean;
    validate: boolean;
  }[];
  setTemplateNames: (names: any) => void;
}

export const TemplatesNameValidator: React.FC<TemplateNameValidatorProps> = props => {
  const getValidateStatus = (id: string) => {
    const item = props.templateNames.find((val: any) => val.id === id);
    if (item && item.validating) {
      return "validating";
    } else if (item && !item.blur) {
      return "";
    } else if (item && item.blur && item.currentName.length > 0 && item.valid) {
      return "success";
    } else return "error";
  };

  const getError = (id: string) => {
    const item = props.templateNames.find((val: any) => val.id === id);
    if (item && item.currentName && !item.valid) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  };

  const onInputChange = (id: string, value: string) => {
    let list = props.templateNames;
    const index = list.findIndex(item => item.id === id);
    const item = list[index];
    if (index !== -1) {
      list = [
        ...list.slice(0, index),
        { ...item, currentName: value, valid: false, validate: true },
        ...list.slice(index + 1)
      ];
    }
    props.setTemplateNames(list);
  };

  const setBlur = (id: string, value: boolean) => {
    let list = props.templateNames;
    const index = list.findIndex(item => item.id === id);
    const item = list[index];
    if (index !== -1) {
      list = [...list.slice(0, index), { ...item, blur: value }, ...list.slice(index + 1)];
    }
    props.setTemplateNames(list);
  };

  return (
    <div className="importValidator">
      {(props.templateNames || []).length >= 1 && props.templateNames[0].id !== "singleTemplate" && (
        <Row style={{ marginBottom: "1rem" }}>
          <Col span={8}>
            <AntText style={{ fontWeight: 600 }}>Template Count</AntText>
          </Col>
          <Col span={8}>
            <AntText style={{ fontWeight: 600 }}>Initial Name</AntText>
          </Col>
          <Col span={8}>
            <AntText style={{ fontWeight: 600 }}>Name Input/Current Name</AntText>
          </Col>
        </Row>
      )}
      {(props.templateNames || []).length >= 1 &&
        props.templateNames[0].id !== "singleTemplate" &&
        props.templateNames.map((item, index: number) => (
          <Row key={index}>
            <Col span={8}>{`Template ${index + 1}`}</Col>
            <Col span={8}>{item.initialName}</Col>
            <Col span={8}>
              <Form.Item
                validateStatus={getValidateStatus(item.id)}
                required
                hasFeedback={true}
                help={getValidateStatus(item.id) === "error" && getError(item.id)}>
                <AntInput
                  value={item.currentName}
                  style={{ width: "100%" }}
                  onFocus={() => setBlur(item.id, false)}
                  onBlur={() => setBlur(item.id, true)}
                  onChange={(e: any) => onInputChange(item.id, e.target.value)}
                />
              </Form.Item>
            </Col>
          </Row>
        ))}
      {(props.templateNames || []).length === 1 && props.templateNames[0].id === "singleTemplate" && (
        <Form.Item
          validateStatus={getValidateStatus(props.templateNames[0].id)}
          required
          label="Template name"
          hasFeedback={true}
          help={getValidateStatus(props.templateNames[0].id) === "error" && getError(props.templateNames[0].id)}>
          <AntInput
            value={props.templateNames[0].currentName}
            style={{ width: "100%" }}
            onFocus={() => setBlur(props.templateNames[0].id, false)}
            onBlur={() => setBlur(props.templateNames[0].id, true)}
            onChange={(e: any) => onInputChange(props.templateNames[0].id, e.target.value)}
          />
        </Form.Item>
      )}
    </div>
  );
};
