import React, { useMemo, useState, Dispatch, SyntheticEvent, useCallback } from "react";
import { Form, Input } from "antd";
import { get, isEmpty } from "lodash";
import { EMPTY_FIELD_WARNING } from "constants/formWarnings";
import { formWarming } from "../../../common/commons.component";
import { SigninStatusType } from "../../signinTypes";
import { restValidateCompany } from "utils/restRequest";
import SigninButton from "../signin-button/signinButton";
import BackButton from "../back-button/back-button";
import { AntText } from "shared-resources/components";
import { SIGNIN_STEPS, FIRST_TIME_SSO } from "../../constants";
import FieldsContainer from "../fields-container/field-container.component";
import { buildApiUrl, VERSION } from "constants/restUri";
import "../password/password.style.scss";
interface CompanyPageProps {
  signinStatus: SigninStatusType;
  setSigninStatus: Dispatch<SigninStatusType>;
  onBack: (event: SyntheticEvent) => void;
  steps: string[];
}

const CompanyPage: React.FC<CompanyPageProps> = ({ signinStatus, setSigninStatus, onBack, steps }) => {
  const { email, company: _company } = signinStatus;
  const [loading, setLoading] = useState<boolean>(false);
  const [company, setCompany] = useState<string>(_company as string);
  const [errorMessage, setErrorMessage] = useState<string>("");

  function apiCall() {
    setLoading(true);
    restValidateCompany(email, company)
      .then(response => {
        const data = response.data;
        let apiErrorMessage = data?.error_message || "";
        let status = data?.status || false;
        let isSSO = data?.is_sso_enabled || signinStatus.isSSOEnabled;
        if (isEmpty(apiErrorMessage) && status) {
          // Success response
          setSigninStatus({
            ...signinStatus,
            company,
            isSSOEnabled: isSSO
          });
          setCompany("");
        } else {
          // error response
          setErrorMessage(apiErrorMessage || "Some error occurred, please try again!");
        }
      })
      .catch(response => {
        // TODO: test with live data
        const { message } = response.data;
        setErrorMessage(message ?? "Some error occured, please try again!");
      })
      .finally(() => {
        setLoading(false);
      });
  }

  const ssoUrl = useCallback(() => {
    localStorage.setItem("sign_in_status", JSON.stringify(signinStatus));
    window.location.replace(buildApiUrl(`/${VERSION}/generate_authn?company=${company}`));
  }, [company]);

  const handleFormSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (!errorMessage && !signinStatus.troublingInSignIn) {
      apiCall();
    } else if (signinStatus?.troublingInSignIn) {
      ssoUrl();
    }
  };

  const handleChange = (event: any) => {
    const value = event?.target?.value;
    setCompany(value);
    if (!value) {
      setErrorMessage(EMPTY_FIELD_WARNING);
    } else {
      setErrorMessage("");
    }
  };

  const loginDisabled = useMemo(() => {
    return !company?.length || errorMessage?.length;
  }, [company, errorMessage]);

  const helpText = useMemo(() => {
    return formWarming(errorMessage);
  }, [errorMessage]);

  const _onBack = (e: any) => {
    setCompany("");
    setErrorMessage("");
    onBack(e);
  };

  return (
    <FieldsContainer className="password-input-component" step={SIGNIN_STEPS.COMPANY} steps={steps}>
      <div className="mb-30 mt-80 w-90 password-welcome-text">
        <div className="email-arrow">
          <AntText className="email">Hi </AntText>
          <AntText className="email"> {signinStatus?.first_name ?? email}</AntText>
        </div>
        <AntText className="content">
          {signinStatus?.firstTimeSSO
            ? FIRST_TIME_SSO
            : `It looks like this email is used with more than one company. Which would you like to sign in with?`}
        </AntText>
      </div>
      <Form onSubmit={handleFormSubmit} layout={"vertical"} data-testid="login-form" className="signIn-password-form">
        <Form.Item className={"my-20"} id="company" label="Company" help={helpText}>
          <Input onChange={handleChange} value={company} id="company" />
        </Form.Item>
        <div className="flex next-back-button">
          <BackButton onClick={_onBack} />
          <SigninButton loading={loading} loginDisabled={loginDisabled} label={"Next"} />
        </div>
      </Form>
    </FieldsContainer>
  );
};

export default CompanyPage;
