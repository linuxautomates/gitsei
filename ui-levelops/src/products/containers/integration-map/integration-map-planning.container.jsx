import React from "react";
import * as PropTypes from "prop-types";
import { mapFormDispatchToPros, mapJiraMappingFormStatetoProps } from "reduxConfigs/maps/formMap";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { connect } from "react-redux";
import { SelectWrapper } from "shared-resources/helpers";
import { AntButton, AntButtonGroup, Input, IntegrationIcon, AntTabs } from "shared-resources/components";
import { Row } from "antd";
import SearchableSelect from "components/SearchableSelect/SearchableSelect";
import { RestJiraMapping } from "classes/RestProduct";
import { getError, getLoading } from "utils/loadingUtils";
import Loader from "components/Loader/Loader";
import { RegexModal } from "products/components";

export class IntegrationMapPlanningContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      integration_select: undefined,
      integration_loading: false,
      loading: true,
      create_loading: false,
      integration_type: "jira",
      show_regex_modal: false
    };
    this.onTabChange = this.onTabChange.bind(this);
    this.onFieldChangeHandler = this.onFieldChangeHandler.bind(this);
    this.handleCancel = this.handleCancel.bind(this);
    this.handleAdd = this.handleAdd.bind(this);
    let filter = {
      filter: {
        stage_ids: [this.props.stage.id]
      }
    };
    this.props.mappingsList(filter);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.loading) {
      if (!getLoading(props.rest_api, "mappings", "list", "0") && !getError(props.rest_api, "mappings", "list", "0")) {
        // artificially limit to one mapping per stage
        let integrationLoading = false;
        let records = props.rest_api.mappings.list["0"].data.records.filter(
          record => record.integration_type === state.integration_type
        );
        if (records.length > 0) {
          let jiraMapping = new RestJiraMapping(records[0]);
          props.formUpdateObj("jira_mapping_form", jiraMapping);
          props.integrationsGet(records[0].integration_id);
          props.restapiClear("mappings");
          integrationLoading = true;
        } else {
          //let jiraMapping = props.jira_mapping_form;
          let jiraMapping = new RestJiraMapping();
          jiraMapping.stage_id = props.stage.id;
          props.formUpdateObj("jira_mapping_form", jiraMapping);
        }

        return {
          ...state,
          loading: false,
          integration_loading: integrationLoading,
          integration_select: {}
        };
      }
    }
    if (state.integration_loading) {
      if (
        !getLoading(props.rest_api, "integrations", "get", props.jira_mapping_form.integration_id) &&
        !getError(props.rest_api, "integrations", "get", props.jira_mapping_form.integration_id)
      ) {
        return {
          ...state,
          integration_loading: false,
          integration_select: {
            label: props.rest_api.integrations.get[props.jira_mapping_form.integration_id].data.name,
            value: props.jira_mapping_form.integration_id
          }
        };
      }
    }
    if (state.create_loading) {
      // fire off api to create the mapping
      let method = props.jira_mapping_form.id ? "update" : "create";
      let id = props.jira_mapping_form.id ? props.jira_mapping_form.id : "0";
      if (!getLoading(props.rest_api, "mappings", method, id) && !getError(props.rest_api, "mappings", method, id)) {
        props.formClear("jira_mapping_form");
        props.onDone();
        return {
          ...state,
          create_loading: false
        };
      }
    }
  }

  onFieldChangeHandler(field) {
    return value => {
      let jiraMapping = Object.assign(
        Object.create(Object.getPrototypeOf(this.props.jira_mapping_form)),
        this.props.jira_mapping_form
      );
      jiraMapping[field].regex = value;
      this.props.formUpdateObj("jira_mapping_form", jiraMapping);
    };
  }

  handleCancel(e) {
    this.props.restapiClear("mappings");
    this.props.onDone();
  }

  handleAdd(e) {
    this.props.restapiClear("mappings");
    this.setState({ create_loading: true }, () => {
      if (this.props.jira_mapping_form.id === undefined) {
        this.props.mappingsCreate(this.props.jira_mapping_form);
      } else {
        this.props.mappingsUpdate(this.props.jira_mapping_form.id, this.props.jira_mapping_form);
      }
    });
  }

  onTabChange(key) {
    this.setState(
      {
        integration_type: key,
        loading: true
      },
      () =>
        this.props.mappingsList({
          filter: {
            stage_ids: [this.props.stage.id],
            integration_type: key
          }
        })
    );
  }

  render() {
    const mappedTabs = this.props.stage.integration_types.map(integration_type => ({
      id: integration_type,
      label: integration_type,
      tab: (
        <div className={`flex direction-row justify-start`}>
          <IntegrationIcon type={integration_type} />
          {integration_type.toUpperCase()}
        </div>
      )
    }));
    if (this.state.loading) {
      return <Loader />;
    }
    return (
      <div className={`flex direction-column justify-start`} style={{ width: "100%" }}>
        {this.state.show_regex_modal && (
          <RegexModal
            onCloseEvent={() => {
              this.setState({ show_regex_modal: false });
            }}
          />
        )}
        <div style={{ marginBottom: "10px" }}>
          <a
            href={"#"}
            onClick={e => {
              e.preventDefault();
              this.setState({ show_regex_modal: true });
            }}>
            Regex Guide
          </a>
        </div>
        <div style={{ paddingBottom: "20px" }}>
          <AntTabs size="small" animated={false} onChange={this.onTabChange} tabpanes={mappedTabs} />
        </div>
        <div style={{ width: "40%" }}>
          <SelectWrapper label={"Integration"}>
            <SearchableSelect
              method={`list`}
              uri={`integrations`}
              searchField="name"
              rest_api={this.props.rest_api}
              fetchData={this.props.integrationsList}
              moreFilters={{ applications: [this.state.integration_type] }}
              additionalField={"application"}
              id={"select-type"}
              value={this.state.integration_select}
              isMulti={false}
              isClearable={false}
              closeMenuOnSelect={true}
              creatable={false}
              placeholder={"Select Integration for Planning"}
              isLoading={this.state.integration_loading}
              onChange={option => {
                this.setState({ integration_select: option || {} }, () => {
                  let jiraMapping = this.props.jira_mapping_form;
                  jiraMapping.mappings = undefined;
                  jiraMapping.integration_id = option ? option.value : undefined;
                  console.log(jiraMapping);
                  this.props.formUpdateObj("jira_mapping_form", jiraMapping);
                });
              }}
            />
          </SelectWrapper>
        </div>
        {Object.keys(this.props.jira_mapping_form.mappings).map(mapping => (
          <div className={`flex direction-row justify-space-between align-center`} style={{ marginTop: "20px" }}>
            <div style={{ flexBasis: "40%" }}>
              <Input
                key={`${mapping}-regex`}
                value={this.props.jira_mapping_form.mappings[mapping].regex}
                label={`${mapping.toUpperCase()} Regex`}
                name={mapping}
                //hasError={field.hasError}
                //type={field.type || "text"}
                isRequired={true}
                onChangeEvent={this.onFieldChangeHandler(mapping)}
              />
            </div>
            <div style={{ flexBasis: "40%" }}>
              <SelectWrapper label={`${mapping.toUpperCase()} Mapping *`}>
                <SearchableSelect
                  method={`list`}
                  uri={`fields`}
                  searchField="name"
                  rest_api={this.props.rest_api}
                  fetchData={this.props.fieldsList}
                  moreFilters={{
                    integration_id: this.props.jira_mapping_form.integration_id
                  }}
                  id={"select-type"}
                  value={{
                    label: this.props.jira_mapping_form.mappings[mapping].field_name,
                    value: this.props.jira_mapping_form.mappings[mapping].field_id
                  }}
                  isDisabled={this.props.jira_mapping_form.integration_id === undefined}
                  isMulti={false}
                  closeMenuOnSelect={true}
                  creatable={false}
                  isClearable={false}
                  placeholder={"Select Field for Mapping"}
                  onChange={option => {
                    if (!option) {
                      option = {};
                    }
                    let jiraMappings = this.props.jira_mapping_form;
                    jiraMappings.mappings[mapping].field_name = option.label;
                    jiraMappings.mappings[mapping].field_id = option.value;
                    this.props.formUpdateObj("jira_mapping_form", jiraMappings);
                  }}
                />
              </SelectWrapper>
            </div>
          </div>
        ))}
        <Row type={"flex"} justify={"end"}>
          <AntButtonGroup>
            <AntButton onClick={this.handleCancel}>Cancel</AntButton>
            <AntButton type={"primary"} onClick={this.handleAdd} disabled={!this.props.jira_mapping_form.validate()}>
              {this.props.jira_mapping_form.id ? "Update" : "Add"}
            </AntButton>
          </AntButtonGroup>
        </Row>
      </div>
    );
  }
}

IntegrationMapPlanningContainer.propTypes = {
  className: PropTypes.string,
  stage: PropTypes.object.isRequired,
  jira_mapping_form: PropTypes.object.isRequired,
  rest_api: PropTypes.object.isRequired,
  onDone: PropTypes.func.isRequired
};

IntegrationMapPlanningContainer.defaultProps = {
  className: "integration-planning"
};

const mapStatetoProps = state => {
  return {
    ...mapJiraMappingFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(IntegrationMapPlanningContainer);
