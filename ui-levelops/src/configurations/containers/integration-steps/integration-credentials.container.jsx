import React from "react";
import { connect } from "react-redux";
import { Row, Col } from "antd";
import { AntCard, AntInput, AntForm, AntFormItem, AntCheckbox } from "shared-resources/components";
import { mapIntegrationStatetoProps, mapIntegrationDispatchtoProps } from "reduxConfigs/maps/integrationMap";
import { getIntegrationUrlMap } from "constants/integrations";
import { CredentialOauth } from "configurations/components/integrations";
import { validateURL, validatePort } from "../../../utils/stringUtils";
import { URL_WARNING, EMPTY_FIELD_WARNING, SUCCESS, IP_WARNING } from "constants/formWarnings";
import { ERROR } from "../../../constants/formWarnings";
import { GenericFormComponent } from "../../../shared-resources/containers";
import { get } from "lodash";
import { Tooltip } from "antd";
import Icon from "antd/lib/icon";
import "./integration.style.scss";
import { timezones } from "utils/timezones.utils";
import ArrayInput from "./integration-array-input-type.component";
import { IntegrationTypes } from "constants/IntegrationTypes";

export class IntegrationCredentialsContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      dirtyKeys: []
    };
    this.validateField = this.validateField.bind(this);
    this.updateMetadata = this.updateMetadata.bind(this);
    this.getFieldValue = this.getFieldValue.bind(this);
    this.getFieldType = this.getFieldType.bind(this);
    this.updateSSLFingerPrint = this.updateSSLFingerPrint.bind(this);
    this.integrationMap = getIntegrationUrlMap();
  }

  componentDidMount() {
    const integration = this.integrationMap[this.props.integration_type];
    integration.form_fields.forEach(field => {
      if (field.hasOwnProperty("defaultValue")) {
        this.props.integrationForm({ [field.key]: field.defaultValue });
      }
    });

    (integration.metadata_fields || []).forEach(field => {
      // setting options default values
      if (field.key === "options") {
        let metadata = {};
        field.options.forEach(option => {
          if (option.hasOwnProperty("defaultValue")) {
            metadata = {
              ...metadata,
              [option.value]: option.defaultValue
            };
            if (option.value === "fetch_commits" && option.defaultValue) {
              metadata = {
                ...metadata,
                fetch_commit_files: true
              };
            }
          }
        });
        this.props.integrationForm({ metadata: metadata });
      }
    });
  }

  updateField(key, value) {
    const dirtyIndex = this.state.dirtyKeys.findIndex(v => v === key);
    if (dirtyIndex === -1) {
      this.setState(state => ({ ...state, dirtyKeys: [...state.dirtyKeys, key] }));
    }
    this.props.integrationForm({ [key]: value });
  }

  updateSSLFingerPrint(key, value) {
    const metadata = { ...this.props.integration_form["metadata"] };
    this.props.integrationForm({ metadata: { ...metadata, [key]: value } });
  }

  getFieldValue(currentValue, defaultValue, key) {
    if (!defaultValue) {
      return currentValue || "";
    }
    const dirtyIndex = this.state.dirtyKeys.findIndex(v => v === key);
    if (dirtyIndex === -1) {
      return defaultValue;
    }
    return currentValue;
  }

  updateMetadata(values, key) {
    const isFetchCommit = values.options?.value?.[0]?.includes("fetch_commits");
    // updates hiddenOptions to set fetch_commit_files to false
    if (!isFetchCommit && values.hiddenOptions?.value) {
      values.hiddenOptions.value[0] = values.hiddenOptions.value.flat().filter(v => v !== "fetch_commit_files");
    }

    const isPushBased = values.options?.value?.[0]?.includes("is_push_based");

    if (!isPushBased && values.autoRegister?.value) {
      values.autoRegister.value[0] = values.autoRegister.value[0].filter(v => v != "auto_register_webhook");
    }

    if (key === "options" && isPushBased && !values?.autoRegister?.value?.[0]?.[0]) {
      values["autoRegister"] = {
        value: [["auto_register_webhook"]]
      };
    }

    const metadata = values.elements.reduce((acc, obj) => {
      switch (obj.type) {
        case "checkbox-group":
          const keys = get(values, [obj.key, "value", 0], []);
          const hiddenKeys = get(values, ["hiddenOptions", "value"], []);
          const autoRegister = get(values, ["autoRegister", "value", 0], []);
          keys.forEach(val => {
            acc[val] = true;
          });
          if (hiddenKeys.length) {
            hiddenKeys.flat().forEach(val => {
              if (val.length) {
                acc[val] = true;
              }
            });
          }
          autoRegister.forEach(val => {
            acc[val] = true;
          });
          break;
        case "text":
          acc[obj.key] = get(values, [obj.key, "value", 0], "");
          break;
        case "comma-multi-select":
          acc[obj.key] = get(values, [obj.key, "value"], "");
          break;
        default:
          if (obj.key === "timezone") {
            const timeKeys = values[obj.key].value || [];
            let timeValues = [];
            timeKeys.forEach(timekey => {
              timezones.forEach(elements => {
                if (elements.label === timekey) timeValues.push(elements.value);
              });
            });
            acc[obj.key] = timeValues.length ? (timeValues.length === 1 ? timeValues[0] : timeValues) : "";
          } else acc[obj.key] = values[obj.key].value || [];
      }
      return acc;
    }, {});
    this.props.integrationForm({ metadata: metadata });
  }

  validateField(field, value, isRequired = true) {
    if (!this.state[field] || !isRequired) {
      return "";
    }
    switch (field) {
      case "url":
      case "swarmurl":
        return validateURL(value) ? "" : ERROR;
      case "port":
        return validatePort(value) ? "" : ERROR;
      case "server":
      case "ip":
      case "apikey":
      case "company":
      case "database":
      case "password":
      case "username":
        if (value !== undefined && value !== null && value !== "") {
          return "";
        } else {
          return ERROR;
        }
      default:
        return SUCCESS;
    }
  }

  helpText(field) {
    if (field.errorText) {
      return field.errorText;
    }

    switch (field.key) {
      case "server":
        return IP_WARNING;
      case "url":
        return URL_WARNING;
      case "apikey":
      case "username":
      default:
        return EMPTY_FIELD_WARNING;
    }
  }

  placeHolder(field) {
    switch (field) {
      case "swarmurl":
      case "url":
        return "https://yourintegrationurl.com";
      case "apikey":
        return "Integration api key";
      case "username":
        return "user@integration.com";
      case "company":
        return "Your company name";
      case "database":
        return "Your database name";
      case "server":
        return "101.152.176.91";
      case "password":
        return "a****z";
      case "ip":
        return "Server name or IP address.";
      case "port":
        return "Integration Port";
      default:
        return "";
    }
  }

  getFieldType(field) {
    if (field.hasOwnProperty("type")) {
      return ["search", "textarea", "password", "group", "number"].includes(field.type) ? field.type : "text";
    } else {
      return field.key.toLowerCase() === "password" ? "password" : "text";
    }
  }

  render() {
    const fields =
      ["form", "hybrid", "oauth"].includes(this.integrationMap[this.props.integration_type].type.toLowerCase()) &&
      this.integrationMap[this.props.integration_type].form_fields
        .filter(field => !field?.arrayType)
        .map(field => ({
          ...field,
          value: this.getFieldValue(this.props.integration_form[field.key], field.defaultValue, field.key)
        }));

    const metadataFields = get(this.integrationMap, [this.props.integration_type, "metadata_fields"], []);
    const configDocUrl = get(this.integrationMap, [this.props.integration_type, "config_docs_url"], undefined);

    metadataFields.forEach(element => {
      if (element.key === "timezone") element.required = this.props.integration_form.satellite;
    });

    const arrayFields =
      ["form", "hybrid", "oauth"]?.includes(this.integrationMap[this.props.integration_type]?.type?.toLowerCase()) &&
      this.integrationMap[this.props.integration_type].form_fields
        .filter(field => !!field?.arrayType)
        .map(field => ({
          ...field,
          value: this.getFieldValue(this.props.integration_form[field.key], field.defaultValue, field.key)
        }));

    return (
      <AntCard style={{ margin: "20px" }}>
        {this.integrationMap[this.props.integration_type].type.toLowerCase() === "oauth" && (
          <CredentialOauth type={this.props.integration_type} setIntegrationState={this.props.integrationState} />
        )}
        <div className="configuration-guide-container">
          {this.integrationMap[this.props.integration_type].type.toLowerCase() === "form" && (
            <div className="row">
              <Row type={"flex"} justify={"center"}>
                <Col span={12}>
                  <AntForm layout={"vertical"}>
                    {this.props.integration_type !== "tenable" && (
                      <AntCheckbox
                        checked={this.props.integration_form.satellite}
                        onChange={e => this.updateField("satellite", e.target.checked)}>
                        Satellite Integration
                      </AntCheckbox>
                    )}
                    {this.props.integration_form.satellite && (
                      <AntFormItem
                        label={"Enter integration credentials to generate yml file. Credentials will not be stored"}
                      />
                    )}

                    {fields.map(field => (
                      <AntFormItem
                        label={field.label}
                        colon={false}
                        key={field.key}
                        required={field.hasOwnProperty("required") ? !!field.required : true}
                        validateStatus={this.validateField(
                          field.key,
                          field.value,
                          field.hasOwnProperty("required") ? field.required : true
                        )}
                        hasFeedback={true}
                        help={
                          this.validateField(
                            field.key,
                            field.value,
                            field.hasOwnProperty("required") ? field.required : true
                          ) === ERROR && this.helpText(field)
                        }>
                        <AntInput
                          value={field.value}
                          placeholder={this.placeHolder(field.type || field.key)}
                          type={this.getFieldType(field)}
                          onChange={e => {
                            this.updateField(
                              field.key,
                              this.getFieldType(field) === "number" ? e : e.currentTarget.value
                            );
                          }}
                          suffix={
                            field?.helpText && (
                              <Tooltip title={field.helpText}>
                                <Icon type="info-circle" theme="outlined" />
                              </Tooltip>
                            )
                          }
                          onBlur={e => {
                            this.setState({
                              [field.key]: true
                            });
                          }}
                        />
                      </AntFormItem>
                    ))}

                    {arrayFields.length > 0 && (
                      <ArrayInput fields={arrayFields} updateField={(key, value) => this.updateField(key, value)} />
                    )}

                    {metadataFields.length > 0 && (
                      <GenericFormComponent elements={metadataFields} onChange={this.updateMetadata} />
                    )}
                    {this.props.integration_type === IntegrationTypes.PERFORCE_HELIX_SERVER &&
                      this.props.integration_form?.metadata &&
                      this.props.integration_form.metadata?.ssl_enabled && (
                        <AntFormItem
                          className="ssl-fingerprint"
                          label={"SSL Fingerprint"}
                          colon={false}
                          key={"ssl_fingerprint"}
                          required={false}
                          validateStatus={true}
                          hasFeedback={true}
                          help={true}>
                          <AntInput
                            value={this.props.integration_form?.metadata?.ssl_fingerprint}
                            placeholder={"Expected fingerprint of the server’s public key"}
                            type={"text"}
                            onChange={e => this.updateSSLFingerPrint("ssl_fingerprint", e.currentTarget.value)}
                            suffix={
                              <Tooltip
                                title={
                                  "Public key fingerprint verification is a security measure used to reduce the chance of a Man-In-The-Middle attack. Expected fingerprint can be obtained by running “p4d -Gf” on the Helix Core server."
                                }>
                                <Icon type="info-circle" theme="outlined" />
                              </Tooltip>
                            }
                          />
                        </AntFormItem>
                      )}
                  </AntForm>
                </Col>
              </Row>
            </div>
          )}
          {configDocUrl && (
            <div>
              <a className="link-Container" target="_blank" href={configDocUrl} rel="noopener noreferrer">
                Configuration Guide
              </a>
            </div>
          )}
        </div>
      </AntCard>
    );
  }
}

export default connect(mapIntegrationStatetoProps, mapIntegrationDispatchtoProps)(IntegrationCredentialsContainer);
