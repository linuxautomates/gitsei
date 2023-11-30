import React, { useEffect, useState } from "react";
import { connect, useDispatch } from "react-redux";
import { RouteComponentProps } from "react-router-dom";

import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { AntButton, AntCard, AntFormItem, AntInput, AntSwitch, AntText } from "shared-resources/components";
import { Form, Icon, Upload } from "antd";
import { RestSamlconfig } from "../../../../classes/RestSamlconfig";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import ErrorWrapper from "hoc/errorWrapper";
import { validateURL } from "utils/stringUtils";
import { getError, getLoading } from "../../../../utils/loadingUtils";
import { urlPattern } from "../../../../utils/stringUtils";
import "./sso-settings.style.scss";
import { DOCS_ROOT, DOCS_PATHS } from "constants/docsPath";
import { getSettingsPage } from "constants/routePaths";

interface SsoSettingsProps extends RouteComponentProps {
  form?: any;
  restapiClear: any;
  rest_api: any;
  history: any;
  samlssoGet: any;
  samlssoUpdate: any;
}

export function SsoSettingsPage(props: SsoSettingsProps) {
  const [samlSsoConfig, setSamlSsoConfig] = useState(new RestSamlconfig());
  const [loading, setLoading] = useState(true);
  const [updateLoading, setUpdateLoading] = useState(false);
  const [IdpCertValid, setIdpCertValid] = useState(false); // eslint-disable-line @typescript-eslint/no-unused-vars
  const dispatch = useDispatch();

  const fields = [
    {
      prefix: "sso",
      name: "sp_entity_id",
      label: "SP Entity ID",
      isReadOnly: true,
      value: samlSsoConfig.spId || ""
    },
    {
      prefix: "sso",
      name: "acs_url",
      label: "ACS Url",
      isReadOnly: true,
      value: samlSsoConfig.acsUrl || ""
    },
    {
      prefix: "sso",
      name: "default_relay_state",
      label: "Default Relay State",
      isReadOnly: true,
      value: samlSsoConfig.defaultRelayState || ""
    },
    {
      prefix: "sso",
      name: "idp_entity_id",
      label: "IDP Entity ID",
      isRequired: true,
      hasError: samlSsoConfig.idpId === "",
      value: samlSsoConfig.idpId || ""
    },
    {
      prefix: "sso",
      name: "idp_sso_url",
      label: "IDP SSO Url",
      isRequired: true,
      hasError: !validateURL(samlSsoConfig.idpSsoUrl),
      matchPattern: urlPattern,
      patternErrorMessage: "This is not a valid URL",
      value: samlSsoConfig.idpSsoUrl || ""
    },
    {
      prefix: "sso",
      name: "idp_cert",
      label: "IDP Certificate",
      isRequired: true,
      rows: 15,
      type: "textarea",
      hasError: IdpCertValid,
      value: samlSsoConfig.idpCert || ""
    }
  ];

  // const formItemLayout = {
  //   labelCol: {
  //     xs: { span: 24 },
  //     sm: { span: 6 }
  //   },
  //   wrapperCol: {
  //     xs: { span: 24 },
  //     sm: { span: 18 }
  //   }
  // };

  useEffect(() => {
    props.samlssoGet(0);

    return () => {
      dispatch(restapiClear("apikeys", "create", "0"));
      props.restapiClear("samlsso", "get", "0");
      props.restapiClear("samlsso", "put", "0");
      props.restapiClear("samlsso", "delete", "0");
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loading) {
      if (props.rest_api.samlsso?.get.hasOwnProperty("0")) {
        if (!getLoading(props.rest_api, "samlsso", "get", "0") && !getError(props.rest_api, "samlsso", "get", "0")) {
          let sso = new RestSamlconfig(props.rest_api.samlsso.get["0"].data);
          setLoading(false);
          setSamlSsoConfig(sso);
        }
      }
    }

    if (updateLoading) {
      if (
        !getLoading(props.rest_api, "samlsso", "update", "0") &&
        !getError(props.rest_api, "samlsso", "update", "0")
      ) {
        props.history.push(getSettingsPage());
      }
    }
  }, [props.rest_api.samlsso]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleEnableChange = () => {
    return () => {
      let samlSSO = new RestSamlconfig(samlSsoConfig.json());
      samlSSO.enabled = !samlSSO.enabled;
      setSamlSsoConfig(samlSSO);
    };
  };

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      let samlSSO = samlSsoConfig;
      let value = e.target.value;

      switch (field) {
        case "idp_sso_url":
          samlSSO.idpSsoUrl = value;
          break;
        case "idp_entity_id":
          samlSSO.idpId = value;
          break;
        case "idp_cert":
          samlSSO.idpCert = value;
          break;
        default:
          break;
      }

      setSamlSsoConfig(samlSSO);
    };
  };

  const beforeUpload = (file: any) => {
    const reader = new FileReader();

    reader.onload = () => {
      let samlSSO = new RestSamlconfig(samlSsoConfig.json());
      if (reader.result) {
        samlSSO.idpCert = reader.result.toString();
      }

      setSamlSsoConfig(samlSSO);
    };

    reader.readAsText(file);
    return false;
  };

  const handleUpdate = () => {
    return () => {
      setUpdateLoading(true);
    };
  };

  useEffect(() => {
    if (updateLoading) {
      props.samlssoUpdate(0, samlSsoConfig);
    }
  }, [updateLoading]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className={`flex direction-column align-center`}>
      <div className={`sso-page__content`}>
        <AntCard
          title="SSO Configutations"
          extra={
            <a href={DOCS_ROOT + DOCS_PATHS.SSO} target="_blank" rel="noopener noreferrer">
              Configuration Guide
            </a>
          }>
          <Form layout={"vertical"}>
            <AntText>{`SSO `}</AntText>
            <AntSwitch
              checkedChildren="ON"
              unCheckedChildren="OFF"
              defaultChecked
              onChange={handleEnableChange()}
              checked={samlSsoConfig.enabled}
            />

            <br />
            <br />

            {fields.map(field => (
              <AntFormItem
                label={field.label}
                key={field.name}
                validateStatus={
                  props.form.isFieldTouched(field.name) && props.form.getFieldError(field.name) ? "error" : ""
                }
                colon={false}>
                {props.form.getFieldDecorator(field.name, {
                  initialValue: field.value,
                  validateTrigger: "onBlur",
                  rules: [
                    {
                      required: field.isRequired,
                      message: "This field cannot be empty"
                    },
                    {
                      pattern: field.matchPattern,
                      message: field.patternErrorMessage
                    }
                  ]
                })(
                  field.isReadOnly ? (
                    <AntText copyable>{field.value}</AntText>
                  ) : (
                    <AntInput type={field.type} onChange={onFieldChangeHandler(field.name)} />
                  )
                )}
              </AntFormItem>
            ))}

            <Upload
              beforeUpload={beforeUpload}
              customRequest={() => {
                return;
              }}
              showUploadList={false}>
              <AntButton>
                <Icon type="upload" /> Upload IDP Certificate
              </AntButton>
            </Upload>

            <br />
            <br />

            <AntFormItem style={{ float: "right" }}>
              <AntButton type="primary" onClick={handleUpdate()} htmlType="submit">
                Update SSO Settings
              </AntButton>
            </AntFormItem>
          </Form>
        </AntCard>
      </div>
    </div>
  );
}

const SsoForm = Form.create({ name: "sso_settings" })(SsoSettingsPage);
export default ErrorWrapper(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SsoForm));
