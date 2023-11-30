import React, { useCallback, useMemo, useRef, useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Form, Input, notification } from "antd";
import { isSanitizedValue, sanitizeObject } from "utils/commonUtils";
import queryString from "query-string";
import { sessionResetPasswordAction } from "reduxConfigs/actions/sessionActions";
import { PASSWORD_MATCH, SOMETHING_BAD_HAPPEN, TRY_AGAIN } from "constants/formWarnings";
import { formWarming } from "../common/commons.component";
import SigninButton from "./components/signin-button/signinButton";
import { HAS_ERROR } from "constants/error.constants";
import { RouteComponentProps } from "react-router-dom";
import { AntText } from "shared-resources/components";
import { validatePassword } from "./helper";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { SIGN_IN_PAGE } from "constants/routePaths";
import { get } from "lodash";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";

type SetPasswordFormType = {
  newPassword: string;
  confirmNewPassword: string;
  newPasswordValidated: boolean;
  confirmPassValidated: boolean;
  token: string;
};

type ResetPasswordFormType = {
  newPassword: string;
  confirmNewPassword: string;
  token: string;
};

const SetPasswordPage: React.FC<RouteComponentProps> = props => {
  const dispatch = useDispatch();
  const values = queryString.parse(props.location.search);
  const token = (values?.["token"] || "") as string;
  const firstname = (values?.["name"] || undefined) as string;
  const [error, setError] = useState<string>("");
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const defaultValues: SetPasswordFormType = {
    newPassword: "",
    confirmNewPassword: "",
    token,
    newPasswordValidated: false,
    confirmPassValidated: false
  };
  const resetPassValues: ResetPasswordFormType = {
    newPassword: "",
    confirmNewPassword: "",
    token
  };
  const [loading, setLoading] = useState<boolean>(false);
  const [resetForm, setResetForm] = useState<ResetPasswordFormType>(resetPassValues);
  const [formValidation, setFormValidation] = useState<SetPasswordFormType>(defaultValues);
  const newPassword = useRef<string>("");
  const [passwordClicked, setPasswordClicked] = useState<boolean>(false);
  const [confirmPasswordclicked, setConfirmPasswordClicked] = useState<boolean>(false);

  useEffect(() => {
    const values = sanitizeObject(queryString.parse(props.location.search));
    if (!values.token) {
      notification.error({ message: SOMETHING_BAD_HAPPEN, description: TRY_AGAIN });
      props.history.replace(SIGN_IN_PAGE);
    }
  }, []);

  useEffect(() => {
    const session_error = get(sessionRestState, ["session_error"], false);
    if (loading && session_error) {
      const session_error_message = get(sessionRestState, ["session_error_message"], "");
      if (session_error_message === "You are unable to proceed, this token has expired") {
        setError(session_error_message);
      }
      setLoading(false);
      notification.error({ message: session_error_message });
    }
  }, [sessionRestState]);

  useEffect(() => {
    if (passwordClicked || confirmPasswordclicked) {
      const formvalidationValue = validatePassword(resetForm.newPassword);
      if (passwordClicked) {
        setFormValidation((prev: any) => ({
          ...prev,
          newPassword: formWarming(formvalidationValue),
          newPasswordValidated: formvalidationValue.length === 0
        }));
      }
      if (confirmPasswordclicked) {
        setFormValidation((prev: any) => ({
          ...prev,
          confirmNewPassword: resetForm.newPassword !== resetForm.confirmNewPassword ? HAS_ERROR : "",
          confirmPassValidated: resetForm.newPassword !== resetForm.confirmNewPassword ? false : true
        }));
      }
    }
  }, [resetForm]);

  const handleChange = useCallback(
    (event: any) => {
      event?.persist?.();
      const field = event?.target?.id;
      if (isSanitizedValue(field)) {
        setResetForm((prev: any) => ({ ...prev, [field]: event?.target?.value || "" }));
        switch (field) {
          case "confirmNewPassword":
            setConfirmPasswordClicked(true);
            setFormValidation((prev: any) => ({
              ...prev,
              confirmNewPassword: newPassword.current === event?.target?.value ? "" : HAS_ERROR,
              confirmPassValidated: newPassword.current === event?.target?.value
            }));
            break;
          case "newPassword":
            setPasswordClicked(true);
            newPassword.current = event?.target?.value;
            const formvalidationValue = validatePassword(event?.target?.value);
            setFormValidation((prev: any) => ({
              ...prev,
              newPassword: formWarming(formvalidationValue),
              newPasswordValidated: formvalidationValue.length === 0
            }));
            break;
          default:
            break;
        }
      }
    },
    [resetForm, formValidation]
  );

  const onResetPassword = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      const params = {
        new_password: resetForm.newPassword,
        token: resetForm.token
      };
      dispatch(sessionResetPasswordAction(params, props.history));
      setLoading(true);
    },
    [resetForm]
  );

  const loginDisabled = useMemo(() => {
    return !(formValidation.newPasswordValidated && formValidation.confirmPassValidated);
  }, [formValidation]);

  const passwordWarning = useMemo(() => formWarming(PASSWORD_MATCH), []);

  return (
    <div className="set-password-form-container">
      <div className="reset-welcome-container">
        <AntText className="welcome-description">Please set a password to sign in.</AntText>
      </div>
      <Form onSubmit={onResetPassword} layout={"vertical"} data-testid="reset-form" className="set-password-form">
        <Form.Item className={"my-20 set-password"} label="Set Password" help={formValidation.newPassword}>
          <Input onChange={handleChange} type="password" value={resetForm.newPassword} id="newPassword" />
        </Form.Item>
        <AntText className="middle-text">
          Passwords must be 8 characters or more and contain an upper case letter, a number and a special character.
        </AntText>
        <Form.Item
          className="confirm-password"
          label="Confirm Password"
          help={formValidation.confirmNewPassword === HAS_ERROR ? passwordWarning : null}>
          <Input onChange={handleChange} type="password" value={resetForm.confirmNewPassword} id="confirmNewPassword" />
        </Form.Item>
        <SigninButton loading={loading} loginDisabled={loginDisabled} label={"Set Password"} />

        <div className="flex align-items-center footer-info">
          <AntText>Brought to you by</AntText>
          <SvgIconComponent className="harness-icon" icon="harnessIcon" />
          <AntText className="harness-name">harness</AntText>
        </div>
      </Form>
    </div>
  );
};

export default SetPasswordPage;
