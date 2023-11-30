import React, { useMemo, useState, Dispatch, SyntheticEvent, useEffect } from "react";
import { Form, Input } from "antd";
import { EMPTY_FIELD_WARNING, MFA_CODE_INVALID } from "constants/formWarnings";
import { formWarming } from "../../../common/commons.component";
import { SigninStatusType } from "../../signinTypes";
import SigninButton from "../signin-button/signinButton";
import { AntText, SvgIcon } from "shared-resources/components";
import { SIGNIN_STEPS } from "../../constants";
import FieldsContainer from "../fields-container/field-container.component";
import { checkMFAValidation } from "utils/regexUtils";
import { useDispatch, useSelector } from "react-redux";
import { sessionLogin } from "reduxConfigs/actions/sessionActions";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { isSanitizedValue } from "utils/commonUtils";
import "../password/password.style.scss";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { validateUrl } from "layouts/helper";
import { getBaseUrl } from "constants/routePaths";
import { getLocationObject } from "views/Pages/helper";
interface MFAPageProps {
  signinStatus: SigninStatusType;
  setSigninStatus: Dispatch<SigninStatusType>;
  onBack: (event: SyntheticEvent) => void;
  steps: string[];
  history: any;
}

const MFAPage: React.FC<MFAPageProps> = ({ signinStatus, setSigninStatus, onBack, steps, history }) => {
  const { email } = signinStatus;
  const [loading, setLoading] = useState<boolean>(false);
  const [mfa, setMfa] = useState<string>("" as string);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const dispatch = useDispatch();
  const sessionCurrentUser = useSelector(sessionCurrentUserState);
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (!errorMessage) {
      setLoading(true);
      const { email, company, password } = signinStatus;
      dispatch(sessionLogin(email, password, company, mfa));
    }
  };

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

    const mfaErr = session_mfa?.error;
    if (mfaErr) {
      setErrorMessage(MFA_CODE_INVALID);
    } else {
      setErrorMessage("");
    }

    if (loading) {
      setLoading(false);
    }
  }, [sessionRestState, sessionCurrentUser]);

  const handleChange = (event: any) => {
    const value = event?.target?.value;
    setMfa(value);
    const validMFA = checkMFAValidation(value);
    if (!value) {
      setErrorMessage(EMPTY_FIELD_WARNING);
    } else if (!validMFA) {
      setErrorMessage(MFA_CODE_INVALID);
    } else {
      setErrorMessage("");
    }
  };

  const loginDisabled = useMemo(() => {
    return !(mfa?.length === 6) || errorMessage?.length;
  }, [mfa, errorMessage]);

  const helpText = useMemo(() => {
    return formWarming(errorMessage);
  }, [errorMessage]);

  return (
    <FieldsContainer className="password-input-component" step={SIGNIN_STEPS.MFA} steps={steps}>
      <div className="mb-30 mt-80 w-90 mfa-welcome-text">
        <div className="email-arrow">
          <AntText className="email"> {signinStatus?.first_name ?? email}</AntText>
        </div>
        <AntText className="content">
          Open the two-factor authentication app on your device to view your authentication code and verify your
          identity.
        </AntText>
      </div>
      <Form onSubmit={handleFormSubmit} layout={"vertical"} data-testid="login-form" className="signIn-password-form">
        <Form.Item className={"my-20"} label="MFA" help={helpText}>
          <Input onChange={handleChange} value={mfa} />
        </Form.Item>
        <div className="flex justify-end next-back-button">
          <SigninButton loading={loading} loginDisabled={loginDisabled} label={"Verify Code"} />
        </div>
      </Form>
    </FieldsContainer>
  );
};

export default MFAPage;
