import React, { useEffect, useState } from "react";
import { Form } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";

import { AntForm, AntFormItem, AntInput, AntModal, AntSelect } from "shared-resources/components";
import { APIKEY_TYPE_ADMIN, RestApikey } from "../../../classes/RestApikey";
import { toTitleCase } from "../../../utils/stringUtils";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { apiKeyCreateState } from "reduxConfigs/selectors/apiKeysSelector";
import { restAPILoadingState } from "../../../utils/stateUtil";
import { formClear, formUpdateObj } from "reduxConfigs/actions/formActions";
import { apikeysCreate } from "reduxConfigs/actions/restapi";
import { getApikeyForm } from "reduxConfigs/selectors/formSelector";

interface ApikeyCreateContainerProps {
  onOk: (apiKey: any) => void;
  onCancel: any;
  form?: any;
}

export const ApikeyCreateContainer: React.FC<ApikeyCreateContainerProps> = (props: ApikeyCreateContainerProps) => {
  const API_KEY_FORM_NAME = "apikey_form";

  const [creating, setCreating] = useState(false);

  const dispatch = useDispatch();
  const apiKeyCreate = useSelector(apiKeyCreateState);
  const apiKeyFarm = useSelector(getApikeyForm);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("apikeys", "create", "0"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (creating) {
      let isLoading = true;
      let apiKey = undefined;
      const { loading, error } = restAPILoadingState(apiKeyCreate);
      if (!loading) {
        isLoading = false;
        if (!error) {
          apiKey = get(apiKeyCreate, ["0", "data"], {});
        }
        setCreating(isLoading);
        props.onOk(apiKey);
      }
    }
  }, [apiKeyCreate]); // eslint-disable-line react-hooks/exhaustive-deps

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      const apiKey = Object.assign(Object.create(Object.getPrototypeOf(apiKeyFarm)), apiKeyFarm);
      apiKey[field] = e.target ? e.target.value : e;
      dispatch(formUpdateObj(API_KEY_FORM_NAME, apiKey));
    };
  };

  const onCancel = () => {
    dispatch(formClear(API_KEY_FORM_NAME));
    props.form.resetFields();
    props.onCancel();
  };

  const onOK = () => {
    const apiKey = Object.assign(Object.create(Object.getPrototypeOf(apiKeyFarm)), apiKeyFarm);
    setCreating(true);
    dispatch(apikeysCreate(apiKey));
    dispatch(formClear(API_KEY_FORM_NAME));
    props.form.resetFields();
  };

  const { getFieldDecorator, getFieldError, isFieldTouched } = props.form;
  return (
    <AntModal
      title="Create Apikey"
      visible
      onOk={onOK}
      onCancel={onCancel}
      okText={"Create"}
      closable={false}
      okButtonProps={{
        disabled: !apiKeyFarm.valid
      }}>
      <AntForm layout={"vertical"}>
        <AntFormItem label={"Name"} validateStatus={isFieldTouched("name") && getFieldError("name") ? "error" : ""}>
          {getFieldDecorator("name", {
            initialValue: apiKeyFarm.name || "",
            validateTrigger: "onBlur",
            rules: [
              {
                required: true,
                message: "This field cannot be empty"
              }
            ]
          })(<AntInput data-testid="apikey-name" onChange={onFieldChangeHandler("name")} />)}
        </AntFormItem>
        <AntFormItem
          label={"Description"}
          validateStatus={isFieldTouched("description") && getFieldError("description") ? "error" : ""}>
          {getFieldDecorator("description", {
            initialValue: apiKeyFarm.description || "",
            validateTrigger: "onBlur",
            rules: [
              {
                required: true,
                message: "This field cannot be empty"
              }
            ]
          })(<AntInput data-testid="apikey-description" onChange={onFieldChangeHandler("description")} />)}
        </AntFormItem>
        <AntFormItem label={"Role"} validateStatus={isFieldTouched("role") && getFieldError("role") ? "error" : ""}>
          {getFieldDecorator("role", {
            initialValue: apiKeyFarm.role || APIKEY_TYPE_ADMIN,
            validateTrigger: "onBlur",
            rules: [
              {
                required: true,
                message: "This field cannot be empty"
              }
            ]
          })(
            <AntSelect
              data-testid="apikey-role"
              options={RestApikey.ROLES.map(option => ({ label: toTitleCase(option), value: option }))}
              value={apiKeyFarm.role}
              onChange={onFieldChangeHandler("role")}
            />
          )}
        </AntFormItem>
      </AntForm>
    </AntModal>
  );
};

const ApikeyForm = Form.create({ name: "templates_add" })(ApikeyCreateContainer);

export default ApikeyForm;
