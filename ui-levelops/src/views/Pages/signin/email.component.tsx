import React, { useEffect, useMemo, useState, Dispatch } from "react";
import { Form, Input, notification } from "antd";
import { EMAIL_WARNING } from "constants/formWarnings";
import { validateEmail } from "utils/stringUtils";
import { formWarming } from "../common/commons.component";
import SigninButton from "./components/signin-button/signinButton";
import { restValidateEmail } from "utils/restRequest";
import { SigninStatusType } from "./signinTypes";
import { isEmpty } from "lodash";
import { AntText } from "shared-resources/components";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";

interface EmailPageProps {
  signinStatus: SigninStatusType;
  setSigninStatus: Dispatch<SigninStatusType>;
  sessionError: string;
}

const EmailPage: React.FC<EmailPageProps> = ({ signinStatus, setSigninStatus, sessionError }) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [email, setEmail] = useState<string>(signinStatus.email as string);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [validEmail, setValidEmail] = useState(false);
  const [showTrobleSignIn, setShowTrobleSignIn] = useState<boolean>(false);

  useEffect(() => {
    if (!isEmpty(sessionError)) {
      setErrorMessage(sessionError);
      // setValidEmail(false);
    }

    if (validateEmail(signinStatus.email)) {
      setValidEmail(true);
    }
  }, []);

  function emailValidation() {
    setLoading(true);
    restValidateEmail(email)
      .then(response => {
        const data = response?.data;
        let token = signinStatus.token;
        let isValidEmail = data?.is_valid_email || false;
        let isMultiTenant = data?.is_multi_tenant || false;
        let isSSO = data?.is_sso_enabled || false;
        let company = data?.company || "";
        let apiErrorMessage = data?.error_message || "";
        let first_name = data?.first_name || undefined;
        if (isValidEmail) {
          setSigninStatus({
            ...signinStatus,
            email,
            company,
            isValidEmail,
            isMultiTenant,
            isSSOEnabled: isSSO,
            token,
            first_name
          });
          setEmail("");
        } else {
          if (apiErrorMessage === "Invalid email id provided") {
            setShowTrobleSignIn(true);
          } else {
            setErrorMessage(apiErrorMessage);
          }
        }
      })
      .catch(response => {
        let displayMessage = "There was an issue trying to log in. Please try again later";
        if (response && "data" in response) {
          const { message } = response.data;
          displayMessage = message;
        }
        setErrorMessage(displayMessage);
      })
      .finally(() => {
        setLoading(false);
      });
  }

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (validEmail) {
      setLoading(true);
      emailValidation();
    }
  };

  const handleChange = (event: any) => {
    const value = event.target.value;
    setEmail(value);
    let validErr = "";
    if (!validateEmail(value)) {
      validErr = EMAIL_WARNING;
    } else {
      setValidEmail(true);
    }
    setErrorMessage(validErr);
  };

  const helpText = useMemo(() => {
    return formWarming(errorMessage);
  }, [errorMessage]);

  const troubleSignInHandler = () => {
    setShowTrobleSignIn(false);
    setSigninStatus({
      ...signinStatus,
      email,
      company: "",
      isValidEmail: false,
      troublingInSignIn: true,
      first_name: undefined,
      isMultiTenant: false,
      isSSOEnabled: false,
      firstTimeSSO: true
    });
  };

  const loginOldFlow = () => {
    window.location.replace("/auth/login-page");
  };

  return (
    <div className="email-input-component">
      <Form layout={"vertical"} className="sign-in-email-form" data-testid="login-form" onSubmit={handleFormSubmit}>
        <div className="welcome-container">
          <AntText className="welcome-text">Welcome to</AntText>
          <AntText className="welcome-description">Software Engineering Insights.</AntText>
          <span className="flex">
            <AntText className="powered-by">Formerly Propelo</AntText>
            <div className="propelo-logo" />
          </span>
          <AntText className="more-info">
            Sign in and view data-led insights to remove DevOps bottlenecks, automate workflows, and improve dev
            productivity.
          </AntText>
        </div>
        <Form.Item className={"my-20 email-form label"} label="Email" help={helpText}>
          <Input onChange={handleChange} value={email} id="email" />
        </Form.Item>
        <SigninButton loading={loading} loginDisabled={!validEmail || email.length === 0} label={"Next"} />
        <div className="old-flow-wrapper">
          <AntText className="old-flow-login" onClick={loginOldFlow}>
            Try other methods to sign-in
          </AntText>
        </div>
        {showTrobleSignIn && (
          <div className="trouble-sign-in-div">
            <AntText className="trouble-sign-in">Trouble signing in?</AntText>
            <div className="trouble-question-dev" onClick={troubleSignInHandler}>
              <AntText className="trouble-question-text">
                Are you signing in for the first time and want to use your organization's SSO?
              </AntText>
            </div>
          </div>
        )}
      </Form>
    </div>
  );
};

export default EmailPage;
