import { notification } from "antd";
import { RestUsers } from "classes/RestUsers";
import Loader from "components/Loader/Loader";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import ErrorWrapper from "hoc/errorWrapper";
import { get, set } from "lodash";
import moment from "moment";
import React, { useCallback, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { formClear, formInitialize, formUpdateField, formUpdateObj } from "reduxConfigs/actions/formActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import {
  configsList,
  usersCreate,
  usersGet,
  usersUpdate,
  userTrellisPermissionUpdate
} from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";
import { getConfigListSelector } from "reduxConfigs/selectors/configSelector";
import { getUserForm } from "reduxConfigs/selectors/formSelector";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { sessionUserState } from "reduxConfigs/selectors/session_current_user.selector";
import {
  userCreateState,
  userGetState,
  userTrellisUpdateState,
  userUpdateState
} from "reduxConfigs/selectors/usersSelector";
import { UserWorkspaceSelectionType } from "reduxConfigs/types/response/me.response";
import { USERROLESUPPER } from "routes/helper/constants";
import { AntCard } from "shared-resources/components";
import { isSanitizedValue } from "utils/commonUtils";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { restAPILoadingState } from "utils/stateUtil";
import { validateEmail } from "utils/stringUtils";
import { isInValidOrgUnitAdminUser, transformUsersMetadata } from "./helper";
import "./user-edit.style.scss";
import { getSettingsPage } from "constants/routePaths";
import UserEditForm from "configurations/components/UserEditForm/UserEditForm";

const validateForm = (form: any) => {
  let baseValidations =
    !form.email.length || !form.first_name.length || !form.last_name.length || !validateEmail(form.email);
  if (form?.user_type === USERROLESUPPER.ORG_UNIT_ADMIN) {
    baseValidations = baseValidations || isInValidOrgUnitAdminUser(get(form, ["metadata", "workspaces"], {}));
  }
  return baseValidations;
};

export interface UserEditPageProps extends RouteComponentProps {
  className?: string;
}

