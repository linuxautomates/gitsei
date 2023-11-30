import React, { SyntheticEvent, useEffect, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import { useDispatch, useSelector } from "react-redux";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { isSanitizedValue } from "utils/commonUtils";
import { SigninStatusType } from "./signinTypes";
import CompanyPage from "./components/company/company.component";
import PasswordPage from "./components/password/password.component";
import EmailPage from "./email.component";
import { get, isEmpty, isEqual } from "lodash";
import { navigateToSSO } from "utils/routeUtils";
import "./signIn.component.scss";
import { SIGNIN_STEPS, SSO_ERROR_DESCRIPTIONS } from "./constants";
import MFAPage from "./components/MFA/mfa.component";
import MFAEnrollmentPage from "./components/MFA/mfa-enrollment.component";
import { sessionErrorClear } from "reduxConfigs/actions/sessionActions";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { validateUrl } from "layouts/helper";
import { getBaseUrl } from "constants/routePaths";
import { getLocationObject } from "../helper";
import SSOErrorPage from "./components/sso-error/Error";

const SigninPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const initialSigninState = {
    token: "",
    email: "",
    company: "",
    isValidEmail: false,
    isSSOEnabled: false,
    isMultiTenant: false,
    isMFARequired: false,
    signinError: "",
    first_name: undefined,
    password: undefined,
    isMFAEnrollmentRequired: false,
    troublingInSignIn: false,
    firstTimeSSO: false
  };

  const initialSsoErrorState = {
    isError: false,
    message: ""
  };

  const [signinStatus, setSigninStatus] = useState<SigninStatusType>(initialSigninState);
  const [steps, setSteps] = useState(["email"]);
  const [ssoError, setSSOError] = useState<any>(initialSsoErrorState);
  const [sessionError, setSessionError] = useState<string>("");
  const dispatch = useDispatch();
  const sessionCurrentUser = useSelector(sessionCurrentUserState);

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
      // setVerifyingOTP(false);
    }
  }, [sessionRestState, sessionCurrentUser]);

  useEffect(() => {
    const sso_redirect_error = localStorage.getItem("sso_redirect_error");
    if (sso_redirect_error) {
      const redirectStatus = localStorage?.getItem("sign_in_status");
      const rstatus = redirectStatus?.length ? JSON?.parse(redirectStatus) : signinStatus;
      localStorage.removeItem("sso_redirect_error");
      localStorage.removeItem("sign_in_status");
      setSigninStatus({
        ...rstatus,
        isValidEmail: false,
        troublingInSignIn: true
      });
      //instead of notification showing error in component
      setSSOError({
        isError: true,
        message: get(SSO_ERROR_DESCRIPTIONS, [sso_redirect_error], sso_redirect_error)
      });
    } else {
      const values = queryString.parse(props.location.search);
      const email = (values?.email || "") as string;
      const token = (values?.token || "") as string;
      setSigninStatus({
        ...signinStatus,
        email,
        token
      });
    }
  }, []);

  useEffect(() => {
    const {
      token,
      company,
      isValidEmail,
      isSSOEnabled,
      isMultiTenant,
      isMFARequired,
      isMFAEnrollmentRequired,
      troublingInSignIn
    } = signinStatus;
    let _pageName = steps[steps.length - 1];
    if (isValidEmail) {
      if (isMultiTenant && isEmpty(company)) {
        _pageName = SIGNIN_STEPS.COMPANY;
      } else if (isMFARequired) {
        _pageName = SIGNIN_STEPS.MFA;
      } else if (isMFAEnrollmentRequired) {
        _pageName = SIGNIN_STEPS.MFA_ENROLLMENT;
      } else {
        _pageName = SIGNIN_STEPS.PASSWORD;
      }

      if (isSSOEnabled) {
        return navigateToSSO(company);
      } else {
        setSteps(steps => [...steps, _pageName]);
      }
    }
    if (troublingInSignIn && !isValidEmail) {
      _pageName = SIGNIN_STEPS.COMPANY;
      setSteps(steps => [...steps, _pageName]);
    }
    if (isEqual(signinStatus, initialSigninState)) {
      _pageName = SIGNIN_STEPS.EMAIL;
      setSteps(["email"]);
    }
  }, [signinStatus]);

  const handleBack = (e: SyntheticEvent) => {
    if (steps.length > 1) {
      dispatch(sessionErrorClear());
      const updated_steps = [...steps];
      updated_steps.pop();
      setSteps(updated_steps);
      setSSOError({
        isError: false,
        message: ""
      });
    }
  };

  return (
    <div className="sign-in-form-content">
      {ssoError?.isError ? (
        <SSOErrorPage signinStatus={signinStatus} onBack={handleBack} message={ssoError?.message} />
      ) : (
        <div>
          <EmailPage signinStatus={signinStatus} setSigninStatus={setSigninStatus} sessionError={sessionError} />
          <CompanyPage
            signinStatus={signinStatus}
            setSigninStatus={setSigninStatus}
            onBack={handleBack}
            steps={steps}
          />
          <PasswordPage
            signinStatus={signinStatus}
            setSigninStatus={setSigninStatus}
            history={props.history}
            onBack={handleBack}
            steps={steps}
          />
          <MFAPage
            onBack={handleBack}
            history={props.history}
            signinStatus={signinStatus}
            setSigninStatus={setSigninStatus}
            steps={steps}
          />
          <MFAEnrollmentPage
            onBack={handleBack}
            history={props.history}
            signinStatus={signinStatus}
            setSigninStatus={setSigninStatus}
            steps={steps}
          />
        </div>
      )}
    </div>
  );
};

export default SigninPage;
