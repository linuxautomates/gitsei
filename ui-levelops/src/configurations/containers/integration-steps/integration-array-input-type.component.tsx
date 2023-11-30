import { Col, Icon, Row } from "antd";
import * as React from "react";
import { AntFormItem, AntInput } from "shared-resources/components";

interface Field {
  key: string;
  label: string;
  fields: { key: string; label: string; isIndex?: boolean, type?: string; }[];
  value: any;
  required?: boolean;
}

interface FieldProps {
  field: Field;
  updateField: any;
}

interface ArrayInputProps {
  fields: Field[];
  updateField: any;
}

const Input: React.FC<FieldProps> = ({ field, updateField }) => {
  const getInitialField = () => {
    return field.fields.reduce(
      (acc, _field) => ({ ...acc, [_field.key]: _field.isIndex ? `${_field.key}_${field.value.length + 1}` : "" }),
      {}
    );
  };

  const values = field.value.length > 0 ? field.value : [getInitialField()];

  const getValue = (key: string, index: number) => {
    return values[index][key];
  };

  const handleRemoveField = (index: number) => {
    const updatedValues = [...values.slice(0, index), ...values.slice(index + 1)];
    updateField(field.key, updatedValues);
  };

  const addNewField = () => {
    values.push(getInitialField());
    updateField(field.key, values);
  };

  const onValueChange = (key: string, value: string, index: number) => {
    values[index][key] = value;
    updateField(field.key, values);
  };

  const requiredField = field.hasOwnProperty("required") ? !!field.required : true;

  const dividerStyle = React.useMemo(() => {
    return { opacity: "0.5" };
  }, []);

  const iconStyle = React.useMemo(() => {
    return { fontSize: "16px", cursor: "pointer", paddingLeft: "16px" };
  }, []);

  return (
    <>
      <AntFormItem
        label={
          <div className="flex align-center">
            <span>
              {field.label} <span style={{ color: "red" }}>{requiredField ? "*" : ""}</span>
            </span>
            <Icon type="plus" style={iconStyle} onClick={() => addNewField()} />
          </div>
        }
        colon={false}
        key={field.key}>
        {values.map((value: Field, index: number) => (
          <>
            {index > 0 && <hr style={dividerStyle} />}
            <Row type="flex" style={{ paddingBottom: index != values.length - 1 ? "12px" : 0 }} align="middle">
              <Col span={field.value.length > 1 ? 22 : 24}>
                {field.fields
                  .filter(field => !field.isIndex)
                  .map(field => (
                    <AntInput
                      value={getValue(field.key, index)}
                      placeholder={field.label}
                      type={"type" in field ? field.type : "text"}
                      onChange={(e: any) => onValueChange(field.key, e.currentTarget.value, index)}
                    />
                  ))}
              </Col>
              {field.value.length > 1 && (
                <Col span={2} className="flex justify-center align-center">
                  <Icon
                    type="delete"
                    style={{ fontSize: "18px", cursor: "pointer" }}
                    onClick={() => handleRemoveField(index)}
                  />
                </Col>
              )}
            </Row>
          </>
        ))}
      </AntFormItem>
    </>
  );
};

const ArrayInput: React.FC<ArrayInputProps> = ({ fields, updateField }) => {
  return (
    <>
      {fields.map(field => (
        <Input field={field} updateField={updateField} />
      ))}
    </>
  );
};

export default ArrayInput;
