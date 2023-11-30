import React, { useMemo, useState, Dispatch, SyntheticEvent, useEffect } from "react";
import { Form, Input } from "antd";
import { EMPTY_FIELD_WARNING, MFA_CODE_INVALID } from "constants/formWarnings";
import { formWarming } from "../../../common/commons.component";
import { SigninStatusType } from "../../signinTypes";
import SigninButton from "../signin-button/signinButton";
import { AntText } from "shared-resources/components";
import { SIGNIN_STEPS } from "../../constants";
import FieldsContainer from "../fields-container/field-container.component";
import "./mfa.component.scss";
import { checkMFAValidation } from "utils/regexUtils";
import { useDispatch, useSelector } from "react-redux";
import { sessionLogout } from "reduxConfigs/actions/sessionActions";
import {
  sessionMFAEnroll,
  sessionMFAEnrollClear,
  sessionMFAEnrollGet,
  sessionMFAEnrollPost
} from "reduxConfigs/actions/restapi/mfa.action";
import {
  RestMFAEnrollGetState,
  RestMFAEnrollPostState,
  RestMFAEnrollState
} from "reduxConfigs/types/rest-state/session-reducer/RestMFAEnrollState";
import { sessionMFAEnrollSelector } from "reduxConfigs/selectors/session_mfa.selector";
import { SIGN_IN_PAGE } from "constants/routePaths";

interface MFAEnrollmentPageProps {
  signinStatus: SigninStatusType;
  setSigninStatus: Dispatch<SigninStatusType>;
  onBack: (event: SyntheticEvent) => void;
  steps: string[];
  history: any;
}

const MFAEnrollmentPage: React.FC<MFAEnrollmentPageProps> = ({ signinStatus, steps, history, setSigninStatus }) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [mfaCode, setMfaCode] = useState<string>("" as string);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const dispatch = useDispatch();
  const restSessionMFAEnroll: RestMFAEnrollState = useSelector(sessionMFAEnrollSelector);
  const restSessionEnrollGet: RestMFAEnrollGetState = useMemo(() => restSessionMFAEnroll?.get, [restSessionMFAEnroll]);
  const restSessionEnrollPost: RestMFAEnrollPostState = useMemo(
    () => restSessionMFAEnroll?.post,
    [restSessionMFAEnroll]
  );

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (!errorMessage) {
      setLoading(true);
      dispatch(sessionMFAEnroll("post", { loading: false, error: false }));
      dispatch(sessionMFAEnrollPost({ otp: mfaCode }));
    }
  };

  useEffect(() => {
    const { isMFAEnrollmentRequired } = signinStatus;
    if (isMFAEnrollmentRequired) {
      dispatch(sessionMFAEnrollClear());
      dispatch(sessionMFAEnrollGet());
    }
    return () => {
      dispatch(sessionMFAEnrollClear());
    };
  }, [signinStatus]);

  useEffect(() => {
    if (restSessionEnrollPost) {
      const { loading, error, enrollment_success } = restSessionEnrollPost;
      if (error && !loading) {
        setErrorMessage(error as string);
        setLoading(false);
      } else if (!loading && !error && enrollment_success) {
        dispatch(sessionLogout());
        setLoading(false);
        setSigninStatus({
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
          troublingInSignIn: false
        });
        history.push(SIGN_IN_PAGE);
      }
    }
  }, [restSessionEnrollPost]);

  const handleChange = (event: any) => {
    const value = event?.target?.value;
    setMfaCode(value);
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
    return !(mfaCode?.length === 6);
  }, [mfaCode]);

  const helpText = useMemo(() => {
    return formWarming(errorMessage);
  }, [errorMessage]);

  return (
    <FieldsContainer className="mfa-enrollment-component" step={SIGNIN_STEPS.MFA_ENROLLMENT} steps={steps}>
      <div className="mb-30 w-90 mfa-welcome-text">
        <div className="email-arrow">
          <AntText className="email">Setup MFA</AntText>
        </div>
        <AntText className="content">
          Use an app on your phone to get two-factor authentication codes when prompted. We recommend cloud-based TOTP
          apps such as Google Authenticator, LassPass Authenticator or Microsoft Authenticator.
        </AntText>
      </div>
      <div className="qr-help-container">
        <AntText className="qr-content">Scan the image (QR-code) below with your authentication app.</AntText>
        <div className="qr-container">
          <img src={restSessionEnrollGet?.qrcode} alt="QR" className="qr-image" />
        </div>
        <AntText className="qr-content">Alternatively, enter this text code.</AntText>
        <div className="help-code">
          <Input onChange={handleChange} value={restSessionEnrollGet?.code} />
        </div>
      </div>
      <form onSubmit={handleFormSubmit}>
        <Form layout={"vertical"} data-testid="login-form" className="signIn-mfa-form">
          <Form.Item className={"my-20"} label="MFA" help={helpText}>
            <Input onChange={handleChange} value={mfaCode} />
          </Form.Item>
          <div className="flex next-back-button">
            <SigninButton loading={loading} loginDisabled={loginDisabled} label={"Verify Code"} />
          </div>
        </Form>
      </form>
    </FieldsContainer>
  );
};

export default MFAEnrollmentPage;
