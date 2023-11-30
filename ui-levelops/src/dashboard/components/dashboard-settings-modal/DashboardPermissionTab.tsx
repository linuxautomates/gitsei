import { Checkbox, Form, Icon, Input, Radio, Typography } from "antd";
import { EMAIL_ERROR, PERMISSION_CHECKBOX_MSG } from "constants/formWarnings";
import React, { useEffect, useState } from "react";
import { AntButton, AntCol, AntRow, AntTooltip } from "shared-resources/components";
import { validateEmail } from "utils/stringUtils";
import {
  DashboardSettingsModalRadioHoverText,
  DashboardSettingsModalTitleType,
  DASHBOARD_SETTINGS_PERMISSIONS,
  DASHBOARD_SETTINGS_USER_PERMISSIONS
} from "./helper";
import { PERMISSION_DESC } from "./constant";

interface DashboardPermissionTabProps {
  permissions: any;
  email: any;
  setPermissions: (key: string, val: string) => void;
}

const DashboardPermissionTab: React.FC<DashboardPermissionTabProps> = props => {
  const { permissions, email, setPermissions } = props;
  const [userEmail, setUserEmail] = useState<string>("");
  const [toggleUserEmailInput, setUserEmailInput] = useState<boolean>(false);
  const [isValidEmail, setValidEmail] = useState<boolean>(true);

  useEffect(() => {
    if (permissions?.rbac?.isPublic) {
      const newPermissions = { ...permissions };
      newPermissions.rbac.dashboardPermission = newPermissions.rbac.isPublic;
      delete newPermissions.rbac.isPublic;
      setPermissions("rbac", newPermissions);
    }
  }, []);

  const removeUserPermission = (email: string) => {
    const newPermissions = { ...permissions };
    delete newPermissions.rbac.users[email];
    setPermissions("rbac", newPermissions);
  };

  const addUserEmail = () => {
    const newPermissions = { ...permissions };
    newPermissions.rbac.users[userEmail] = {
      permission: DASHBOARD_SETTINGS_USER_PERMISSIONS.VIEW
    };
    setUserEmail("");
    setPermissions("rbac", newPermissions);
  };

  const setPublicDefault = (access: string) => {
    const newPermissions = {
      ...permissions,
      rbac: {
        ...permissions?.rbac,
        dashboardPermission: access,
        users:
          access === DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC || access === DASHBOARD_SETTINGS_PERMISSIONS.ADMIN
            ? {}
            : permissions?.rbac?.users
      }
    };
    setPermissions("rbac", newPermissions);
  };

  const setPermissionsMeta = (email: string, key: string) => {
    const permission = {
      ...permissions,
      rbac: {
        ...permissions.rbac,
        users: {
          ...permissions.rbac.users,
          [email]: {
            ...permissions.rbac.users[email],
            permission: key
          }
        }
      }
    };
    setPermissions("rbac", permission);
  };

  const allUsersChecked = (e: any) => {
    const newPermissions = { ...permissions };
    newPermissions.rbac.allUsers = e?.target?.checked;
    setPermissions("rbac", newPermissions);
  };

  return (
    <>
      <AntRow gutter={[5, 5]}>
        <AntCol span={24}>
          <Form layout="vertical" className="permissions-form-container">
            <Form.Item label={DashboardSettingsModalTitleType.PERMISSIONS} colon={false}>
              <div className="form-content flex-start">
                <Radio.Group
                  onChange={() => {
                    setPublicDefault(DASHBOARD_SETTINGS_PERMISSIONS.ADMIN);
                    setUserEmailInput(false);
                  }}
                  value={permissions?.rbac?.dashboardPermission}>
                  <Radio value={DASHBOARD_SETTINGS_PERMISSIONS.ADMIN}></Radio>
                </Radio.Group>
                <Typography.Text className="form-content__text permission-option-desc-container">
                  <p className="permission-option-header">{PERMISSION_DESC.admin_only.header}</p>
                  <p className="permission-option-subheader">{PERMISSION_DESC.admin_only.subHeader}</p>
                </Typography.Text>
              </div>
              <div className="form-content flex-start">
                <Radio.Group
                  onChange={() => {
                    setPublicDefault(DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC);
                    setUserEmailInput(false);
                  }}
                  value={permissions?.rbac?.dashboardPermission}>
                  <Radio value={DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC}></Radio>
                </Radio.Group>
                <Typography.Text className="form-content__text permission-option-desc-container">
                  <p className="permission-option-header">{PERMISSION_DESC.public_view.header}</p>
                  <p className="permission-option-subheader">{PERMISSION_DESC.public_view.subHeader}</p>
                </Typography.Text>
              </div>
              <div className="form-content flex-start">
                <Radio.Group
                  onChange={() => {
                    setPublicDefault(DASHBOARD_SETTINGS_PERMISSIONS.LIMITED);
                  }}
                  value={permissions?.rbac?.dashboardPermission}>
                  <Radio value={DASHBOARD_SETTINGS_PERMISSIONS.LIMITED}></Radio>
                </Radio.Group>
                <Typography.Text className="form-content__text permission-option-desc-container">
                  <p className="permission-option-header">{PERMISSION_DESC.restricted.header}</p>
                  <p className="permission-option-subheader">{PERMISSION_DESC.restricted.subHeader}</p>
                  <p className="permission-option-subheader">{PERMISSION_DESC.restricted?.note}</p>
                </Typography.Text>
                {permissions?.rbac?.dashboardPermission === DASHBOARD_SETTINGS_PERMISSIONS.LIMITED && (
                  <Icon
                    onClick={() => {
                      setUserEmailInput(!toggleUserEmailInput);
                    }}
                    className="plus-icon-state"
                    type={toggleUserEmailInput ? "minus-circle" : "plus-circle"}
                  />
                )}
              </div>
            </Form.Item>
          </Form>
        </AntCol>
      </AntRow>
      {toggleUserEmailInput && (
        <>
          <AntRow gutter={[5, 5]}>
            <AntCol span={19}>
              <Form.Item label={"User Email"} colon={false}>
                <Input
                  name={"user_email"}
                  onChange={e => {
                    setUserEmail(e.target.value);
                    setValidEmail(
                      validateEmail(e.target.value) &&
                        e.target.value !== email &&
                        permissions?.rbac?.owner !== e.target.value &&
                        !permissions?.rbac?.users?.[e.target.value]
                    );
                  }}
                  value={userEmail}
                  style={{ margin: 0, padding: 0 }}
                />
                <span className="error-msg">{userEmail.length > 0 && !isValidEmail ? EMAIL_ERROR : ""}</span>
              </Form.Item>
            </AntCol>
            <AntCol span={5} style={{ marginTop: "7%" }}>
              <div className="form-content flex-start">
                <AntButton disabled={userEmail.length === 0 || !isValidEmail} type="primary" onClick={addUserEmail}>
                  Add User
                </AntButton>
              </div>
            </AntCol>
          </AntRow>
        </>
      )}
      {permissions?.rbac?.dashboardPermission === DASHBOARD_SETTINGS_PERMISSIONS.LIMITED && (
        <>
          <AntRow gutter={[5, 5]}>
            <AntCol span={24}>
              <div className="form-content flex-start">
                <Typography.Text className="form-content__text m-5">
                  <AntTooltip title={DashboardSettingsModalRadioHoverText.ALL_USER_CHECKOBOX}>
                    {PERMISSION_CHECKBOX_MSG}
                  </AntTooltip>
                </Typography.Text>
                <Checkbox className="m-5" onChange={allUsersChecked} checked={permissions?.rbac?.allUsers} />
              </div>
            </AntCol>
          </AntRow>
          <span className="user-permission-wrapper">
            <AntRow className="f-w-600" gutter={[16, 16]}>
              <AntCol span={9}>Account</AntCol>
              <AntCol span={3}>Creator</AntCol>
              <AntCol span={3}>Viewer</AntCol>
              <AntCol span={3}>Editor</AntCol>
              <AntCol span={4}>
                Owner{" "}
                <AntTooltip title={DashboardSettingsModalRadioHoverText.OWNER}>
                  <Icon type="info-circle" />
                </AntTooltip>{" "}
              </AntCol>
              <AntCol span={2} />
            </AntRow>
            <AntRow gutter={[8, 8]}>
              <AntCol span={9}>{permissions?.rbac?.owner}</AntCol>
              <AntCol span={3} style={{ textAlign: "center" }}>
                <Radio checked disabled />
              </AntCol>
              <AntCol span={3} style={{ textAlign: "center" }} />
              <AntCol span={3} style={{ textAlign: "center" }} />
              <AntCol span={3} style={{ textAlign: "center" }} />
              <AntCol span={3} style={{ textAlign: "center" }} />
            </AntRow>
            {(Object.keys(permissions?.rbac?.users) || []).map((userEmail: any) => {
              return (
                <AntRow gutter={[5, 5]}>
                  <AntCol span={9}>{userEmail}</AntCol>
                  <AntCol className="text-align-center" span={3} />
                  <AntCol className="text-align-center" span={3}>
                    <Radio.Group
                      onChange={() => {
                        setPermissionsMeta(userEmail, DASHBOARD_SETTINGS_USER_PERMISSIONS.VIEW);
                      }}
                      value={permissions?.rbac?.users?.[userEmail]?.permission}>
                      <Radio
                        disabled={permissions?.rbac?.allUsers}
                        value={DASHBOARD_SETTINGS_USER_PERMISSIONS.VIEW}></Radio>
                    </Radio.Group>
                  </AntCol>
                  <AntCol className="text-align-center" span={3}>
                    <Radio.Group
                      onChange={() => {
                        setPermissionsMeta(userEmail, DASHBOARD_SETTINGS_USER_PERMISSIONS.EDIT);
                      }}
                      value={permissions?.rbac?.users?.[userEmail]?.permission}>
                      <Radio value={DASHBOARD_SETTINGS_USER_PERMISSIONS.EDIT}></Radio>
                    </Radio.Group>
                  </AntCol>
                  <AntCol className="text-align-center" span={3}>
                    <Radio.Group
                      onChange={() => {
                        setPermissionsMeta(userEmail, DASHBOARD_SETTINGS_USER_PERMISSIONS.OWNER);
                      }}
                      value={permissions?.rbac?.users?.[userEmail]?.permission}>
                      <Radio value={DASHBOARD_SETTINGS_USER_PERMISSIONS.OWNER}></Radio>
                    </Radio.Group>
                  </AntCol>
                  <AntCol
                    span={2}
                    onClick={() => {
                      removeUserPermission(userEmail);
                    }}>
                    <Icon type="close" />
                  </AntCol>
                </AntRow>
              );
            })}
          </span>
        </>
      )}
    </>
  );
};

export default React.memo(DashboardPermissionTab);
