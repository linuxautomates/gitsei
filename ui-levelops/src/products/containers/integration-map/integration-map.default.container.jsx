import React from "react";
import * as PropTypes from "prop-types";
import { mapMappingFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { connect } from "react-redux";
import { AntButton, AntButtonGroup } from "shared-resources/components";
import { Row } from "antd";
import { SelectWrapper } from "shared-resources/helpers";
import SearchableSelect from "components/SearchableSelect/SearchableSelect";
import { getError, getLoading } from "utils/loadingUtils";
import { RestMapping } from "../../../classes/RestProduct";

export class IntegrationMapDefaultContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      integration_loading: false,
      create_loading: false,
      integration_select: [],
      new_mappings: [],
      deleted_mappings: []
    };
    this.handleAdd = this.handleAdd.bind(this);
    this.handleCancel = this.handleCancel.bind(this);
    let filter = {
      filter: {
        stage_ids: [this.props.stage.id]
      }
    };
    this.props.mappingsList(filter);
  }

  componentDidMount() {
    let mappingForm = this.props.mapping_form;
    mappingForm.stage_id = this.props.stage.id;
    mappingForm.integration_type = this.props.stage.integration_types[0];
    this.props.formUpdateObj("mapping_form", mappingForm);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.loading) {
      if (!getLoading(props.rest_api, "mappings", "list", "0") && !getError(props.rest_api, "mappings", "list", "0")) {
        props.rest_api.mappings.list["0"].data.records.forEach(mapping => {
          props.integrationsGet(mapping.integration_id);
        });
        return {
          ...state,
          loading: false,
          integration_loading: props.rest_api.mappings.list["0"].data.records.length > 0
        };
      }
    }
    if (state.integration_loading) {
      console.log("in integration loading");
      let integrationLoading = false;
      let integrationSelect = [];
      props.rest_api.mappings.list["0"].data.records.forEach(mapping => {
        if (
          getLoading(props.rest_api, "integrations", "get", mapping.integration_id) ||
          getError(props.rest_api, "integrations", "get", mapping.integration_id)
        ) {
          integrationLoading = true;
        } else {
          console.log("pushing to integration select");
          integrationSelect.push({
            label: props.rest_api.integrations.get[mapping.integration_id].data.name,
            value: props.rest_api.integrations.get[mapping.integration_id].data.id,
            more: props.rest_api.integrations.get[mapping.integration_id].data.application
          });
        }
      });

      return {
        ...state,
        integration_loading: integrationLoading,
        integration_select: integrationLoading ? [] : integrationSelect
      };
    }

    if (state.create_loading) {
      // do some restapi checks here for mapping
      props.onDone();
      return {
        ...state,
        create_loading: false
      };
    }
  }

  handleAdd(e) {
    const existingMappings = this.props.rest_api.mappings.list["0"].data.records.map(record => record.integration_id);
    const currentMappings = this.state.integration_select.map(integration => integration.value);
    const newMappings = currentMappings.filter(integration => !existingMappings.includes(integration));
    const deletedMappings = existingMappings.filter(integration_id => !currentMappings.includes(integration_id));
    this.setState({ create_loading: true }, () => {
      newMappings.forEach(integration => {
        let map = new RestMapping();
        map.integration_id = integration;
        map.integration_type = this.state.integration_select.filter(integ => integ.value === integration)[0].more;
        map.stage_id = this.props.stage.id;
        this.props.mappingsCreate(map);
      });
      deletedMappings.forEach(integration => {
        const mapping = this.props.rest_api.mappings.list["0"].data.records.filter(
          record => record.integration_id === integration
        );
        this.props.mappingsDelete(mapping[0].id);
      });
    });
  }

  handleCancel(e) {
    this.props.onDone();
  }

  render() {
    return (
      <div className={`flex direction-column justify-start`} style={{ width: "100%" }}>
        <div style={{ width: "40%", marginTop: "10px" }}>
          <SelectWrapper label={"Integration"}>
            <SearchableSelect
              method={`list`}
              uri={`integrations`}
              searchField="name"
              additionalField={"application"}
              rest_api={this.props.rest_api}
              fetchData={this.props.integrationsList}
              //moreFilters={{applications:this.props.stage.integration_types}}
              id={"select-type"}
              value={this.state.integration_select}
              isMulti={true}
              closeMenuOnSelect={true}
              creatable={false}
              placeholder={"Select Integrations"}
              onChange={options => {
                console.log(options);
                this.setState({ integration_select: options });
              }}
            />
          </SelectWrapper>
        </div>
        {/*<div className={`flex direction-row justify-end`} style={{marginTop: "20px"}}>*/}
        <Row type={"flex"} justify={"end"}>
          <AntButtonGroup>
            <AntButton onClick={this.handleCancel}>Cancel</AntButton>
            <AntButton type={"primary"} onClick={this.handleAdd} disabled={this.state.integration_select.length === 0}>
              Add
            </AntButton>
          </AntButtonGroup>
        </Row>
        {/*</div>*/}
      </div>
    );
  }
}

IntegrationMapDefaultContainer.propTypes = {
  className: PropTypes.string,
  stage: PropTypes.object.isRequired,
  mapping_form: PropTypes.object.isRequired,
  rest_api: PropTypes.object.isRequired
};

IntegrationMapDefaultContainer.defaultProps = {
  className: "integration-default"
};

const mapStatetoProps = state => {
  return {
    ...mapMappingFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(IntegrationMapDefaultContainer);
