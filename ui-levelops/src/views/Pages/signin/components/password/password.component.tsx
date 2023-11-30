import React, { useCallback, useMemo, useEffect, useState, Dispatch, SyntheticEvent } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Form, Input, notification, Row } from "antd";
import { isEmpty } from "lodash";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { sessionGetMFA } from "reduxConfigs/actions/restapi/mfa.action";
import { EMPTY_FIELD_WARNING } from "constants/formWarnings";
import { isSanitizedValue } from "utils/commonUtils";
import { formWarming } from "../../../common/commons.component";
import { SigninStatusType } from "../../signinTypes";
import SigninButton from "../signin-button/signinButton";
import { HAS_ERROR } from "constants/error.constants";
import { resetPassword } from "reduxConfigs/actions/sessionActions";
import { sessionError as setError } from "reduxConfigs/actions/sessionActions";
import BackButton from "../back-button/back-button";
import "./password.style.scss";
import { AntText } from "shared-resources/components";
import FieldsContainer from "../fields-container/field-container.component";
import { SIGNIN_STEPS } from "../../constants";
import { RestMFAState } from "reduxConfigs/types/rest-state/session-reducer/RestMFAState";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { validateUrl } from "layouts/helper";
import { getBaseUrl } from "constants/routePaths";
import { getLocationObject } from "views/Pages/helper";

type LoginFormType = {
  email: string;
  company: string;
  password: string;
};
interface PasswordPageProps {
  signinStatus: SigninStatusType;
  setSigninStatus: Dispatch<SigninStatusType>;
  history: any;
  onBack: (event: SyntheticEvent) => void;
  steps: string[];
}

const defaultValues: LoginFormType = { company: "", email: "", password: "" };

