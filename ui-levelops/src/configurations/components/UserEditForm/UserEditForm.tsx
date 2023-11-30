import { Divider, Popconfirm } from "antd";
import { RestUsers } from "classes/RestUsers";
import MFASettingsModal from "configurations/pages/global/MFASettingsModal";
import { EMAIL_WARNING, EMPTY_FIELD_WARNING, ERROR, SUCCESS } from "constants/formWarnings";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { get } from "lodash";
import moment from "moment";
import MFASetupModal from "profile/mfa-setup-modal/MFASetupModal";
import * as React from "react";
import { useCallback, useMemo, useState } from "react";
import { MeResponse, UserWorkspaceSelectionType } from "reduxConfigs/types/response/me.response";
import { USERROLESUPPER } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";
import { EntityIdentifier } from "types/entityIdentifier";
import { validateEmail } from "utils/stringUtils";
import { getTimezone } from "utils/timeUtils";
import {
  AntButton,
  AntCheckbox,
  AntCol,
  AntForm,
  AntFormItem,
  AntInput,
  AntRow,
  AntSelect,
  AntSwitch,
  AntText
} from "../../../shared-resources/components/index";
import { TRELLIS_PERMISSION_INFO } from "./constant";
import "./userEditForm.styles.scss";
import UserWorkspaceOUAssociationContainer from "./UserWorkspaceOUAssociationContainer";
interface UserEditFormProps {
  className?: string;
  user_form: Partial<MeResponse>;
  formUpdateField: (name: string, field: string, value: string | boolean | UserWorkspaceSelectionType) => void;
  updateBtnStatus?: (value: boolean) => void;
  compact?: boolean;
  userId?: EntityIdentifier;
  updateUser?: (updatedUser: any) => void;
}

