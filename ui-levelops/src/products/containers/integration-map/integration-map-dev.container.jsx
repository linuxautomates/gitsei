import React from "react";
import * as PropTypes from "prop-types";
import { mapGithubMappingFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { connect } from "react-redux";
import { AntButton, AntButtonGroup, AntTabs, IntegrationIcon } from "shared-resources/components";
import { Row } from "antd";
import { SelectWrapper } from "shared-resources/helpers";
import SearchableSelect from "components/SearchableSelect/SearchableSelect";
import Select from "react-select";
import { getError, getLoading } from "utils/loadingUtils";
import { RestGithubMapping } from "classes/RestProduct";
import Loader from "components/Loader/Loader";

export class IntegrationMapDevContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      integration_select: undefined,
      repos_select: [],
      integration_loading: false,
      repos_loading: false,
      repo_details_loading: false,
      loading: true,
      repos: [],
      integration_type: "github"
    };
    this.handleAdd = this.handleAdd.bind(this);
    this.handleCancel = this.handleCancel.bind(this);
    let filter = {
      filter: {
        stage_ids: [this.props.stage.id]
      }
    };
    this.props.mappingsList(filter);
    this.onTabChange = this.onTabChange.bind(this);
  }

  componentWillUnmount() {
    this.props.restapiClear("integrations", "list", "0");
    this.props.restapiClear("repositories", "list", "0");
    this.props.restapiClear("repositories", "get", "-1");
  }

  static getDerivedStateFromProps(props, state) {
    if (state.loading) {
      if (!getLoading(props.rest_api, "mappings", "list", "0") && !getError(props.rest_api, "mappings", "list", "0")) {
        // artificially limit to one mapping per stage
        let integrationLoading = false;
        let repoDetailsLoading = false;
        let records = props.rest_api.mappings.list["0"].data.records.filter(
          record => record.integration_type === state.integration_type
        );
        if (records.length > 0) {
          let githubMapping = new RestGithubMapping(records[0]);
          props.formUpdateObj("github_mapping_form", githubMapping);
          props.integrationsGet(records[0].integration_id);
          if (githubMapping.repos !== undefined) {
            githubMapping.repos.forEach(repo => props.repositoriesGet(repo.repo_id));
            repoDetailsLoading = true;
          }
          integrationLoading = true;
          props.restapiClear("mappings");
        } else {
          // let mappingForm = props.github_mapping_form;
          let mappingForm = new RestGithubMapping();
          mappingForm.stage_id = props.stage.id;
          mappingForm.integration_type = state.integration_type;
          props.formUpdateObj("github_mapping_form", mappingForm);
        }

        return {
          ...state,
          loading: false,
          integration_loading: integrationLoading,
          repo_details_loading: repoDetailsLoading,
          integration_select: {},
          repos_select: []
        };
      }
    }
    if (state.integration_loading) {
      if (
        !getLoading(props.rest_api, "integrations", "get", props.github_mapping_form.integration_id) &&
        !getError(props.rest_api, "integrations", "get", props.github_mapping_form.integration_id)
      ) {
        let filter = {
          filter: {
            integration_id: props.github_mapping_form.integration_id
          }
        };
        props.repositoriesList(filter);
        return {
          ...state,
          integration_loading: false,
          repos_loading: true,
          integration_select: {
            label: props.rest_api.integrations.get[props.github_mapping_form.integration_id].data.name,
            value: props.github_mapping_form.integration_id,
            more: props.rest_api.integrations.get[props.github_mapping_form.integration_id].data.application
          }
        };
      }
    }
    if (state.repos_loading) {
      if (
        !getLoading(props.rest_api, "repositories", "list", "0") &&
        !getError(props.rest_api, "repositories", "list", "0")
      ) {
        return {
          ...state,
          repos_loading: false,
          repos: props.rest_api.repositories.list["0"].data.records.map(repo => ({
            label: `${repo.name} Master Branch:${repo.master_branch}`,
            name: repo.name,
            value: repo.id,
            branch: repo.master_branch
          }))
        };
      }
    }

    if (state.repo_details_loading) {
      let repoDetailsLoading = false;
      let reposSelect = [];
      props.github_mapping_form.repos.forEach(repo => {
        if (
          getLoading(props.rest_api, "repositories", "get", repo.repo_id.toString()) ||
          getError(props.rest_api, "repositories", "get", repo.repo_id.toString())
        ) {
          repoDetailsLoading = true;
        } else {
          const branch = props.rest_api.repositories.get[repo.repo_id].data.master_branch;
          reposSelect.push({
            label: `${repo.repo_name} Master Branch: ${branch}`,
            name: repo.repo_name,
            value: repo.repo_id,
            branch: branch
          });
        }
      });

      return {
        ...state,
        repo_details_loading: repoDetailsLoading,
        repos_select: repoDetailsLoading ? [] : reposSelect
      };
    }

    if (state.create_loading) {
      let method = props.github_mapping_form.id ? "update" : "create";
      let id = props.github_mapping_form.id ? props.github_mapping_form.id : "0";
      if (!getLoading(props.rest_api, "mappings", method, id) && !getError(props.rest_api, "mappings", method, id)) {
        props.formClear("github_mapping_form");
        props.onDone();
        return {
          ...state,
          create_loading: false
        };
      }
    }
  }

  handleCancel(e) {
    this.props.restapiClear("mappings");
    this.props.onDone();
  }

  handleAdd(e) {
    this.props.restapiClear("mappings");
    this.setState({ create_loading: true }, () => {
      if (this.props.github_mapping_form.id === undefined) {
        this.props.mappingsCreate(this.props.github_mapping_form);
      } else {
        this.props.mappingsUpdate(this.props.github_mapping_form.id, this.props.github_mapping_form);
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
          &nbsp;
          {integration_type.toUpperCase()}
        </div>
      )
    }));

    if (this.state.loading || this.state.integrations_loading || this.state.repo_details_loading) {
      return <Loader />;
    }
    return (
      <div className={`flex direction-column justify-start`} style={{ width: "100%" }}>
        <div style={{ paddingBottom: "20px" }}>
          <AntTabs size="small" animated={false} onChange={this.onTabChange} tabpanes={mappedTabs} />
        </div>
        <div style={{ width: "40%", marginTop: "10px" }}>
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
              isLoading={this.state.integration_loading}
              closeMenuOnSelect={true}
              creatable={false}
              isClearable={false}
              placeholder={"Select Integration"}
              onChange={option => {
                console.log(option);

                this.setState({ integration_select: option || {} }, () => {
                  let githubMapping = this.props.github_mapping_form;
                  //githubMapping.resetDefinitions();
                  githubMapping.mappings = undefined;
                  githubMapping.integration_id = option ? option.value : undefined;
                  this.setState(
                    {
                      repos_loading: option !== null && option !== undefined,
                      repos_select: [],
                      repos: []
                    },
                    () => {
                      this.props.formUpdateObj("github_mapping_form", githubMapping);
                      if (option) {
                        let filter = {
                          filter: {
                            integration_id: option.value
                          }
                        };
                        this.props.repositoriesList(filter);
                      }
                    }
                  );
                });
              }}
            />
          </SelectWrapper>
        </div>
        <div style={{ width: "40%", marginTop: "10px" }}>
          <SelectWrapper label={"Repos"}>
            <Select
              options={this.state.repos}
              value={this.state.repos_select}
              isMulti={true}
              closeMenuOnSelect={true}
              isLoading={this.state.repos_loading || this.state.loading || this.state.repo_details_loading}
              placeholder={"Select Repos"}
              onChange={options => {
                let githubMapping = this.props.github_mapping_form;
                githubMapping.repos = options.map(option => ({
                  repo_name: option.name,
                  repo_id: option.value
                  //repo_master_branch: option.branch
                }));
                this.setState({ repos_select: options }, () => {
                  this.props.formUpdateObj("github_mapping_form", githubMapping);
                });
              }}
            />
          </SelectWrapper>
        </div>
        <Row type={"flex"} justify={"end"}>
          <AntButtonGroup>
            <AntButton onClick={this.handleCancel}>Cancel</AntButton>
            <AntButton type={"primary"} onClick={this.handleAdd} disabled={!this.props.github_mapping_form.validate()}>
              {this.props.github_mapping_form.id ? "Update" : "Add"}
            </AntButton>
          </AntButtonGroup>
        </Row>
      </div>
    );
  }
}

IntegrationMapDevContainer.propTypes = {
  className: PropTypes.string,
  stage: PropTypes.object.isRequired,
  github_mapping_form: PropTypes.object.isRequired,
  rest_api: PropTypes.object.isRequired
};

IntegrationMapDevContainer.defaultProps = {
  className: "integration-dev"
};

const mapStatetoProps = state => {
  return {
    ...mapGithubMappingFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(IntegrationMapDevContainer);