const PasswordPage: React.FC<PasswordPageProps> = ({ signinStatus, history, onBack, steps, setSigninStatus }) => {
  const dispatch = useDispatch();

  const [loading, setLoading] = useState<boolean>(false);
  const [loginForm, setLoginForm] = useState<LoginFormType>({ ...defaultValues });
  const [formValidation, setFormValidation] = useState<LoginFormType>(defaultValues);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const sessionCurrentUser = useSelector(sessionCurrentUserState);
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const sessionMFARestState: RestMFAState = useMemo(() => sessionRestState?.session_mfa, [sessionRestState]);
  useEffect(() => {
    const email = signinStatus.email as string;
    const company = signinStatus.company as string;

    const _form = { ...loginForm };

    let shouldUpdate = false;

    if (loginForm.email !== email) {
      _form.email = email;
      shouldUpdate = true;
    }

    if (loginForm.company !== company) {
      _form.company = company;
      shouldUpdate = true;
    }

    if (shouldUpdate) {
      setLoginForm(_form);
    }
  }, [signinStatus]);

  const navigationURL = (url: string, locationObject: any) => {
    const inValidURL = validateUrl(locationObject);
    if (sessionCurrentUser?.metadata?.selected_workspace) {
      const selected_workspace = sessionCurrentUser?.metadata?.selected_workspace;
      dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, selected_workspace));
    }
    if (inValidURL) {
      history.replace(getBaseUrl());
    } else {
      history.push(url);
    }
  };
  useEffect(() => {
    const { session_token, session_error, session_default_route, session_user_id, session_error_message, session_mfa } =
      sessionRestState;
    if (session_token !== null && !session_error) {
      if (session_user_id !== null) {
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
              history.push(session_default_route);
            }
          }
        }
      }
    }
    if (session_error) {
      let errMsg = session_error_message;
      if (isEmpty(errMsg)) {
        const mfaErr = session_mfa?.error;
        if (!isEmpty(mfaErr)) {
          errMsg = mfaErr?.data?.message ? mfaErr?.data?.message : "SOMETHING WENT WRONG";
        }
      }
      if (!isEmpty(errMsg)) {
        setLoading(false);
        setErrorMessage(errMsg);

        setFormValidation((prev: any) => ({
          ...prev,
          password: HAS_ERROR
        }));
      }
    } else {
      setErrorMessage("");
    }
  }, [sessionRestState, sessionCurrentUser]);

  useEffect(() => {
    if (sessionMFARestState) {
      const { loading, error } = sessionMFARestState;
      if (!loading && !error) {
        const { mfa_required, mfa_enrollment } = sessionMFARestState;
        if (mfa_enrollment) {
          setLoading(false);
          setSigninStatus({
            ...signinStatus,
            isMFAEnrollmentRequired: true,
            password: loginForm.password
          });
          setLoginForm(defaultValues);
        } else if (mfa_required) {
          setLoading(false);
          setSigninStatus({
            ...signinStatus,
            isMFARequired: true,
            password: loginForm.password
          });
          setLoginForm(defaultValues);
        }
      }
    }
  }, [sessionMFARestState]);

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    setLoading(true);
    dispatch(setError(""));
    const { email, company, password } = loginForm;
    dispatch(
      sessionGetMFA({
        email,
        company,
        password
      })
    );
  };

  const handleForgotPassword = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      if (!isEmpty(loginForm.email) && !isEmpty(loginForm.company)) {
        dispatch(resetPassword(loginForm.email, loginForm.company));
        notification.info({
          message: "Forgot Password",
          description: "Please check your email for password reset instructions"
        });
      } else {
        const errMsg = isEmpty(loginForm.email) ? "Need a valid email" : "Need a valid company";
        setFormValidation((prev: any) => ({
          ...prev,
          password: HAS_ERROR
        }));
        setErrorMessage(errMsg);
      }
    },
    [loginForm]
  );

  const handleChange = (event: any) => {
    event?.persist();
    setLoginForm((prev: any) => ({ ...prev, password: event?.target?.value || "" }));
    let validPassword = "";
    let validErr = "";
    if ((event?.target?.value || "").length === 0) {
      validPassword = HAS_ERROR;
      validErr = EMPTY_FIELD_WARNING;
    }
    setFormValidation((prev: any) => ({
      ...prev,
      password: validPassword
    }));
    setErrorMessage(validErr);
  };

  const loginDisabled = useMemo(() => {
    return !(loginForm?.password?.length && formValidation?.password !== HAS_ERROR);
  }, [loginForm, formValidation]);

  const helpText = useMemo(
    () => (formValidation.password === HAS_ERROR ? formWarming(errorMessage) : null),
    [formValidation, errorMessage]
  );

  const _onBack = (e: any) => {
    setLoginForm((prev: any) => ({ ...prev, password: "" }));
    onBack(e);
  };

  return (
    <FieldsContainer className="password-input-component" steps={steps} step={SIGNIN_STEPS.PASSWORD}>
      <div className="mb-30 mt-80 w-90 password-welcome-text">
        <div className="email-arrow">
          <AntText className="email">Hi </AntText>
          <AntText className="email">{signinStatus?.first_name ?? loginForm.email}</AntText>
        </div>
        <AntText className="content">Please enter your password to sign in to SEI.</AntText>
      </div>
      <Form onSubmit={handleFormSubmit} layout={"vertical"} data-testid="login-form" className="signIn-password-form">
        <Form.Item
          className={"my-20"}
          label={
            <span className="custom-label">
              <span>Password</span>
              <span className="forget-password-button" onClick={handleForgotPassword}>
                Forgot Password?
              </span>{" "}
            </span>
          }
          help={helpText}>
          <Input onChange={handleChange} type="password" value={loginForm.password} id="password" />
        </Form.Item>
        <div className="flex justify-end next-back-button">
          <BackButton onClick={_onBack} />
          <SigninButton loading={loading} loginDisabled={loginDisabled} label={"Next"} />
        </div>
      </Form>
    </FieldsContainer>
  );
};

export default PasswordPage;
