import React from "react";
import { connect } from "react-redux";
import { Col, Row } from "antd";
import { AntForm, AntFormItem, AntInput, AntSelect, AntText } from "shared-resources/components";
import { mapIntegrationDispatchtoProps, mapIntegrationStatetoProps } from "reduxConfigs/maps/integrationMap";
import { EMPTY_FIELD_WARNING } from "constants/formWarnings";
import { SelectRestapi } from "shared-resources/helpers";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { loadingStatus } from "../../../utils/loadingUtils";
import { timezones } from "../../../utils/timezones.utils";
import { debounce } from "lodash/function";
import uuid1 from "uuid/v1";
import { get } from "lodash";
import { checkTemplateNameExists } from "../../helpers/checkTemplateNameExits";
import { getIntegrationUrlMap } from "constants/integrations";
import ToggleComponent from "configurations/components/ToggleComponent/ToggleComponent";
import { AZURE_TOGGLE_HEADING } from "./constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const NAME_EXIST_ERROR = "This integration name already exist";
export const TIMEZONEAPPLICATION = ["JIRA", "circleci"];
export class IntegrationDetailsContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      name_exist: false,
      checking_name: false,
      nameCheckId: undefined
    };

    this.searchName = this.searchName.bind(this);
    this.validateStatus = this.validateStatus.bind(this);
    this.getError = this.getError.bind(this);
    this.debouncedSearch = debounce(this.searchName, 500);
    this.integrationMap = getIntegrationUrlMap();
  }

  componentDidMount() {
    const integration = this.integrationMap[this.props.integration_type];
    const integrationForm = this.props.integration_form;
    if (integration) {
      let metadata = {};
      (integration?.detailsPage_metadata_fields || []).forEach(config => {
        metadata = {
          ...metadata,
          [config.be_key]: config?.defaultValue
        };
      });
      if (
        integration.hasOwnProperty("detailsPage_metadata_fields") &&
        this.props.integration_type === IntegrationTypes.AZURE
      ) {
        this.props.integrationForm({ metadata: metadata });
      }
      if (integrationForm.hasOwnProperty("tags")) {
        this.props.integrationForm({ tags: [] });
      }
    }
  }

  searchName(value) {
    const filter = {
      partial: {
        name: value
      }
    };
    const id = uuid1();
    this.props.integrationsList({ filter }, null, id);
    this.setState({
      checking_name: true,
      nameCheckId: id
    });
    this.props.integrationForm({ validating_name: true });
  }

  updateField(key, value) {
    this.props.integrationForm({ [key]: value });
  }

  static getDerivedStateFromProps(props, state) {
    if (state.checking_name) {
      const { loading, error } = loadingStatus(props.rest_api, "integrations", "list", state.nameCheckId);
      if (!loading && !error) {
        const data = get(props.rest_api, ["integrations", "list", state.nameCheckId, "data", "records"], []);
        const nameExist = checkTemplateNameExists(props.integration_form.name, data);
        props.integrationForm({ name_exist: nameExist, validating_name: false });
        return {
          ...state,
          name_exist: nameExist,
          checking_name: false
        };
      }
    }
  }

  validateStatus(field) {
    if (field.required) {
      if (this.state[field.id] && field.value === "") {
        return "error";
      }
      if (field.id === "name" && this.state.name_exist) {
        return "error";
      }
      return true;
    }

    return "success";
  }

  getError(field) {
    if (field.required && this.state[field.id] && field.value === "") {
      return EMPTY_FIELD_WARNING;
    }

    if (field.id === "name" && field.value && this.state.name_exist) {
      return NAME_EXIST_ERROR;
    }
  }

  getMappedOrganisationValue = () => {
    const metaData = this.props.integration_form?.metadata || {};
    const { organization, organizations } = metaData;
    return organizations || organization || "";
  };

  updateOrganisations = e => {
    const metadata = { ...this.props?.integration_form?.["metadata"] };
    this.updateField("metadata", { ...metadata, organizations: e?.currentTarget?.value });
  };
  updateToggleData = value => {
    const metadata = { ...this.props?.integration_form?.["metadata"] };
    this.updateField("metadata", { ...metadata, ...value });
  };

  toggleDisabled = (toggleFilters, be_key) => {
    const toggleKeys = (toggleFilters || []).map(item => item.be_key);
    const metadata = this.props.integration_form?.["metadata"];
    const activeKeys = (toggleKeys || []).filter(key => metadata?.[key]?.enabled);
    if (activeKeys?.length === 1 && be_key === activeKeys?.[0]) {
      return true;
    }
    return false;
  };

  getToggleCheckValue = be_key => {
    const metadata = this.props.integration_form?.["metadata"];
    if (metadata?.hasOwnProperty(be_key)) {
      return this.props.integration_form?.metadata?.[be_key]?.enabled;
    }
    return true;
  };

  render() {
    const fields = [
      {
        id: "name",
        label: "name",
        value: this.props.integration_form.name || "",
        required: true
      },
      {
        id: "description",
        label: "description",
        value: this.props.integration_form.description || "",
        required: false
      }
    ];

    const azureField = {
      id: "organizations",
      label: "Collections",
      value: this.getMappedOrganisationValue(),
      required: false
    };
    const metadataFields = get(this.integrationMap, [this.props.integration_type, "detailsPage_metadata_fields"], []);
    const toggleFilters = metadataFields.filter(item => item.type === "toggle");
    return (
      <Row type={"flex"} justify={"center"}>
        <Col span={22}>
          <AntForm layout={"vertical"}>
            {fields.map(field => (
              <AntFormItem
                label={field.label}
                colon={false}
                key={field.id}
                required={field.required}
                validateStatus={this.validateStatus(field)}
                hasFeedback={field.required}
                help={this.validateStatus(field) === "error" && this.getError(field)}>
                <AntInput
                  value={field.value}
                  placeholder={field.label === "name" ? "Integration Name" : "Integration Description"}
                  onChange={e => {
                    this.updateField(field.id, e.currentTarget.value);
                    this.debouncedSearch(e.currentTarget.value);
                  }}
                  onBlur={e => {
                    this.setState({
                      [field.id]: true
                    });
                  }}
                />
              </AntFormItem>
            ))}
            <AntFormItem label={"Tags"} colon={false} key={"tags"}>
              <SelectRestapi
                value={this.props.integration_form.tags || []}
                mode={"multiple"}
                labelInValue={true}
                uri={"tags"}
                rest_api={this.props.rest_api}
                fetchData={this.props.tagsList}
                createOption={true}
                searchField="name"
                onChange={value => {
                  value = value || [];
                  this.updateField("tags", value);
                }}
              />
            </AntFormItem>
            {TIMEZONEAPPLICATION.includes(this.props.integration_type) && (
              <AntFormItem label={"Timezone"} colon={false} key={"timezone"}>
                <AntSelect
                  defaultValue={this.props.integration_form?.metadata?.timezone || null}
                  options={timezones.map(timezone => ({ label: timezone.label, value: timezone.value }))}
                  onSelect={(value, option) => {
                    const metadata = { ...this.props?.integration_form?.["metadata"] };
                    this.updateField("metadata", { ...metadata, timezone: value });
                  }}
                />
              </AntFormItem>
            )}
            {this.props.integration_type === IntegrationTypes.AZURE && (
              <AntFormItem
                label={azureField.label}
                colon={false}
                key={azureField.id}
                required={azureField.required}
                validateStatus={this.validateStatus(azureField)}
                hasFeedback={azureField.required}
                help={this.validateStatus(azureField) === "error" && this.getError(azureField)}>
                <AntInput
                  value={azureField.value}
                  placeholder={"Collections"}
                  onChange={e => this.updateOrganisations(e)}
                  onBlur={e => {
                    this.setState({
                      [azureField.id]: true
                    });
                  }}
                />
                <span>Limit ingestion to selected Collections. If unspecified all Collections are ingested.</span>
              </AntFormItem>
            )}
            {this.props.integration_type === IntegrationTypes.AZURE && (
              <div>
                <AntText>{AZURE_TOGGLE_HEADING}</AntText>
                {toggleFilters.length > 0 &&
                  toggleFilters.map(item => (
                    <ToggleComponent
                      disabled={this.toggleDisabled(toggleFilters, item.be_key)}
                      toggleConfig={item}
                      onchange={this.updateToggleData}
                      checked={this.getToggleCheckValue(item.be_key)}
                    />
                  ))}
              </div>
            )}
          </AntForm>
        </Col>
      </Row>
    );
  }
}

const mapStatetoProps = state => ({
  ...mapIntegrationStatetoProps(state),
  ...mapRestapiStatetoProps(state)
});

const mapDispatchtoProps = dispatch => {
  return {
    ...mapIntegrationDispatchtoProps(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(IntegrationDetailsContainer);
