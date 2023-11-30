import React from "react";
import { Button, Col, Form, Row, Select } from "antd";
import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { get, isEqual } from "lodash";
import { customFieldsSelector } from "reduxConfigs/selectors/restapiSelector";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import uuidv1 from "uuid/v1";

const { Option } = Select;

export class CustomFieldsComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      integration_loading: false,
      custom_fields_loading: false,
      value: {},
      additional_option: "",
      integration_select: {},
      select_string: ""
    };
    this.handleAdditionalOption = this.handleAdditionalOption.bind(this);
    this.addCustomField = this.addCustomField.bind(this);
  }

  static get defaultState() {
    return {
      integration_loading: false,
      custom_fields_loading: false,
      value: {},
      additional_option: "",
      select_string: ""
    };
  }

  componentWillUnmount() {
    console.log("this component is unmounting");
  }

  static getDerivedStateFromProps(props, state) {
    if (!isEqual(props.value, state.value)) {
      // new set of data, act accordingly
      const propIntegration = props.value ? props.value.integration_id : undefined;
      const stateIntegration = state.value ? state.value.integration_id : undefined;

      if (propIntegration) {
        if (propIntegration !== stateIntegration) {
          props.genericGet("integrations", propIntegration);
        }
      }
      return {
        ...CustomFieldsComponent.defaultState,
        value: { ...props.value },
        integration_loading: propIntegration && propIntegration !== stateIntegration
      };
    }

    if (state.integration_loading) {
      const loading = get(props, ["rest_api", "integration", "loading"], true);
      const error = get(props, ["rest_api", "integration", "error"], false);
      if (!loading && !error) {
        const integration = get(props, ["rest_api", "integration", "data"], {});
        if (integration && integration.id !== undefined) {
          const filters = {
            fields: ["status", "priority", "issue_type", "assignee", "project", "component", "label"],
            integration_ids: [integration.id],
            filter: {
              integration_ids: [integration.id]
            }
          };
          props.widgetFilterValuesGet("jira_filter_values", filters);
          return {
            ...state,
            integration_loading: false,
            custom_fields_loading: true
          };
        }
      }
    }

    if (state.custom_fields_loading) {
      const loading = get(props, ["rest_api", "jira_filter_values", "loading"], true);
      const error = get(props, ["rest_api", "jira_filter_values", "error"], false);
      if (!loading && !error) {
        return {
          ...state,
          custom_fields_loading: false
        };
      }
    }

    return null;
  }

  get integrationSelect() {
    const integration = get(this.props, ["rest_api", "integration", "data"], {});
    return { label: integration.name, key: integration.id };
  }

  get customFieldOptions() {
    const fieldOptions = get(this.props, ["rest_api", "jira_filter_values", "data", "custom_fields"], []).map(
      field => ({
        key: field.key,
        label: field.name
      })
    );
    return fieldOptions;
  }

  customFieldOptionValues(index) {
    const key = get(this.props, ["value", "custom_fields", index, "key"], undefined);
    if (!key) {
      return [];
    }
    const filterValues = get(this.props, ["rest_api", "jira_filter_values", "data", "records"], []);
    const filterRecord = filterValues.find(val => Object.keys(val)[0] === key);
    if (filterRecord) {
      const optionValues = (filterRecord[key] || []).map(val => val.key);
      return optionValues;
    }
    return [];
  }

  get customFieldValues() {
    const fieldOptions = this.customFieldOptions;
    return get(this.props, ["value", "custom_fields"], []).map(field => ({
      ...field,
      name: (fieldOptions.find(option => option.key === field.key) || {}).name
    }));
  }

  handleAdditionalOption(value) {
    console.log(value);
    this.setState({ additional_option: value });
  }

  addCustomField() {
    let propsVal = { ...this.props.value };
    if (propsVal.custom_fields) {
      propsVal.custom_fields.push({ key: undefined, value: undefined });
      this.setState(
        {
          value: propsVal
        },
        () => this.props.onChange(propsVal)
      );
    } else {
      propsVal.custom_fields = [{ key: undefined, value: undefined }];
      this.setState(
        {
          value: propsVal
        },
        () => this.props.onChange(propsVal)
      );
    }
  }

  removeCustomField(index) {
    let propsVal = { ...this.props.value };
    propsVal.custom_fields.splice(index, 1);
    this.setState(
      {
        value: propsVal
      },
      () => this.props.onChange(propsVal)
    );
  }

  render() {
    const { layout, onChange } = this.props;
    return (
      <Form layout={layout}>
        <Form.Item label={"Integration"} required={true}>
          <SelectRestapi
            uri={"integrations"}
            method={"list"}
            mode={"single"}
            labelInValue={true}
            loading={this.state.integration_loading}
            value={this.integrationSelect}
            allowClear={false}
            onChange={value => {
              let propsVal = { ...this.props.value };
              // on template change, selected question is invalid
              propsVal.integration_id = value.key;
              this.setState(
                {
                  value: propsVal
                },
                () => onChange(propsVal)
              );
            }}
          />
        </Form.Item>
        <Form.Item label={"Custom Fields"}>
          {this.customFieldValues.map((field, index) => (
            <Row gutter={[10, 10]} key={uuidv1()}>
              <Col span={12}>
                <Select
                  loading={this.state.custom_fields_loading}
                  disabled={this.integrationSelect.key === undefined}
                  mode={"default"}
                  labelInValue={true}
                  showSearch={true}
                  allowClear={false}
                  value={{ key: field.key, label: field.name }}
                  onChange={value => {
                    let propsVal = { ...this.props.value };
                    // on question change, the answers selected have to be reset
                    propsVal.custom_fields[index] = { key: value.key, value: undefined };
                    this.setState(
                      {
                        value: propsVal
                      },
                      () => onChange(propsVal)
                    );
                  }}>
                  {this.customFieldOptions.map(question => (
                    <Option key={question.key}>{question.label}</Option>
                  ))}
                </Select>
              </Col>
              <Col span={11}>
                <Select
                  loading={this.state.custom_fields_loading}
                  disabled={
                    this.integrationSelect.key === undefined ||
                    get(this.props, ["value", "custom_fields", index, "key"], undefined) === undefined
                  }
                  mode={"tags"}
                  labelInValue={true}
                  showSearch={true}
                  allowClear={false}
                  value={field.value ? [{ key: field.value, label: field.value }] : []}
                  //onSearch={value => this.setState({search_string: value})}
                  onDeselect={value => {
                    let propsVal = { ...this.props.value };
                    propsVal.custom_fields[index] = { ...propsVal.custom_fields[index], value: undefined };
                    this.setState(
                      {
                        value: propsVal
                      },
                      () => onChange(propsVal)
                    );
                  }}
                  onSelect={value => {
                    let propsVal = { ...this.props.value };
                    // on question change, the answers selected have to be reset
                    propsVal.custom_fields[index] = { ...propsVal.custom_fields[index], value: value.key };
                    this.setState(
                      {
                        value: propsVal
                      },
                      () => onChange(propsVal)
                    );
                  }}>
                  {(this.customFieldOptionValues(index) || []).map(cf => (
                    <Option key={cf}>{cf}</Option>
                  ))}
                </Select>
              </Col>
              <Col span={1}>
                <Button type={"link"} icon={"delete"} onClick={e => this.removeCustomField(index)} />
              </Col>
            </Row>
          ))}
          {this.integrationSelect.key !== undefined && (
            <Row>
              <Col span={24}>
                <Button icon={"plus"} onClick={this.addCustomField} type={"link"}>
                  Add Custom Field
                </Button>
              </Col>
            </Row>
          )}
        </Form.Item>
      </Form>
    );
  }
}

CustomFieldsComponent.propTypes = {
  onChange: PropTypes.func,
  layout: PropTypes.string.isRequired,
  value: PropTypes.object.isRequired
};

CustomFieldsComponent.defaultProps = {
  layout: "vertical",
  value: {
    integration_id: undefined,
    custom_fields: []
  }
};

const mapStateToProps = (state, ownProps) => ({
  rest_api: customFieldsSelector(state, ownProps)
});

const mapDispatchtoProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapGenericToProps(dispatch)
  };
};

export default connect(mapStateToProps, mapDispatchtoProps)(CustomFieldsComponent);
