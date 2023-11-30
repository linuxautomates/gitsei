import React from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import "./signIn.component.scss";
import SigninPage from "./signin.component";
import SetPasswordPage from "./set-password.component";

const SigninWrapper: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const values = queryString.parse(props.location.search);
  const setPassword = Object.keys(values || {})?.includes("set-password");
  return (
    <div className="sign-in-form-content">
      {setPassword ? <SetPasswordPage {...props} /> : <SigninPage {...props} />}
    </div>
  );
};

export default SigninWrapper;
