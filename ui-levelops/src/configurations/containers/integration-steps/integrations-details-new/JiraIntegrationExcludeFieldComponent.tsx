import React, { useCallback, useMemo, useState } from "react";
import { Form } from "antd";
import { AntCheckbox, TitleWithInfo } from "shared-resources/components";
import { SENSITIVE_FIELDS_DESCRIPTION, SENSITIVE_FIELDS_KEY } from "./integration.constants";

const JiraIntegrationExcludeFieldComponent: React.FC<{
  handleExcludeUpdate: (value: any) => void;
  sensitiveFieldValue: string | undefined;
}> = ({ handleExcludeUpdate, sensitiveFieldValue }) => {
  const [checkboxSwitchState, setCheckboxSwitchState] = useState<{ [x: string]: boolean }>({
    summary: sensitiveFieldValue ? sensitiveFieldValue.includes("summary") : false,
    description: sensitiveFieldValue ? sensitiveFieldValue.includes("description") : false,
    comment: sensitiveFieldValue ? sensitiveFieldValue.includes("comment") : false
  });

  const renderTitle = useMemo(
    () => (
      <TitleWithInfo
        style={{ justifyContent: "flex-start", marginBottom: "1rem" }}
        title="Exclude From Ingestion"
        description={SENSITIVE_FIELDS_DESCRIPTION}
      />
    ),
    []
  );

  const handleSensitiveFieldsCheckboxChange = useCallback(
    (key: string, check: boolean) => {
      const newCheckboxSwitchState = { ...checkboxSwitchState };
      newCheckboxSwitchState[key] = check;
      const checkedKeys = Object.keys(newCheckboxSwitchState).filter(key => newCheckboxSwitchState[key]);
      let checkVal = undefined;
      if (checkedKeys.length > 0) {
        checkVal = checkedKeys.join(",");
      }
      setCheckboxSwitchState(newCheckboxSwitchState);
      handleExcludeUpdate({ [SENSITIVE_FIELDS_KEY]: checkVal });
    },
    [checkboxSwitchState]
  );

  return (
    <Form.Item label={renderTitle} colon={false} key={"Exclude-Checkboxes"}>
      <AntCheckbox
        checked={checkboxSwitchState["summary"]}
        onChange={(e: any) => handleSensitiveFieldsCheckboxChange("summary", e.target.checked)}>
        Summary
      </AntCheckbox>
      <AntCheckbox
        checked={checkboxSwitchState["description"]}
        onChange={(e: any) => handleSensitiveFieldsCheckboxChange("description", e.target.checked)}>
        Description
      </AntCheckbox>
      <AntCheckbox
        checked={checkboxSwitchState["comment"]}
        onChange={(e: any) => handleSensitiveFieldsCheckboxChange("comment", e.target.checked)}>
        Comments
      </AntCheckbox>
    </Form.Item>
  );
};

export default JiraIntegrationExcludeFieldComponent;