export const UserEditPage: React.FC<UserEditPageProps> = (props: UserEditPageProps) => {
  const USER_FORM_NAME = "user_form";

  let userId: undefined | string = undefined;
  let isEditMode = false;
  const { pathname } = props.location;
  if (pathname.includes("edit-user-page")) {
    const { user } = parseQueryParamsIntoKeys(props.location.search, ["user"]);
    if (user) {
      userId = user[0];
    }
    isEditMode = !!userId;
  }

  const [loading, setLoading] = useState(isEditMode);
  const [create_loading, setCreateLoading] = useState(false);
  const [created, setCreated] = useState(false);
  const [set_header, setHeader] = useState(false);
  const [update_btn_status, setUpdateBtnStatus] = useState(false);
  const [updateLoading, setUpdateLoading] = useState(false);

  const dispatch = useDispatch();
  const userForm = useSelector(getUserForm);
  const pageSettingsState = useSelector(pageSettings);
  const userCreate = useSelector(userCreateState);
  const userUpdate = useSelector(userUpdateState);
  const userGet = useSelector(userGetState);
  const configListState = useSelector(getConfigListSelector);
  const sessionCurrentUserData = useSelector(sessionUserState);
  const trellisPermissionUpdateState = useSelector(userTrellisUpdateState);
  const entUserCountExceed = useHasEntitlements(Entitlement.SETTING_USERS_COUNT_10, EntitlementCheckType.AND);

  useEffect(() => {
    dispatch(formInitialize(USER_FORM_NAME, {}));
    dispatch(configsList());
    if (isEditMode) {
      dispatch(usersGet(userId));
    }
    return () => {
      dispatch(restapiClear("configs", "list", "0"));
      dispatch(formClear(USER_FORM_NAME));
      dispatch(restapiClear("users", "get", -1));
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, [usersGet]);

  useEffect(() => {
    const { loading } = restAPILoadingState(configListState);
    if (!loading && !userId) {
      setLoading(false);
      const configs = get(configListState, ["0", "data", "records"], []);
      const defaultRole = configs.find((config: any) => config.name === "AUTO_PROVISIONED_ROLE");
      const defaultSAML = configs.find((config: any) => config.name === "DEFAULT_SAML_ENABLED");
      const defaultPassword = configs.find((config: any) => config.name === "DEFAULT_PASSWORD_ENABLED");
      const mfaEnforced = configs.find((config: any) => config.name === "MFA_ENFORCED");
      const mfaEnrollmentTime = configs.find((config: any) => config.name === "MFA_ENROLLMENT_WINDOW");
      dispatch(formUpdateField(USER_FORM_NAME, "user_type", defaultRole ? defaultRole.value : "ADMIN"));
      dispatch(
        formUpdateField(
          USER_FORM_NAME,
          "password_auth_enabled",
          defaultPassword ? defaultPassword.value === "true" : false
        )
      );
      dispatch(
        formUpdateField(USER_FORM_NAME, "saml_auth_enabled", defaultSAML ? defaultSAML.value === "true" : false)
      );
      dispatch(
        formUpdateField(USER_FORM_NAME, "mfa_enforced", mfaEnforced && mfaEnforced.value && mfaEnforced?.value !== "0")
      );
      dispatch(
        formUpdateField(
          USER_FORM_NAME,
          "mfa_enrollment_end",
          mfaEnforced && mfaEnforced.value && mfaEnforced?.value !== "0"
            ? moment()
                .add(
                  moment.unix(parseInt(mfaEnrollmentTime?.value)).diff(moment.unix(parseInt(mfaEnforced?.value)), "d"),
                  "d"
                )
                .endOf("d")
                .unix()
            : undefined
        )
      );
    }
  }, [configListState]);

  useEffect(() => {
    const addBtnClicked = get(pageSettingsState, [pathname, "action_buttons", "create_update", "hasClicked"], false);
    if (addBtnClicked) {
      let newUser = userForm;
      newUser.notify_user = true;
      let user = new RestUsers(newUser);
      setCreateLoading(true);
      if (isEditMode) {
        const sessionCurrentData = get(sessionCurrentUserData, ["data"], {});
        if (sessionCurrentData && sessionCurrentData?.email === user.username) {
          dispatch(
            sessionCurrentUser({
              loading: false,
              error: false,
              data: {
                ...sessionCurrentData,
                metadata: get(user.json(), ["metadata"], {})
              }
            })
          );
        }
        dispatch(usersUpdate(userId, user));
      } else {
        dispatch(usersCreate(user));
      }
      dispatch(setPageButtonAction(pathname, "create_update", { hasClicked: false }));
    }
  }, [pageSettingsState, userForm, sessionCurrentUserData]);

  useEffect(() => {
    if (loading && userId !== undefined) {
      const { loading, error } = restAPILoadingState(userGet, userId);
      if (!loading && !error) {
        const apiUser = get(userGet, [userId, "data"], {});
        const nMetadata = transformUsersMetadata(get(apiUser, ["metadata"], {}));
        set(apiUser, ["metadata"], nMetadata);
        const user = new RestUsers(apiUser);
        dispatch(formUpdateField(USER_FORM_NAME, "email", user.username));
        dispatch(formUpdateField(USER_FORM_NAME, "first_name", user.firstName));
        dispatch(formUpdateField(USER_FORM_NAME, "last_name", user.lastName));
        dispatch(formUpdateField(USER_FORM_NAME, "password_auth_enabled", user.passwordAuth));
        dispatch(formUpdateField(USER_FORM_NAME, "saml_auth_enabled", user.samlAuth));
        dispatch(formUpdateField(USER_FORM_NAME, "user_type", user.userType));
        dispatch(formUpdateField(USER_FORM_NAME, "mfa_enabled", user.mfa_enabled));
        dispatch(formUpdateField(USER_FORM_NAME, "mfa_enrollment_end", user.mfa_enrollment_end));
        dispatch(formUpdateField(USER_FORM_NAME, "mfa_reset_at", user.mfa_reset_at));
        dispatch(formUpdateField(USER_FORM_NAME, "mfa_enforced", user.mfa_enforced));
        dispatch(formUpdateField(USER_FORM_NAME, "scopes", user?.scopes));
        dispatch(formUpdateField(USER_FORM_NAME, "metadata", user?.metadata));
        setLoading(false);
      }
    }

    if (!loading && !create_loading && !set_header) {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: "Users",
          action_buttons: {
            create_update: {
              type: "primary",
              actionId: "users",
              label: isEditMode ? "Update User" : "Create User",
              hasClicked: false
            }
          }
        })
      );
      setHeader(true);
    }

    if (set_header && !update_btn_status) {
      const btnStatus = validateForm(userForm);
      dispatch(
        setPageButtonAction(pathname, "create_update", {
          tooltip: "",
          disabled: btnStatus
        })
      );
      setUpdateBtnStatus(true);
    }

    if (create_loading) {
      let method = !isEditMode ? "create" : "update";
      const id = userId || "0";
      const { loading, error } = restAPILoadingState(!isEditMode ? userCreate : userUpdate, id);
      if (!loading) {
        if (error) {
          setCreated(false);
        } else {
          let user = new RestUsers(userForm);
          dispatch(userTrellisPermissionUpdate(user.json()));
          setCreated(true);
        }
        setCreateLoading(false);
        dispatch(restapiClear("users", method, -1));
      }
    }

    if (updateLoading) {
      const { loading, error } = restAPILoadingState(userUpdate, userId);
      if (!loading) {
        if (!error) {
          let user = new RestUsers(userForm);
          dispatch(userTrellisPermissionUpdate(user.json()));
          notification.success({ message: "User Updated Successfully" });
        }
        setUpdateLoading(false);
      }
    }
  });

  const onFieldChangeHandler = (name: string, field: string, value: string | boolean | UserWorkspaceSelectionType) => {
    const user = Object.assign(Object.create(Object.getPrototypeOf(userForm)), userForm);
    user[field] = value;
    dispatch(formUpdateObj(USER_FORM_NAME, user));
  };

  const updateUser = useCallback(
    updatedUser => {
      const user = Object.assign(Object.create(Object.getPrototypeOf(updatedUser)), updatedUser);
      dispatch(formUpdateObj(USER_FORM_NAME, user));
      if (isSanitizedValue(userId)) {
        let newUser = updatedUser;
        newUser.notify_user = true;
        let user = new RestUsers(newUser);
        dispatch(usersUpdate(userId, user));
        setUpdateLoading(true);
      }
    },
    [pathname, userId, sessionCurrentUserData]
  );

  const className = props.className || "user-edit-page";

  if (trellisPermissionUpdateState?.[userForm?.email]?.data !== undefined) {
    dispatch(restapiClear("trellis_user_permission", "update", userForm?.email));
    dispatch(formClear(USER_FORM_NAME));
    props.history.push(`${getSettingsPage()}/users-page`);
  }
  if (loading || create_loading) {
    return <Loader />;
  }

  return (
    <div className={`flex direction-column align-center`}>
      <div className={`${className}__content`}>
        <AntCard title={"User"}>
          <UserEditForm
            className={className}
            user_form={userForm}
            formUpdateField={onFieldChangeHandler}
            updateUser={updateUser}
            userId={userId}
            updateBtnStatus={(value: any) => {
              setUpdateBtnStatus(value);
            }}
            compact={false}
          />
        </AntCard>
      </div>
    </div>
  );
};

export default ErrorWrapper(UserEditPage);
