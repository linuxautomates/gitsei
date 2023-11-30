import React, { useCallback, useEffect, useState, useMemo, useRef } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import {
  AntCol,
  AntForm,
  AntFormItem,
  AntInput,
  AntRow,
  AntSelect,
  IntegrationIcon
} from "shared-resources/components";
import { Alert, Badge, Button, Card, Icon, Input, Select, Tag, Modal, Popconfirm, notification } from "antd";
import "./MergeUsers.scss";
import { convertEpochToDate, convertUnixToDate, unixToDate } from "utils/dateUtils";
import { cloneDeep, get, isEqual, uniqBy } from "lodash";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUsersGenericSelector, ORG_USER_SCHEMA_ID } from "reduxConfigs/selectors/orgUsersSelector";
import { validateEmail } from "utils/stringUtils";
import { genericList } from "reduxConfigs/actions/restapi";
import moment from "moment";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { OrgUserContributorsRolesGet } from "reduxConfigs/actions/restapi/orgUserAction";

const MergeUsers: React.FC = ({
  // @ts-ignore
  allSelectedUsers,
  // @ts-ignore
  toggleEditMergeView,
  // @ts-ignore
  isUpdateUser,
  // @ts-ignore
  setUpdatedUserList,
  // @ts-ignore
  updatedUserList,
  // @ts-ignore
  updateButtonState
}) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const [primaryUser, setPrimaryUser] = useState<Record<string, any>>({});
  const [selectedUsers, setSelectedUsers] = useState<Array<Record<string, any>>>(allSelectedUsers);
  const [newEmail, setNewEmail] = useState<string>("");
  const [showAddEmailError, setShowAddEmailError] = useState<boolean>(false);
  const newTrelisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);

  const userSchemaState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "get",
    id: ORG_USER_SCHEMA_ID
  });

  const usersList = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users",
    method: "list",
    id: `org_users_modal_select`
  });

  const contributorsRoles = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_contributors_roles",
    method: "get",
    id: "contributors_roles"
  })
  const breadCrumb = [
    {
      label: "Contributors",
      path: "",
      customOnClick: (e: any) => {
        e.preventDefault();
        updateButtonState();
        toggleEditMergeView();
      }
    },
    {
      label: primaryUser?.full_name,
      path: primaryUser?.full_name,
      customOnClick: (e: any) => {
        e.preventDefault();
      }
    }
  ];

  useEffect(() => {
    dispatch(
      setPageSettings(location.pathname, {
        title: primaryUser?.full_name,
        action_buttons: {
          manage_cancel: {
            type: "default",
            label: "Discard",
            hasClicked: false,
            buttonHandler: () => {
              updateButtonState();
              toggleEditMergeView();
            }
          },
          save: {
            type: "primary",
            label: isUpdateUser ? "Update" : "Merge",
            hasClicked: false,
            disabled: !primaryUser?.email || !primaryUser?.full_name,
            buttonHandler: () => {
              const mergedUser = cloneDeep({ ...primaryUser });
              if (!isUpdateUser) {
                const allUserId = allSelectedUsers.flatMap((users: any) => {
                  return users?.integration_user_ids;
                });
                mergedUser.integration_user_ids = allUserId;
              }
              setUpdatedUserList(mergedUser);
              toggleEditMergeView();
              const message = isUpdateUser
                ? "User updated successfully!"
                : `${allSelectedUsers.length} rows merged successfully!`;
              notification.success({
                message: `${message} Please click on Save to permanently save your changes.`
              });
            }
          }
        },
        withBackButton: true,
        bread_crumbs: breadCrumb,
        bread_crumbs_position: "before",
        goBack: true,
        goBackCallback: () => {
          updateButtonState();
          toggleEditMergeView();
        }
      })
    );
  }, [primaryUser, updatedUserList]);

  useEffect(() => {
    const loading = get(usersList, "loading", true);
    const error = get(usersList, "error", true);
    if (!error && !loading && newEmail) {
      const users = get(usersList, ["data", "records"], []);
      const selectedUsersEmail = selectedUsers.find(usr => usr.email === newEmail);
      if (users.length > 0 || selectedUsersEmail) {
        setShowAddEmailError(true);
      } else {
        let user = cloneDeep({ ...primaryUser });
        if (isUpdateUser) {
          user.email = newEmail;
          user.updated_at = moment().unix();
          setSelectedUsers([user]);
        }
        if (!isUpdateUser) {
          const emailUsers = cloneDeep(selectedUsers);
          const index = emailUsers.findIndex((usr: any) => usr?.email === primaryUser?.email);
          // @ts-ignore
          user =
            index == -1
              ? cloneDeep(emailUsers[0])
              : cloneDeep(emailUsers.find((usr: any) => usr.email === primaryUser.email));
          user.full_name = primaryUser?.full_name || "";
          user.email = newEmail;
          user.updated_at = moment().unix();
          emailUsers.push(user);
          setSelectedUsers(emailUsers);
        }
        user.updated_at = moment().unix();
        setPrimaryUser(user);
        setNewEmail("");
        setShowAddEmailError(false);
      }
    }
  }, [usersList]);

  useEffect(() => {
    if (Object.keys(primaryUser).length === 0) {
      const user = isUpdateUser ? cloneDeep(selectedUsers[0]) : selectedUsers.find((user: any) => !!user?.email);
      // @ts-ignore
      setPrimaryUser(user);
    }
  }, [selectedUsers]);

  const customFields = useMemo(() => {
    const loading = get(userSchemaState, "loading", true);
    const error = get(userSchemaState, "error", true);
    if (!loading && !error) {
      const data = get(userSchemaState, ["data", "fields"], []);
      const allKeys =  data
        .filter((item: any) => !["integration", "start_date", "full_name", "email"].includes(item?.key))
        .map((item: any) => item?.key);
        if(allKeys?.indexOf("contributor_role") !== -1) {
          dispatch(OrgUserContributorsRolesGet("contributors_roles", "org_users_contributors_roles"));
        }
        return allKeys;
    }
  }, [userSchemaState, selectedUsers]);

  const customFieldsRows = useMemo(() => {
    return (customFields.map((fields: any) => {
      return (
        newTrelisProfile && fields === "contributor_role" ?
        <AntRow className="m-t-1r">
        <AntCol span={10}>
          <AntFormItem label={"Contributor Role"}></AntFormItem>
          <AntSelect
            options={contributorsRoles?.data}
            value={primaryUser?.additional_fields?.[fields] || "Default"}
            onChange={(e: any) => {
              updateUserDetails(fields, e, true);
            }}
            disableLabelTransform={true}
          />
        </AntCol>
      </AntRow>
      :
        <AntRow className="m-t-1r">
          <AntCol span={10}>
            <AntFormItem label={`${fields}`}>
              <AntInput
                onChange={(val: any) => updateUserDetails(fields, val?.target?.value, true)}
                name={fields}
                value={primaryUser?.additional_fields?.[fields]}
              />
            </AntFormItem>
          </AntCol>
        </AntRow>
      );
    }))
  }, [newTrelisProfile, contributorsRoles, primaryUser])
  const integrationUsersId = useMemo(() => {
    const allUserId = allSelectedUsers.flatMap((users: any) => {
      users.integration_user_ids = users?.integration_user_ids.map((usr: any) => {
        usr.full_name = users.full_name;
        return usr;
      });
      return users?.integration_user_ids;
    });
    return allUserId;
  }, [selectedUsers]);

  const updateUserDetails = (updateKey: string, value: any, dynamicColumn: boolean = false) => {
    let updateUsers: any = {};
    if (dynamicColumn) {
      updateUsers = {
        ...primaryUser,
        additional_fields: {
          ...(primaryUser.additional_fields || {}),
          [updateKey]: value
        }
      };
    } else {
      updateUsers = {
        ...primaryUser,
        [updateKey]: value
      };
    }
    setPrimaryUser(updateUsers);
  };
  const addEmail = (email: string) => {
    dispatch(genericList("org_users", "list", { filter: { email: [email] } }, null, "org_users_modal_select", true));
  };

  const setAsPrimary = (user: any) => {
    setPrimaryUser(user);
  };

  return (
    <>
      <AntRow className="merge-users m-t-1r">
        <AntCol span={24}>
          <AntForm layout={"vertical"}>
            <AntRow>
              <AntCol span={10}>
                <AntFormItem
                  validateStatus={primaryUser?.full_name ? undefined : "error"}
                  help={primaryUser?.full_name ? undefined : "Display name is required"}
                  required={true}
                  label={"DISPLAY NAME"}>
                  <AntInput
                    onChange={(val: any) => updateUserDetails("full_name", val?.target?.value)}
                    name={"full_name"}
                    value={primaryUser?.full_name || ""}
                  />
                </AntFormItem>
              </AntCol>
            </AntRow>
            <AntRow className="m-t-1r">
              <AntCol span={10}>
                <AntFormItem
                  validateStatus={validateEmail(newEmail) || !newEmail ? undefined : "error"}
                  help={validateEmail(newEmail) || !newEmail ? undefined : "Invalid email"}
                  label={`${isUpdateUser && primaryUser?.email ? "Update" : "Add"} Email Address`}>
                  <span className="flex">
                    <Input
                      placeholder="Enter the email address"
                      value={newEmail}
                      onChange={e => setNewEmail(e?.target?.value)}
                      className="email-box"
                    />
                    <Button
                      type="default"
                      disabled={!validateEmail(newEmail)}
                      icon="plus"
                      onClick={() => {
                        addEmail(newEmail);
                      }}>
                      {" "}
                      {`${isUpdateUser && primaryUser?.email ? "Update" : "Add"} Email`}
                    </Button>
                  </span>
                </AntFormItem>
              </AntCol>
            </AntRow>
            <AntRow>
              <AntCol span={10}>
                <AntFormItem label={"Email Addresses"}></AntFormItem>
                {!primaryUser?.email && (
                  <Alert
                    showIcon={true}
                    message="There is no email id associated with the user. Please add an email address."
                    type="info"
                  />
                )}
                {selectedUsers.map((user: any) => {
                  return (
                    user?.email && (
                      <AntRow className="row-card">
                        <Card bordered={false}>
                          <AntCol span={17}>
                            <span>{user?.email}</span>
                            <span style={{ marginLeft: "1rem" }}>
                              {user?.email === primaryUser?.email && primaryUser?.email ? (
                                <Tag color="#FFFEE5">
                                  <span style={{ color: "#E5B800" }}>PRIMARY</span>
                                </Tag>
                              ) : (
                                user?.email && (
                                  <Button type="default" size="small" onClick={(val: any) => setAsPrimary(user)}>
                                    Set as Primary
                                  </Button>
                                )
                              )}
                            </span>
                          </AntCol>
                          <AntCol span={7}>
                            Updated On{" "}
                            {user?.updated_at
                              ? unixToDate(user?.updated_at, false, "DD MMM YYYY")
                              : unixToDate(moment().unix(), false, "DD MMM YYYY")}
                          </AntCol>
                        </Card>
                      </AntRow>
                    )
                  );
                })}
              </AntCol>
            </AntRow>
            <AntRow className="m-t-1r">
              <AntCol span={16}>
                <AntFormItem label={"Contributor IDs"}></AntFormItem>
                <AntRow>
                  <AntCol span={9}>
                    <span className="label-table user-id">Contributor ID</span>
                  </AntCol>
                  <AntCol span={9} className="flex align-items-center">
                    <span className="label-table integration">INTEGRATION NAME</span>
                  </AntCol>
                  <AntCol span={5} className="flex align-items-center">
                    <span className="label-table">DISPLAY NAME</span>
                  </AntCol>
                </AntRow>
                {integrationUsersId.map((userId: any) => {
                  return (
                    <AntRow className="row-card">
                      <Card bordered={false}>
                        <span>
                          <AntCol span={9}>
                            <span>{userId?.user_id}</span>
                          </AntCol>
                          <AntCol span={9} className="flex align-items-center">
                            {
                              // @ts-ignore
                              <IntegrationIcon
                                className="applications-menu-container-label-div__icon"
                                type={userId?.application || ""}
                              />
                            }
                            <span style={{ marginLeft: "1rem" }}>{userId?.name}</span>
                          </AntCol>
                          <AntCol span={5}>
                            <span>{userId?.full_name}</span>
                          </AntCol>
                        </span>
                      </Card>
                    </AntRow>
                  );
                })}
              </AntCol>
            </AntRow>
            <AntRow className="m-t-1r">
              <AntCol span={10}>
                <AntFormItem label={"Custom Fields"}></AntFormItem>
              </AntCol>
            </AntRow>
            {customFieldsRows}
          </AntForm>
        </AntCol>
      </AntRow>

      {showAddEmailError && (
        <Modal
          title={
            <div className="warning-title">
              <span>Email ID already exists</span>
            </div>
          }
          visible={showAddEmailError}
          className="org-user-warning"
          closable={false}
          cancelText="Cancel"
          okText="Go to user list"
          onCancel={() => setShowAddEmailError(false)}
          onOk={() => {
            toggleEditMergeView();
          }}
          width={390}
          maskClosable={false}>
          <div className="flex flex-column warning-content">
            <span className="error-icon">
              {" "}
              <Icon type="info-circle" />
              <span className="text">Alert</span>
            </span>
            <span>Looks like the provided email id is already associated with an existing user.</span>
            <span className="warning-text">Please start the merge process from the user list page.</span>
          </div>
        </Modal>
      )}
    </>
  );
};

export default MergeUsers;
