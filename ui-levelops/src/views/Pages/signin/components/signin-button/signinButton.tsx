import React from "react";
import { Button, Spin } from "antd";
import Loader from "components/Loader/Loader";
import "./signinButton.scss";

const SigninButton = (props: any) => {
  const { loginDisabled, loading, label = "SIGN IN" } = props;
  const disabled = loginDisabled ? "disabled" : "";
  return (
    <div className="signin-button-wrapper flex justify-center">
      {loading && (
        <div className="signup-button-loader flex justify-center align-center">
          <Spin className="signup-button-loader__spin" />
        </div>
      )}
      {!loading && (
        <Button
          className={"signin-button " + disabled}
          size="default"
          type="primary"
          htmlType={"submit"}
          disabled={loginDisabled}
          block>
          {label}
        </Button>
      )}
    </div>
  );
};

export default SigninButton;
