import { Form } from "antd";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { ERROR, LONG_NAME_ERROR, NAME_EXISTS_ERROR, REQUIRED_FIELD, SUCCESS } from "constants/formWarnings";
import React, { useCallback, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { AntInput, AntText } from "shared-resources/components";

interface ConfigurationInfoProps {
  profile: RestWorkflowProfile;
  handleChanges: (newValue: any) => void;
  disabled?: boolean;
  profilesList: Array<any>;
}

const ConfigurationInfo: React.FC<ConfigurationInfoProps> = ({ profile, handleChanges, disabled, profilesList }) => {
  const dispatch = useDispatch();
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);

  const handleNameChange = useCallback(
    (value: string) => {
      const isValidName = !profilesList?.some(
        (item: { id: string; name: string }) =>
          item?.id !== profile.id && item?.name?.toLowerCase() === value.trim().toLowerCase()
      );
      handleChanges({
        isValidName: isValidName,
        name: value
      });
    },
    [profile, profilesList]
  );

  const getValidateStatus = useMemo(() => {
    if (!nameFieldBlur) return "";
    if ((profile?.name?.trim() || "").length > 75) return ERROR;
    if (nameFieldBlur && (profile?.name?.trim() || "").length > 0 && profile.isValidName) return SUCCESS;
    return ERROR;
  }, [profile, profile.isValidName, nameFieldBlur]);

  const getError = useMemo(() => {
    if ((profile?.name?.trim() || "").length > 75) {
      return LONG_NAME_ERROR;
    }
    if (profile.name === "" || profile.isValidName) return REQUIRED_FIELD;
    return NAME_EXISTS_ERROR;
  }, [profile.isValidName, profile]);

  const handleNameFieldBlurChange = useCallback(() => setNameFieldBlur(true), []);

  return (
    <div className="profile-basic-info-container dev-score-profile-container-section">
      <div className="dev-score-profile-container-section-container-header">
        <AntText className="section-header">CONFIGURATION</AntText>
      </div>
      <div className="basic-info-content-container">
        <Form colon={false}>
          <Form.Item
            label="Name"
            required
            hasFeedback
            validateStatus={getValidateStatus}
            help={getValidateStatus === "error" && getError}>
            <AntInput
              defaultValue={profile.name}
              onChange={(e: any) => handleNameChange(e?.target?.value)}
              value={profile.name}
              onFocus={handleNameFieldBlurChange}
              disabled={disabled}
              placeholder="add workflow profile name"
              className={profile.name ? "" : "font-familt-italic"}
            />
          </Form.Item>
          <Form.Item label="Description">
            <AntInput
              defaultValue={profile?.description}
              value={profile?.description}
              onChange={(e: any) => handleChanges({ description: e?.target?.value })}
              disabled={disabled}
              placeholder="add description"
              className={profile.description ? "" : "font-familt-italic"}
            />
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default ConfigurationInfo;
