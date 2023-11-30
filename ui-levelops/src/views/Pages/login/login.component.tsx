import { Alert, Button, Card, Divider, Form, Icon, Input, notification, Typography } from "antd";
import Loader from "components/Loader/Loader";
import { EMAIL_WARNING, EMPTY_FIELD_WARNING, MFA_CODE_INVALID } from "constants/formWarnings";
import { buildApiUrl, VERSION } from "constants/restUri";
import { ERROR_TEXT } from "profile/mfa-setup-modal/MFASetup.constants";
import MFASetupModal from "profile/mfa-setup-modal/MFASetupModal";
import queryString from "query-string";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { sessionGetMFA } from "reduxConfigs/actions/restapi/mfa.action";
import {
  resetPassword,
  sessionLoad,
  sessionLogin,
  sessionLogout,
  sessionError as setError
} from "reduxConfigs/actions/sessionActions";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { RestMFAState } from "reduxConfigs/types/rest-state/session-reducer/RestMFAState";
import { AntButton } from "shared-resources/components";
import { isSanitizedValue, sanitizeObject } from "utils/commonUtils";
import { checkMFAValidation } from "utils/regexUtils";
import { validateEmail } from "utils/stringUtils";
import { formWarming } from "../common/commons.component";
import "./LoginPage.style.scss";
import {
  CODE_INPUT_LABEL,
  HELP_TEXT_1,
  HELP_TEXT_2,
  MFA_VERIFICATION_HEADER_TEXT,
  VERIFY_BUTTON_TEXT
} from "./mfaVerification.constants";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { validateUrl } from "layouts/helper";
import { getBaseUrl } from "constants/routePaths";
import { getLocationObject } from "../helper";

const { Title } = Typography;

type LoginFormType = {
  company: string;
  email: string;
  password: string;
};

const defaultValues: LoginFormType = { company: "", email: "", password: "" };

const HAS_ERROR = "hasError";
const BROADRIDGE = "broadridge";
const CUSTOMER_SUCCESS_EMAIL = "customersuccess@levelops.io";

const NEW_WORKSPACES_NOTIFICATION_KEY = "new_workspaces_notification";

const LoginPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const dispatch = useDispatch();
  const sessionCurrentUser = useSelector(sessionCurrentUserState);
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const sessionMFARestState: RestMFAState = useMemo(() => sessionRestState?.session_mfa, [sessionRestState]);

  const getComapny = useMemo(() => {
    const prevQuery = localStorage.getItem("prev_search");
    let tenant = "";
    if (isSanitizedValue(prevQuery)) {
      const values = queryString.parse(prevQuery || "");
      if (values.tenant !== undefined) {
        tenant = values.tenant as string;
      }
    }
    return tenant;
  }, []);

  const [cardHidden, setCardHidden] = useState<boolean>(true);
  const [loading, setLoading] = useState<boolean>(false);
  const [verifyingOTP, setVerifyingOTP] = useState<boolean>(false);
  const [sessionError, setSessionError] = useState<string>("");
  const [loginForm, setLoginForm] = useState<LoginFormType>({ ...defaultValues, company: getComapny });
  const [showMFAVerificationModal, setShowMFAVerificationModal] = useState<boolean>(false);
  const [showMFASetupModal, setShowMFASetupModal] = useState<boolean>(false);
  const [formValidation, setFormValidation] = useState<LoginFormType>(defaultValues);
  const [code, setCode] = useState<string>("");
  const [codeValidation, setCodeValidation] = useState<string>();

  useEffect(() => {
    dispatch(sessionLoad());
    const { session_token, session_error, session_default_route } = sessionRestState;
    if (session_token !== null && !session_error && isSanitizedValue(session_default_route)) {
      props.history.push(session_default_route);
    }
    setTimeout(() => setCardHidden(false), 700);
  }, []);

  useEffect(() => {
    const { session_token } = sessionRestState;
    const values = queryString.parse(props.location.search);
    if (!session_token && values.tenant && ((values?.auth_method as string) || "").toLowerCase() === "sso") {
      window.location.replace(buildApiUrl(`/${VERSION}/generate_authn?company=${values.tenant}`));
    }
  }, [props.location]);

  const navigationURL = (url: string, locationObject: any) => {
    const inValidURL = validateUrl(locationObject);
    if (sessionCurrentUser?.metadata?.selected_workspace) {
      const selected_workspace = sessionCurrentUser?.metadata?.selected_workspace;
      dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, selected_workspace));
    }
    if (inValidURL) {
      props.history.replace(getBaseUrl());
    } else {
      props.history.push(url);
    }
  };
  useEffect(() => {
    const { session_token, session_error, session_default_route, session_user_id, session_error_message } =
      sessionRestState;
    if (session_token !== null && !session_error) {
      if (session_user_id !== null) {
        notification.close(NEW_WORKSPACES_NOTIFICATION_KEY);

        const prevQuery = localStorage.getItem("prev_search");
        const prevLocation = localStorage.getItem("prev_location");
        if (isSanitizedValue(prevLocation)) {
          let url = prevLocation || "";
          if (isSanitizedValue(prevQuery)) {
            url = url.concat(prevQuery || "");
          }
          if (url?.length) {
            const locationObject = { pathname: prevLocation || "", search: prevQuery || "" };
            navigationURL(url, locationObject);
          }
        } else {
          if (sessionCurrentUser?.metadata?.last_login_url && sessionCurrentUser?.metadata?.selected_workspace) {
            const subsequent_url = sessionCurrentUser?.metadata?.last_login_url;
            const locationObject = getLocationObject(subsequent_url);
            navigationURL(sessionCurrentUser?.metadata?.last_login_url, locationObject);
          } else {
            if (isSanitizedValue(session_default_route)) {
              props.history.push(session_default_route);
            }
          }
        }
      }
    }
    if (session_error) {
      setSessionError(session_error_message);
      setLoading(false);
      setVerifyingOTP(false);
    }
  }, [sessionRestState, sessionCurrentUser]);

  useEffect(() => {
    if (sessionMFARestState) {
      const { loading, error } = sessionMFARestState;
      if (!loading && !error) {
        const { mfa_required, mfa_enrollment } = sessionMFARestState;
        if (mfa_required) {
          if (!showMFAVerificationModal) {
            setShowMFAVerificationModal(true);
            setLoading(false);
          }
        } else if (mfa_enrollment) {
          setShowMFASetupModal(true);
          setLoading(false);
        } else {
          notification.close(NEW_WORKSPACES_NOTIFICATION_KEY);
          dispatch(sessionLogin(loginForm.email, loginForm.password, loginForm.company));
        }
      }
    }
  }, [sessionMFARestState]);

  const ssoUrl = useCallback(
    () => window.location.replace(buildApiUrl(`/${VERSION}/generate_authn?company=${loginForm.company}`)),
    [loginForm.company]
  );

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (
      Object.keys(sanitizeObject(loginForm) || {}).length === 3 &&
      Object.keys(sanitizeObject(formValidation) || {}).length === 0
    ) {
      setLoading(true);
      dispatch(setError(""));
      setSessionError("");
      dispatch(sessionGetMFA({ company: loginForm.company, password: loginForm.password, email: loginForm.email }));
    }
  };

  const handleSSORedirect = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      if (loginForm.company.length > 0) {
        ssoUrl();
      }
    },
    [loginForm.company]
  );

  const handleForgotPassword = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      if (formValidation.email !== HAS_ERROR && formValidation.company !== HAS_ERROR) {
        dispatch(resetPassword(loginForm.email, loginForm.company));
        notification.info({
          message: "Forgot Password",
          description: "Please check your email for password reset instructions"
        });
      }
    },
    [loginForm]
  );

  const handleChange = useCallback(
    (event: any) => {
      event?.persist?.();
      const field = event?.target?.id;
      if (isSanitizedValue(field)) {
        setLoginForm((prev: any) => ({ ...prev, [field]: event?.target?.value || "" }));
        switch (field) {
          case "email":
            setFormValidation((prev: any) => ({
              ...prev,
              email: validateEmail(event?.target?.value || "") ? "" : HAS_ERROR
            }));
            break;
          case "company":
          case "password":
            setFormValidation((prev: any) => ({
              ...prev,
              [field]: (event?.target?.value || "").length > 0 ? "" : HAS_ERROR
            }));
            break;
          default:
            break;
        }
      }
    },
    [loginForm, formValidation]
  );

  const handleCodeChange = useCallback((event: any) => {
    event?.persist?.();
    setCode(event?.target?.value);
    setCodeValidation(checkMFAValidation(event?.target?.value) ? "" : HAS_ERROR);
  }, []);

  const loader = useMemo(
    () => (
      <div data-testid="loader">
        <Loader />
      </div>
    ),
    []
  );

  const loginDisabled = useMemo(() => {
    return !(
      loginForm?.company?.length &&
      loginForm?.email?.length &&
      loginForm?.password?.length &&
      formValidation?.company !== HAS_ERROR &&
      formValidation?.email !== HAS_ERROR &&
      formValidation?.password !== HAS_ERROR
    );
  }, [loginForm, formValidation]);

  const disableForBrodridge = useMemo(() => {
    return (
      loginForm?.company.toLowerCase() === BROADRIDGE && loginForm?.email?.toLowerCase() !== CUSTOMER_SUCCESS_EMAIL
    );
  }, [loginForm]);

  const verifyDisabled = useMemo(() => !(code.length && codeValidation !== HAS_ERROR), [code, codeValidation]);

  const handleMFAVerify = useCallback(
    (e: React.SyntheticEvent) => {
      // Stops from refreshing the page on form submit
      e.preventDefault();

      setVerifyingOTP(true);
      dispatch(setError(""));
      setSessionError("");
      dispatch(sessionLogin(loginForm.email, loginForm.password, loginForm.company, code));
    },
    [loginForm, code]
  );

  const closeMFAEnrollModal = () => {
    dispatch(sessionLogout());
    setLoginForm({ ...defaultValues, company: getComapny });
    setShowMFASetupModal(false);
  };

  const emailWarning = useMemo(() => formWarming(EMAIL_WARNING), []);
  const emptyWarning = useMemo(() => formWarming(EMPTY_FIELD_WARNING), []);
  const mfaCodeWarning = useMemo(() => formWarming(MFA_CODE_INVALID), []);

  const renderLogin = (
    <>
      <Form.Item className={"mb-20"} label="Company" help={formValidation.company === HAS_ERROR ? emptyWarning : null}>
        <Input onChange={handleChange} value={loginForm.company} id="company" placeholder="Company" />
      </Form.Item>
      <Form.Item
        className={"my-20"}
        label="Email Address"
        help={formValidation.email === HAS_ERROR ? emailWarning : null}>
        <Input onChange={handleChange} value={loginForm.email} id="email" placeholder="Email Address" />
      </Form.Item>
      <Form.Item
        className={"my-20"}
        label={
          <div style={{ height: "1rem" }} className={"flex justify-space-between"}>
            <span>Password</span>
            <a
              href={"#"}
              style={{ fontSize: 14, fontWeight: "normal", textTransform: "capitalize" }}
              onClick={handleForgotPassword}>
              Forgot Password
            </a>
          </div>
        }
        help={formValidation.password === HAS_ERROR ? emptyWarning : null}>
        <Input
          onChange={handleChange}
          type="password"
          value={loginForm.password}
          id="password"
          placeholder="Password"
          disabled={disableForBrodridge}
        />
      </Form.Item>
      <div className="flex justify-center mb-20 mt-30">
        <>
          {loading && loader}
          {!loading && (
            <>
              <Button
                className="mr-10 "
                type="primary"
                htmlType={"submit"}
                disabled={loginDisabled || disableForBrodridge}>
                Login
              </Button>
              <Button disabled={loading || loginForm.company.length === 0} onClick={handleSSORedirect}>
                Login with SSO
              </Button>
            </>
          )}
        </>
      </div>
    </>
  );

  const renderMFAVerification = (
    <>
      <div className="verification-modal">
        <div className="verification_code_container">
          <Form.Item label={CODE_INPUT_LABEL} required help={codeValidation === HAS_ERROR ? mfaCodeWarning : null}>
            <Input placeholder="6-digit-code" value={code} onChange={handleCodeChange} />
          </Form.Item>
        </div>
        <div className="verifiy_button_container">
          {verifyingOTP ? (
            loader
          ) : (
            <AntButton type="primary" disabled={verifyDisabled} htmlType={"submit"}>
              {VERIFY_BUTTON_TEXT}
            </AntButton>
          )}
        </div>
        <Divider />
        <div className="verification-help-container">
          <Icon type="laptop" className="help-icon" />
          <p className="text">{HELP_TEXT_1}</p>
        </div>
        <div className="verification-help-container">
          <Icon type="info-circle" className="help-icon" />
          <p className="text">{HELP_TEXT_2}</p>
        </div>
      </div>
    </>
  );

  return (
    <>
      {showMFASetupModal && <MFASetupModal showSetupModal={showMFASetupModal} closeSetupModal={closeMFAEnrollModal} />}
      <Card
        hidden={cardHidden}
        title={
          <>
            <Title level={3} className="mb-0 text-center">
              {showMFAVerificationModal ? MFA_VERIFICATION_HEADER_TEXT : "Login"}
            </Title>
            {sessionError && (
              <Alert className={"my-5"} message={showMFAVerificationModal ? ERROR_TEXT : sessionError} type="error" />
            )}
          </>
        }>
        <Form
          data-testid="login-form"
          className="w-450 login-form"
          onSubmit={showMFAVerificationModal ? handleMFAVerify : handleFormSubmit}
          layout={"vertical"}>
          {showMFAVerificationModal ? renderMFAVerification : renderLogin}
        </Form>
      </Card>
    </>
  );
};

export default React.memo(LoginPage);