const UserEditFormComponent: React.FC<UserEditFormProps> = ({
  className = "",
  user_form,
  formUpdateField,
  updateBtnStatus,
  compact = false,
  userId,
  updateUser
}) => {
  const ls = new LocalStoreService();
  const [form, setForm] = useState<any>({});
  const [mfaSettingsModal, setMfaSettingsModal] = useState<boolean>(false);
  const [mfaSetupModal, setMfaSetupModal] = useState<boolean>(false);
  const orgUnitEnhancementSupport = useHasEntitlements(Entitlement.ORG_UNIT_ENHANCEMENTS, EntitlementCheckType.AND);

  const isLoggedInUser = useMemo(() => {
    const loggedInId = ls.getUserId();
    return loggedInId === userId;
  }, [userId]);

  const fields: any[] = [
    {
      prefix: "user",
      name: "email",
      label: "Email",
      isReadOnly: false,
      isRequired: true,
      value: user_form.email,
      compact: true
    },
    {
      prefix: "user",
      name: "first_name",
      label: "First Name",
      isReadOnly: false,
      isRequired: true,
      value: user_form.first_name,
      compact: false
    },
    {
      prefix: "user",
      name: "last_name",
      label: "Last Name",
      isReadOnly: false,
      isRequired: true,
      value: user_form.last_name,
      compact: false
    }
  ];

  const validateField = (field: string, value: string) => {
    if (!form[field]) {
      return "";
    }

    switch (field) {
      case "email":
        if (validateEmail(value)) {
          return "";
        } else {
          return ERROR;
        }
      case "last_name":
      case "first_name":
        if (value !== undefined && value !== null && (value !== "" || compact)) {
          return "";
        } else {
          return ERROR;
        }
      default:
        return SUCCESS;
    }
  };

  const helpText = (field: string) => {
    switch (field) {
      case "email":
        return EMAIL_WARNING;
      case "first_name":
      case "last_name":
      default:
        return EMPTY_FIELD_WARNING;
    }
  };

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      setForm((state: any) => ({ ...state, [field]: false }));
      updateBtnStatus && updateBtnStatus(false);
      formUpdateField("user_form", field, e.target ? e.target.value : e);
    };
  };

  const handleToggle = (e: any) => {
    const field = e.target.id;
    const currentValue = (user_form as any)[field];
    formUpdateField("user_form", field, !currentValue);
    updateBtnStatus && updateBtnStatus(false);
  };

  const handlePasswordAndSSO = (field: "saml_auth_enabled" | "password_auth_enabled" | "scopes", e?: any) => {
    let currentValue = (user_form as any)[field];
    if (field === "scopes") {
      currentValue = e ? { dev_productivity_write: [] } : {};
      formUpdateField("user_form", field, currentValue);
    } else {
      formUpdateField("user_form", field, !currentValue);
    }
    updateBtnStatus && updateBtnStatus(false);
  };

  const handleMFASettingsChange = useCallback(
    (timestamp?: number) => {
      const user = Object.assign(Object.create(Object.getPrototypeOf(user_form)), user_form);
      user["mfa_enabled"] = false;
      if (timestamp) {
        user["mfa_enrollment_end"] = timestamp;
        user["mfa_reset_at"] = moment().unix();
      } else {
        user["mfa_enrollment_end"] = 0;
      }
      updateUser?.(user);
      setMfaSettingsModal(false);
    },
    [user_form]
  );

  const handleMFAEnrollmentSuccess = useCallback(() => {
    const user = Object.assign(Object.create(Object.getPrototypeOf(user_form)), user_form);
    user["mfa_enabled"] = true;
    updateUser?.(user);
    setMfaSetupModal(false);
  }, [user_form]);

  const handleEnableMFA = useCallback(() => {
    if (isLoggedInUser) {
      setMfaSetupModal(true);
      return;
    }
    setMfaSettingsModal(true);
  }, [isLoggedInUser]);

  const isCurrentlyInEnrollmentPeriod = useMemo(
    () => (user_form.mfa_enrollment_end || 0) >= moment().unix(),
    [user_form]
  );

  const handleUserFormMetadataChange = (newMetadata: { [x: string]: any }) => {
    formUpdateField("user_form", "metadata", newMetadata);
    updateBtnStatus && updateBtnStatus(false);
  };

  const getRoleOptions = useMemo(() => {
    const baseRoles = RestUsers.TYPES;
    if (orgUnitEnhancementSupport) {
      return [...baseRoles, USERROLESUPPER.ORG_UNIT_ADMIN];
    }
    return baseRoles;
  }, [orgUnitEnhancementSupport]);

  const allowWorkspaceAndOUSelection = useMemo(() => {
    if (user_form?.user_type === USERROLESUPPER.ORG_UNIT_ADMIN) return true;
    if (user_form?.user_type === USERROLESUPPER.ADMIN)
      return !user_form?.scopes?.hasOwnProperty("dev_productivity_write");
    return false;
  }, [user_form]);

  return (
    <>
      {mfaSettingsModal && (
        <MFASettingsModal
          visible={mfaSettingsModal}
          mfaEnrollmentPeriod={user_form.mfa_enrollment_end}
          onCancel={() => setMfaSettingsModal(false)}
          onSave={handleMFASettingsChange}
        />
      )}
      {mfaSetupModal && (
        <MFASetupModal
          onMFAEnrollSuccess={handleMFAEnrollmentSuccess}
          closeSetupModal={() => setMfaSetupModal(false)}
          showSetupModal={mfaSetupModal}
        />
      )}
      <AntForm layout="vertical">
        {!compact && (
          <AntFormItem label="role" wrapperCol={{ span: 7 }}>
            <AntSelect
              className={`${className} ${className}__search-filter`}
              id="select-type"
              options={getRoleOptions.map(option => ({ label: option, value: option }))}
              value={user_form.user_type}
              onChange={onFieldChangeHandler("user_type")}
            />
          </AntFormItem>
        )}
        {compact && (
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <AntFormItem label="role" style={{ width: "8rem" }}>
              <AntSelect
                className={`${className} ${className}__search-filter`}
                id="select-type"
                options={getRoleOptions.map(option => ({ label: option, value: option }))}
                value={user_form.user_type}
                onChange={onFieldChangeHandler("user_type")}
              />
            </AntFormItem>
            <AntCheckbox id={`password_auth_enabled`} checked={user_form.password_auth_enabled} onChange={handleToggle}>
              Password Enabled
            </AntCheckbox>
            <AntCheckbox id={`saml_auth_enabled`} checked={user_form.saml_auth_enabled} onChange={handleToggle}>
              SSO Enabled
            </AntCheckbox>
          </div>
        )}
        {fields
          .filter(field => field.compact || !compact)
          .map((field, i) => {
            return (
              <AntFormItem
                key={`${field}-${i}`}
                label={<span>{field.label}</span>}
                hasFeedback
                required={field.isRequired}
                validateStatus={validateField(field.name, field.value)}
                style={{ marginBottom: "30px" }}
                help={validateField(field.name, field.value) === ERROR && helpText(field.name)}>
                <AntInput
                  key={field.name}
                  value={field.value || ""}
                  name={field.name}
                  rows={field.rows}
                  hasError={field.hasError}
                  type={field.type || "text"}
                  onChange={onFieldChangeHandler(field.name)}
                  onBlur={() => {
                    setForm({
                      [field.name]: true
                    });
                  }}
                />
              </AntFormItem>
            );
          })}
        {!compact && (
          <>
            <Divider />
            <div className={"user-edit flex direction-column"}>
              <span className={"pb-20"} style={{ fontWeight: 500, fontSize: 16 }}>
                Authentication Settings
              </span>
              <AntRow>
                <AntCol span={8}>
                  <div className={"w-80"}>
                    <div className={"flex align-center"}>
                      <AntSwitch
                        id={`saml_auth_enabled`}
                        onChange={() => handlePasswordAndSSO("saml_auth_enabled")}
                        checked={user_form.saml_auth_enabled}
                      />
                      <AntText className={"ml-10"}>SSO</AntText>
                    </div>
                    <div className={"mt-10"}>
                      Authenticate using a SSO Identity Provider. Most secure and recommended option.
                    </div>
                  </div>
                </AntCol>
                <AntCol span={8}>
                  <div className={"w-80"}>
                    <div className={"flex align-center"}>
                      <AntSwitch
                        id={`password_auth_enabled`}
                        onChange={() => handlePasswordAndSSO("password_auth_enabled")}
                        checked={user_form.password_auth_enabled}
                      />
                      <AntText className={"ml-10"}>Password</AntText>
                    </div>
                    <div className={"mt-10"}>
                      Authenticate using email and password. Ideal for small companies without an IdP.
                    </div>
                  </div>
                </AntCol>
                <AntCol span={8}>
                  {user_form.password_auth_enabled && (
                    <div>
                      {!user_form.mfa_enabled && !user_form.mfa_enforced && !isCurrentlyInEnrollmentPeriod ? (
                        <>
                          <AntButton onClick={handleEnableMFA} type={"ghost"}>
                            Enable MFA
                          </AntButton>
                          <div className={"mt-10"}>
                            Enable MFA for extra layer of security while using password based authentication.
                          </div>
                        </>
                      ) : (
                        <>
                          {!user_form.mfa_enforced && (
                            <Popconfirm
                              onConfirm={() => handleMFASettingsChange()}
                              title="Are you sure you want to disable MFA?"
                              okText="Yes"
                              cancelText="Cancel">
                              <AntButton type={"danger"} disabled={user_form.mfa_enforced} ghost>
                                Disable MFA
                              </AntButton>
                            </Popconfirm>
                          )}
                          <AntButton onClick={() => setMfaSettingsModal(true)} type={"ghost"} className={"ml-10"}>
                            Reset MFA
                          </AntButton>
                          {!user_form.mfa_enabled && !user_form.mfa_enforced && (
                            <>
                              {user_form.mfa_enrollment_end ? (
                                <div className={"mt-10"}>
                                  MFA Enrollment Period ends at:{" "}
                                  {`${moment
                                    .unix(user_form.mfa_enrollment_end)
                                    .format("D MMM, YYYY HH:mm")} ${getTimezone()}`}
                                </div>
                              ) : (
                                <div className={"mt-10"}>
                                  Enable MFA for extra layer of security while using password based authentication.
                                </div>
                              )}
                            </>
                          )}
                          {user_form.mfa_enforced && <div className={"mt-10"}>MFA is Enforced Globally</div>}
                        </>
                      )}
                    </div>
                  )}
                </AntCol>
              </AntRow>
              <Divider />
              <AntText className={"title pb-20"}>Profile Settings</AntText>
              <AntRow>
                <AntCol span={8}>
                  <div className={"w-80"}>
                    <div className={"flex align-center"}>
                      <AntSwitch
                        id={`scopes`}
                        onChange={(e: any) => handlePasswordAndSSO("scopes", e)}
                        checked={user_form?.scopes?.hasOwnProperty("dev_productivity_write")}
                      />
                      <AntText className={"ml-10"}>Trellis Access</AntText>
                    </div>
                    <AntText className={"mt-10"}>{TRELLIS_PERMISSION_INFO}</AntText>
                  </div>
                </AntCol>
              </AntRow>
              {orgUnitEnhancementSupport && allowWorkspaceAndOUSelection && (
                <>
                  <Divider />
                  <AntText className={"title pb-20 text-uppercase"}>
                    {user_form?.user_type === USERROLESUPPER.ORG_UNIT_ADMIN ? (
                      <>
                        <span>{"This account is an admin for the following Collection"}</span>
                        <span style={{ color: "red" }} className="ml-5">
                          *
                        </span>
                      </>
                    ) : (
                      <span>{"Associate Collection for trellis access"}</span>
                    )}
                  </AntText>
                  <AntRow>
                    <AntCol span={8} className={"w-100p"}>
                      <div className="w-100p">
                        <UserWorkspaceOUAssociationContainer
                          metadata={get(user_form, ["metadata"], {})}
                          handleMetadataChange={handleUserFormMetadataChange}
                        />
                      </div>
                    </AntCol>
                  </AntRow>
                </>
              )}
              {user_form?.user_type === USERROLESUPPER.PUBLIC_DASHBOARD && orgUnitEnhancementSupport && (
                <>
                  <Divider />
                  <AntText className={"title pb-20 flex flex-column"}>
                    <span>{"SELECT THE COLLECTIONS TO WHICH THIS USER HAS PERMISSION TO VIEW THE INSIGHTS"}</span>
                    <AntText className="note">
                      Note: Not linking to any collection will grant this user access to Insights across all Collection.
                    </AntText>
                  </AntText>
                  <AntRow>
                    <AntCol span={8} className={"w-100p"}>
                      <div className="w-100p">
                        <UserWorkspaceOUAssociationContainer
                          metadata={get(user_form, ["metadata"], {})}
                          handleMetadataChange={handleUserFormMetadataChange}
                        />
                      </div>
                    </AntCol>
                  </AntRow>
                </>
              )}
            </div>
          </>
        )}
      </AntForm>
    </>
  );
};

export default UserEditFormComponent;
