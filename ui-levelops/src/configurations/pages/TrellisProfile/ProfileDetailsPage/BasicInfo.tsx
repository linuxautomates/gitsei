import { Form } from "antd";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { ERROR, NAME_EXISTS_ERROR, REQUIRED_FIELD, SUCCESS } from "constants/formWarnings";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { trellisProfilesListReadAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { AntInput } from "shared-resources/components";

interface BasicInfoProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (newValue: any) => void;
  profilesList: Array<any>;
  disabled?: boolean;
}

const BasicInfo: React.FC<BasicInfoProps> = ({ profile, handleChanges, profilesList, disabled }) => {
  const dispatch = useDispatch();
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);

  const handleNameChange = useCallback(
    (value: string) => {
      const isValidName = !profilesList?.some(
        (item: any) => item?.id !== profile.id && item?.name?.toLowerCase() === value.trim().toLowerCase()
      );
      handleChanges({
        isValidName: isValidName,
        name: value
      });
    },
    [profile, profilesList]
  );

  useEffect(() => {
    dispatch(trellisProfilesListReadAction({}, false));
  }, []);

  const getValidateStatus = useMemo(() => {
    if (!nameFieldBlur) return "";
    if (nameFieldBlur && (profile?.name || "").length > 0 && profile?.isValidName) return SUCCESS;
    return ERROR;
  }, [profile, profile?.isValidName, nameFieldBlur]);

  const handleNameFieldBlurChange = useCallback(() => setNameFieldBlur(true), []);

  const getError = useMemo(() => {
    if (profile?.name === "" || profile?.isValidName) return REQUIRED_FIELD;
    return NAME_EXISTS_ERROR;
  }, [profile?.isValidName, profile]);

  return (
    <div className="profile-basic-info-container">
      <h2 className="basic-info-container-title">Basic Info</h2>
      <div className="basic-info-content-container">
        <Form colon={false}>
          <Form.Item
            label="Name"
            required
            hasFeedback
            validateStatus={getValidateStatus}
            help={getValidateStatus === "error" && getError}>
            <AntInput
              defaultValue={profile?.name}
              onChange={(e: any) => handleNameChange(e?.target?.value)}
              value={profile?.name}
              onFocus={handleNameFieldBlurChange}
              disabled={disabled}
            />
          </Form.Item>
          <Form.Item label="Description">
            <AntInput
              defaultValue={profile?.description}
              value={profile?.description}
              onChange={(e: any) => handleChanges({ description: e?.target?.value })}
              disabled={disabled}
            />
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default BasicInfo;
