import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { useDispatch } from "react-redux";
import { Form, Switch } from "antd";
import { debounce, get } from "lodash";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import {
  CURRENT_PROPERTIES_DESCRIPTION,
  DEFAULT_PROFILE_DESC
} from "configurations/pages/ticket-categorization/constants-new/constants";
import {
  profileIssueManagementOption,
  TICKET_CATEGORIZATION_SCHEME,
  TICKET_CATEGORIZATION_SCHEMES_CHECK_ID
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import {
  currentPriorityLabelMapping,
  ProfileBasicInfoType,
  profilesCurrentPriorities
} from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { ERROR, NAME_EXISTS_ERROR, REQUIRED_FIELD, SUCCESS } from "constants/formWarnings";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { ticketCategorizationSchemesList } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntInput, AntSelect } from "shared-resources/components";
import "./profileBasicInfoComponent.styles.scss";
import EIColorPickerWrapper from "../../color-picker-wrapper/EIColorPickerWrapper";

interface ProfileBasicInfoProps {
  profile: RestTicketCategorizationScheme;
  handleChanges: (key: ProfileBasicInfoType, value: string | boolean | object) => void;
}

const ProfileBasicInfoComponent: React.FC<ProfileBasicInfoProps> = ({ profile, handleChanges }) => {
  const dispatch = useDispatch();
  const schemesCheckList = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "list",
    uuid: TICKET_CATEGORIZATION_SCHEMES_CHECK_ID
  });

  const [nameExists, setNameExists] = useState<boolean>(false);
  const [searching, setSearching] = useState<boolean>(false);
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);

  const categoriesLengthRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (searching) {
      const loading = get(schemesCheckList, ["loading"], true);
      const error = get(schemesCheckList, ["error"], true);
      if (!loading && !error) {
        const records = get(schemesCheckList, ["data", "records"], []);
        const resNameExists =
          !!records?.filter((item: any) => item?.name?.toLowerCase() === profile?.name?.trim().toLowerCase())?.length ||
          false;
        setNameExists(resNameExists);
        setSearching(false);
      }
    }
  }, [schemesCheckList]);

  useEffect(() => {
    categoriesLengthRef.current = (profile?.categories || []).length;
    return () => {
      dispatch(restapiClear(TICKET_CATEGORIZATION_SCHEME, "list", TICKET_CATEGORIZATION_SCHEMES_CHECK_ID));
    };
  }, []);

  useEffect(() => {
    const categoriesLength = (profile?.categories || []).length;
    if (categoriesLength !== categoriesLengthRef.current) {
      categoriesLengthRef.current = categoriesLength;
      handleNameChange(`${profile?.name || ""}`);
    }
  }, [profile]);

  const handleNameChange = useCallback(
    (value: string) => {
      handleChanges("name", value);
      debouncedSearch(value as string);
    },
    [profile]
  );

  const handlePriorityChange = useCallback(
    (key: string, value: boolean) => {
      let priorityMapping = {
        ...(profile?.current_priorities_mapping || {}),
        [key]: value
      };
      handleChanges("current_priorities_mapping", priorityMapping);
    },
    [profile]
  );

  const debouncedSearch = useCallback(
    debounce((name: string) => {
      const filters = {
        filter: {
          partial: {
            name
          }
        }
      };
      dispatch(ticketCategorizationSchemesList(filters, TICKET_CATEGORIZATION_SCHEMES_CHECK_ID));
      setSearching(true);
    }, 200),
    []
  );

  const getValidateStatus = useMemo(() => {
    if (!nameFieldBlur) return "";
    if (nameFieldBlur && (profile?.name || "").length > 0 && !nameExists) return SUCCESS;
    return ERROR;
  }, [profile, nameExists, nameFieldBlur]);

  const handleNameFieldBlurChange = useCallback(() => setNameFieldBlur(true), []);

  const getError = useMemo(() => {
    if (profile?.name === "" || !nameExists) return REQUIRED_FIELD;
    return NAME_EXISTS_ERROR;
  }, [nameExists, profile]);

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
            />
          </Form.Item>
          <Form.Item label="Description">
            <AntInput
              defaultValue={profile?.description}
              value={profile?.description}
              onChange={(e: any) => handleChanges("description", e?.target?.value)}
            />
          </Form.Item>
          <Form.Item
            label={
              <div className="ant-form-item-label default-switch-item">
                <label className={"ant-form-item-no-colon"}>Current Priorities</label>
                <p className="priorities-desc">{CURRENT_PROPERTIES_DESCRIPTION}</p>
              </div>
            }>
            {profilesCurrentPriorities.map(priority => (
              <div key={priority} className="priority-list-container">
                <div className="label">{currentPriorityLabelMapping[priority]}</div>
                <div className="switch-container">
                  <Switch
                    onChange={(value: boolean) => handlePriorityChange(priority, value)}
                    checked={profile?.current_priorities_mapping[priority]}
                  />
                </div>
              </div>
            ))}
          </Form.Item>
          <Form.Item label="Issue Management System">
            <AntSelect
              value={profile?.issue_management_integration}
              options={profileIssueManagementOption}
              onChange={(value: string) => handleChanges("issue_management_integration", value)}
            />
          </Form.Item>
          <div className="ant-form-item">
            <div className="ant-form-item-label default-switch-item">
              <label className={"ant-form-item-no-colon"}>Default Profile</label>
              <p className="priorities-desc">{DEFAULT_PROFILE_DESC}</p>
            </div>
            <div className="default-switch-container">
              <div className="label">Set as default</div>
              <div className="switch-container">
                <Switch
                  disabled={profile?.defaultScheme}
                  checked={profile?.defaultScheme}
                  onChange={e => handleChanges("defaultScheme", e)}
                />
              </div>
            </div>
          </div>
          <EIColorPickerWrapper
            handleChanges={handleChanges as any}
            label="Uncategorized Tickets Color"
            color={profile?.uncategorized_color ?? ""}
            filterKey="uncategorized_color"
            profile={profile}
          />
        </Form>
      </div>
    </div>
  );
};

export default ProfileBasicInfoComponent;
