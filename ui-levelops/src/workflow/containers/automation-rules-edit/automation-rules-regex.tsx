import React from "react";
import { Form, Input } from "antd";
import { ERROR } from "constants/formWarnings";
import { AntText, AntButton } from "shared-resources/components";

export interface AutomationRulesRegexProps {
  onChange: (regexes: string[]) => any;
  regexes: string[];
  validateStatus?: any;
  help: React.ReactNode;
  index: number;
}

const AutomationRulesRegex: React.FC<AutomationRulesRegexProps> = props => {
  const {
    // Might be []
    regexes,
    onChange,
    validateStatus,
    help,
    index: outerIndex
  } = props;

  const onRegexChange = (value: string, index: number) => {
    const newRegexes = regexes.slice();
    newRegexes[index] = value;
    onChange(newRegexes);
  };

  const addCondition = () => {
    const newRegex = "";

    // In the case where you're creating a new rule,
    // The values object is []. But the UI makes it look like
    // it's actually [ "" ]. So behind the scenes, we need
    // to add 2 new strings, not 1.
    if (!regexes.length) {
      onChange(regexes.concat([newRegex, newRegex]));
    } else {
      onChange(regexes.concat(newRegex));
    }
  };

  const removeCondition = (index: number) => {
    if (regexes.length < 2) {
      return;
    }

    let newValue = regexes.filter((item, i) => i !== index);
    onChange(newValue);
  };

  const editableRegexes = regexes.slice();
  if (!editableRegexes.length) {
    editableRegexes.push("");
  }

  return (
    <Form.Item
      label={
        <div>
          <AntText strong style={{ textTransform: "uppercase" }}>
            Regular Expression
          </AntText>

          <AntButton
            id={`plus-circle${outerIndex}`}
            icon={"plus-circle"}
            type={"link"}
            size={"small"}
            onClick={addCondition}
            style={{
              position: "absolute",
              top: "0px",
              right: "0px",
              fontSize: "16px",
              zIndex: 3,
              pointerEvents: "auto",
              width: "auto",
              height: "auto"
            }}
          />
        </div>
      }
      validateStatus={validateStatus}
      help={validateStatus === ERROR && help}
      required={true}>
      {editableRegexes.map((regex: string, index: number) => {
        return (
          <React.Fragment key={index}>
            <div style={{ position: "relative" }}>
              <Input
                style={{ paddingRight: "20px" }}
                value={regex}
                onChange={event => onRegexChange(event.target.value, index)}
                // onKeyDown={event => onKeyDown(event, index)}
              />
              {editableRegexes.length > 1 && (
                <AntButton
                  id={`delete${index}`}
                  icon={"delete"}
                  type={"link"}
                  size={"small"}
                  onClick={() => removeCondition(index)}
                  style={{
                    position: "absolute",
                    top: "50%",
                    right: "0px",
                    transform: "translate(0px, -50%)",
                    zIndex: 3,
                    pointerEvents: "auto",
                    width: "auto",
                    height: "auto"
                  }}
                />
              )}
            </div>
          </React.Fragment>
        );
      })}
    </Form.Item>
  );
};

export default AutomationRulesRegex;
