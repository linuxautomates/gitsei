import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import { isSanitizedValue, sanitizeObject } from "utils/commonUtils";
import queryString from "query-string";
import { useDispatch, useSelector } from "react-redux";
import { sessionResetPasswordAction } from "reduxConfigs/actions/sessionActions";
import { Button, Card, Divider, Form, Input, notification, Typography } from "antd";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import Loader from "components/Loader/Loader";
import { EMPTY_FIELD_WARNING, SOMETHING_BAD_HAPPEN, PASSWORD_MATCH, TRY_AGAIN } from "constants/formWarnings";
import { formWarming } from "../common/commons.component";
import "./reset-password.style.scss";
import { SIGN_IN_PAGE } from "constants/routePaths";

const { Title, Paragraph } = Typography;

type ResetPasswordFormType = {
  newPassword: string;
  confirmNewPassword: string;
};

const defaultValues: ResetPasswordFormType = { newPassword: "", confirmNewPassword: "" };

const HAS_ERROR = "hasError";

const ResetPassword: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const dispatch = useDispatch();
  const sessionRestState = useSelector(mapSessionStatetoProps);

  const [loading, setLoading] = useState<boolean>(false);
  const [resetForm, setResetForm] = useState<ResetPasswordFormType>(defaultValues);
  const [formValidation, setFormValidation] = useState<ResetPasswordFormType>(defaultValues);
  const newPassword = useRef<string>("");

  useEffect(() => {
    const values = sanitizeObject(queryString.parse(props.location.search));
    if (!values.token) {
      notification.error({ message: SOMETHING_BAD_HAPPEN, description: TRY_AGAIN });
      props.history.push(SIGN_IN_PAGE);
    }
  }, []);

  const handleChange = useCallback(
    (event: any) => {
      event?.persist?.();
      const field = event?.target?.id;
      if (isSanitizedValue(field)) {
        setResetForm((prev: any) => ({ ...prev, [field]: event?.target?.value || "" }));
        switch (field) {
          case "confirmNewPassword":
            setFormValidation((prev: any) => ({
              ...prev,
              confirmNewPassword: newPassword.current === event?.target?.value ? "" : HAS_ERROR
            }));
            break;
          case "newPassword":
            newPassword.current = event?.target?.value;
            setFormValidation((prev: any) => ({
              ...prev,
              newPassword: (event?.target?.value || "").length > 0 ? "" : HAS_ERROR
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
      if (
        Object.keys(sanitizeObject(resetForm) || {}).length === 2 &&
        Object.keys(sanitizeObject(formValidation) || {}).length === 0
      ) {
        const values = queryString.parse(props.location.search);
        if (Object.keys(sanitizeObject(values) || {}).length > 0) {
          const params = {
            new_password: resetForm.newPassword,
            token: values.token,
            username: values.username,
            company: values.tenant
          };
          dispatch(sessionResetPasswordAction(params, props.history));
          setLoading(true);
        }
      }
    },
    [resetForm]
  );

  const loader = useMemo(
    () => (
      <div data-testid="loader">
        <Loader />
      </div>
    ),
    []
  );

  const passwordWarning = useMemo(() => formWarming(PASSWORD_MATCH), []);
  const emptyWarning = useMemo(() => formWarming(EMPTY_FIELD_WARNING), []);

  return (
    <Card
      title={
        <>
          <Title level={3} className="mb-0 text-center">
            Reset Password
          </Title>
          <Paragraph type={"danger"} className="mb-0 text-center">
            {sessionRestState.session_error_message}
          </Paragraph>
        </>
      }>
      <Form layout={"vertical"} data-testid="reset-form" className="w-450" onSubmit={onResetPassword}>
        <Form.Item label="Password" help={formValidation.newPassword === HAS_ERROR ? emptyWarning : null}>
          <Input
            onChange={handleChange}
            type="password"
            value={resetForm.newPassword}
            id="newPassword"
            placeholder="Password"
          />
        </Form.Item>
        <Form.Item
          label="Confirm Password"
          help={formValidation.confirmNewPassword === HAS_ERROR ? passwordWarning : null}>
          <Input
            onChange={handleChange}
            type="password"
            value={resetForm.confirmNewPassword}
            id="confirmNewPassword"
            placeholder="Confirm Password"
          />
        </Form.Item>
        <Divider />
        <div className="flex justify-center">
          <>
            {loading && loader}
            {!loading && (
              <Button className=" mr-10" data-testid="reset" id="reset" type="primary" htmlType={"submit"}>
                Reset Password
              </Button>
            )}
          </>
        </div>
      </Form>
    </Card>
  );
};

export default React.memo(ResetPassword);
