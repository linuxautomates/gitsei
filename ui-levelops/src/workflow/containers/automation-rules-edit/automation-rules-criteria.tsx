import React from "react";
import { Col, Form, Row, Icon } from "antd";
import { AntCard, AntText, AntSelect, AntButton, AntPopover } from "shared-resources/components";
import { ERROR } from "constants/formWarnings";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import AutomationRulesRegex from "./automation-rules-regex";
import { BASE_UI_URL } from "helper/envPath.helper";
import { DOCS_ROOT, DOCS_PATHS } from "constants/docsPath";

let REACT_APP_UI_URL = BASE_UI_URL;
if ((REACT_APP_UI_URL || "").includes("localhost")) {
  REACT_APP_UI_URL = "https://testui1.levelops.io";
}

interface AutomationRulesCriteriaProps {
  applicationFields: string[];
  selectedApplicationId: string;
  value: any[];
  onChange: (value: any[]) => any;
  validateField: (field: string, value: string) => "" | "error" | "success";
  helperText: (field: string) => string;
}

const AutomationRulesCriteria: React.FC<AutomationRulesCriteriaProps> = (props: AutomationRulesCriteriaProps) => {
  const { applicationFields, value, onChange, selectedApplicationId, validateField, helperText } = props;

  const prevAppId = usePrevious(selectedApplicationId);

  React.useEffect(() => {
    // When someone changes application.
    // Need to update criteria.
    if (selectedApplicationId !== prevAppId) {
      const newValue = value.slice();
      for (let key in newValue) {
        const obj = newValue[key];
        if (!applicationFields) {
          newValue[key].field_name = "";
        } else if (!applicationFields.includes(obj.field_name)) {
          newValue[key].field_name = "";
        }
      }

      onChange(newValue);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedApplicationId, applicationFields]);

  const onFieldTypeChange = (selectValue: string, index: number) => {
    const newValue = value.slice();
    if (!newValue.length) {
      newValue.push({
        field_name: selectValue,
        regexes: []
      });
    } else {
      newValue[index] = {
        ...value[index],
        field_name: selectValue
      };
    }

    onChange(newValue);
  };

  const regexesChangeHandler = (regexValues: string, index: number) => {
    const newValue = value.slice();
    const regexValuesArray = regexValues ? regexValues.split("\n") : [];

    if (!newValue.length) {
      newValue.push({
        field_name: "",
        regexes: regexValuesArray
      });
    } else {
      newValue[index] = {
        ...value[index],
        regexes: regexValuesArray
      };
    }

    onChange(newValue);
  };

  const addCondition = () => {
    if (!value.length) {
    }
    const newCondition = {
      field_name: "",
      regexes: []
    };

    // In the case where you're creating a new rule,
    // The values object is []. But the UI makes it look like
    // it's actually [ { ... } ]. So behind the scenes, we need
    // to add 2 new conditions, not 1.
    if (!value.length) {
      onChange(value.concat([newCondition, newCondition]));
    } else {
      onChange(value.concat(newCondition));
    }
  };

  const removeCondition = (index: number) => {
    if (value.length < 2) {
      return;
    }

    let newValue = value.filter((condition, i) => i !== index);
    onChange(newValue);
  };

  const editableValue = value.slice();
  if (!editableValue.length) {
    editableValue.push({
      field_name: "",
      regexes: []
    });
  }

  return (
    <React.Fragment>
      <Row>
        <Col className="d-flex align-center" style={{ marginBottom: "1rem", justifyContent: "space-between" }}>
          <AntText strong style={{ fontSize: "16px", textTransform: "uppercase" }}>
            Match Conditions
          </AntText>
          <AntPopover
            placement="leftTop"
            // trigger={"click"}
            content={
              <React.Fragment>
                <div style={{ maxWidth: "300px", padding: "10px" }}>
                  <p style={{ marginBottom: "0px" }}>
                    <span>{`Regular Expressions allow you to look for patterns. For example `}</span>
                    <span>
                      <strong>{`*Security*`}</strong>
                    </span>
                    <span>{` will match any content that contains the word Security in it. `}</span>
                    <AntButton
                      type="link"
                      target="_blank"
                      href={DOCS_ROOT + DOCS_PATHS.REGEX_CHEATSHEET}
                      style={{
                        paddingLeft: "0px",
                        lineHeight: "normal",
                        height: "auto"
                      }}>
                      Read more
                    </AntButton>
                  </p>
                </div>
              </React.Fragment>
            }>
            <Icon type="info-circle" theme="outlined" className="icon" />
          </AntPopover>
        </Col>
      </Row>
      <Row style={{ flex: 1, overflow: "auto" }}>
        <div style={{ paddingBottom: "24px" }}>
          {editableValue.map((criterion, index) => {
            const fieldTypeValidateStatus = validateField("field_name", criterion.field_name);
            const regexesValidateStatus = validateField("regexes", criterion.regexes);
            return (
              <React.Fragment key={index}>
                <AntCard style={{ border: "0px", backgroundColor: "#f5f6f8", marginBottom: "16px", padding: "24px" }}>
                  <Form.Item
                    label={"Field"}
                    required={true}
                    validateStatus={fieldTypeValidateStatus}
                    help={fieldTypeValidateStatus === ERROR && helperText("field_name")}>
                    <AntSelect
                      options={applicationFields}
                      value={criterion.field_name}
                      onChange={(value: string) => onFieldTypeChange(value, index)}
                      disabled={!selectedApplicationId}
                    />
                  </Form.Item>
                  <AutomationRulesRegex
                    index={index}
                    regexes={criterion.regexes}
                    validateStatus={regexesValidateStatus === ERROR ? regexesValidateStatus : undefined}
                    help={regexesValidateStatus === ERROR ? helperText("regexes") : "One regular expression per line."}
                    onChange={newRegexes => {
                      regexesChangeHandler(newRegexes.join("\n"), index);
                    }}
                  />
                  {/* <Form.Item
                    label={"Regular Expression"}
                    required={true}
                    validateStatus={regexesValidateStatus === ERROR ? regexesValidateStatus : undefined}
                    help={regexesValidateStatus === ERROR ? helperText("regexes") : "One regular expression per line."}>
                    <Input.TextArea
                      value={criterion.regexes.join("\n")}
                      onChange={e => regexesChangeHandler(e.currentTarget.value, index)}
                      autoSize={{ minRows: 2, maxRows: 4 }}
                      disabled={!selectedApplicationId}
                      style={{
                        backgroundColor: "white"
                      }}
                    />
                  </Form.Item> */}
                  {editableValue.length > 1 && (
                    <div className="button-alignment-div">
                      <AntButton type="link" onClick={() => removeCondition(index)}>
                        Remove Condition
                      </AntButton>
                    </div>
                  )}
                </AntCard>
              </React.Fragment>
            );
          })}
          <div className="button-alignment-div">
            <AntButton type="secondary" onClick={addCondition}>
              Add Condition
            </AntButton>
          </div>
          {/* <div>{JSON.stringify(value, null, 2)}</div> */}
        </div>
      </Row>
    </React.Fragment>
  );
};

export default AutomationRulesCriteria;
