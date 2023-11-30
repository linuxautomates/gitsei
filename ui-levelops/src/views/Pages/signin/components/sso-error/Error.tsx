import React, { SyntheticEvent } from "react";
import { AntText, SvgIcon } from "shared-resources/components";
import { SigninStatusType } from "../../signinTypes";
import BackButton from "../back-button/back-button";
import "./error.style.scss";

interface SSOErrorPageProps {
  signinStatus?: SigninStatusType;
  onBack: (event: SyntheticEvent) => void;
  email?: string;
  message?: string;
}
const SSOErrorPage: React.FC<SSOErrorPageProps> = ({ signinStatus, onBack, email, message }) => {
  const _onBack = (e: any) => {
    onBack(e);
  };

  return (
    <div className="sso-error-component">
      <div className="email-arrow">
        <AntText className="email">Hi </AntText>
        <AntText className="email">{signinStatus?.first_name ?? email}</AntText>
      </div>
      <AntText className="content">{message}</AntText>
      <div className="flex justify-end next-back-button">
        <BackButton onClick={_onBack} />
      </div>
    </div>
  );
};

export default SSOErrorPage;
