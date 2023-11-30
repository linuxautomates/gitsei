import React from "react";
import { connect } from "react-redux";
import { Button, Col, notification, Result, Row, Steps } from "antd";
import { AntButton } from "shared-resources/components";
import { IntegrationCredentials, IntegrationDetails } from "configurations/containers/integration-steps";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { mapIntegrationDispatchtoProps, mapIntegrationStatetoProps } from "reduxConfigs/maps/integrationMap";
import { getIntegrationUrlMap, stringFields } from "constants/integrations";
import queryString from "query-string";
import { RestIntegrations } from "../../../../classes/RestIntegrations";
import { getError, getLoading } from "../../../../utils/loadingUtils";
import Loader from "components/Loader/Loader";
import { PreFlightCheck } from "configurations/components/integrations";
import {
  ERROR,
  FAILED_ADD_INTEGRATION_WARNING,
  PRE_FLIGHT_WARNING,
  SUCCESS,
  SUCCESS_ADD_INTEGRATION
} from "constants/formWarnings";
import { RestApikey } from "../../../../classes/RestApikey";
import FileSaver from "file-saver";
import yaml from "js-yaml";
import { RestTags } from "../../../../classes/RestTags";
import { removeEmptyIntegrations, removeEmptyObject } from "../../../../utils/integrationUtils";
import { get, isString } from "lodash";
import { FORM_DATA_EXCLUDE_KEYS } from "../gitlab-integration/integration-add.page";
import SatelliteIntegrationDetails from "configurations/containers/integration-steps/satellite-integration-details.component";
import GithubWebhookInfo from "./../gitlab-integration/components/github-webhook-info";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, IntegrationAnalyticsActions } from "dataTracking/analytics.constants";
import { WebRoutes } from "routes/WebRoutes";
import { ADD_TO_WORKSPACE_NOTIFICATION_KEY, DEFAULT_URL_YAML } from "../constants";
import LocalStoreService from "services/localStoreService";
import { getBaseAPIUrl } from "constants/restUri";
import { getHomePage, getSettingsPage } from "constants/routePaths";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { useAppStore } from "contexts/AppStoreContext";

const { Step } = Steps;

const hostnameURLConfigMapping = {
  "asia1.app.levelops.io": "https://asia1.api.levelops.io",
  "testui1.levelops.io": "https://testapi1.levelops.io"
};
export class IntegrationAddPage extends React.Component {
  constructor(props) {
    super(props);
    const values = queryString.parse(this.props.location.search);
    let integrationStep = 0;
    let ret = getHomePage(this.props.match);
    this.integrationMap = getIntegrationUrlMap();
    let retTitle = "Integrations";
    if (
      props.integration_type !== undefined &&
      this.integrationMap[props.integration_type] !== undefined &&
      this.integrationMap[props.integration_type].satellite !== undefined &&
      this.integrationMap[props.integration_type].satellite === true
    ) {
      props.integrationForm({ satellite: true });
      integrationStep = 1;
    }
    if (values.state) {
      // retrieve everything from saved session and load it into form
      const callbackStateId = values.state;
      const items = callbackStateId ? JSON.parse(sessionStorage.getItem(callbackStateId)) : {};
      console.log(items);
      if (items && Object.keys(items).length) {
        this.props.integrationType(items.name);
        const keysToKeep = Object.keys(items).filter(key => !FORM_DATA_EXCLUDE_KEYS.includes(key));
        const fieldsToUpdate = {};
        keysToKeep.forEach(key => (fieldsToUpdate[key] = items[key]));
        if (keysToKeep.includes("error")) {
          //this.props.resetIntegrationStore();
          //this.props.setIntegrationStep(0);
          integrationStep = 0;
          this.props.integrationComplete();
          sessionStorage.removeItem(callbackStateId);
        } else {
          //this.props.setIntegrationStep(2);
          integrationStep = 1;
          this.props.integrationForm({ ...fieldsToUpdate });
          sessionStorage.removeItem(callbackStateId);
        }
      }
    }
    if (values.return) {
      ret = values.return.concat(`&product=${values.product}`);
      retTitle = "Return to Products";
    }
    this.state = {
      step: integrationStep,
      return: ret,
      return_title: retTitle,
      loading: false,
      create_loading: false,
      create_error: false,
      created: false,
      preview: [],
      error_message: "",
      apikey_loading: false,
      integration_id: undefined,
      create_tags_loading: false,
      create_tags: []
    };
    this.prev = this.prev.bind(this);
    this.next = this.next.bind(this);
    this.validCredentials = this.validCredentials.bind(this);
    this.validInformation = this.validInformation.bind(this);
    this.createIntegration = this.createIntegration.bind(this);
    this.cancelHandler = this.cancelHandler.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.create_loading) {
      let loading = true;
      let error = state.create_error;
      let created = state.created;
      let id = undefined;
      const closeAddToWorkspaceNotificationHandler = () => {
        props.history.push(WebRoutes.workspace.root());
        notification.close(ADD_TO_WORKSPACE_NOTIFICATION_KEY);
      };
      if (!getLoading(props.rest_api, "integrations", "create", "0")) {
        loading = false;
        created = true;
        if (getError(props.rest_api, "integrations", "create", "0")) {
          error = true;
          console.log("preflight check failed");
        } else {
          id = props.rest_api.integrations.create["0"].data.id;

          // GA EVENT
          emitEvent(
            AnalyticsCategoryType.INTEGRATION,
            IntegrationAnalyticsActions.INTEGRATION_APP_SUCCESS,
            props.integration_type
          );

          if (props.integration_type === "jenkins") {
            props.history.push(`${getSettingsPage()}/integrations/add-integration/jenkins?id=${id}`);
            return;
          }
          const storage = new LocalStoreService();
          storage.setNewIntegrationAdded(id);
          id = props.rest_api.integrations.create["0"].data.id;
          if (props.integration_form.satellite) {
            const apikey = new RestApikey({
              name: `${props.integration_form.name} apikey`,
              role: "INGESTION"
            });
            props.apikeysCreate(apikey);
          }
        }
      }
      return {
        ...state,
        create_loading: loading,
        create_error: error,
        created: created,
        integration_id: id,
        apikey_loading: props.integration_form.satellite && created
      };
    }

    if (state.apikey_loading) {
      let apiLoading = true;
      let apiError = false;
      if (!getLoading(props.rest_api, "apikeys", "create", "0")) {
        apiLoading = false;
        if (getError(props.rest_api, "apikeys", "create", "0")) {
          apiError = true;
        } else {
          const createApikey = props.rest_api.apikeys.create["0"].data.key;
          let _application = props.integration_type || "";
          let apiDataKey = "api_key";
          if (props.integration_type === IntegrationTypes.PERFORCE_HELIX_SERVER) {
            _application = get(this.integrationMap, [props.integration_type, "application"], props.integration_type);
          }
          if (props.integration_type === "circleci") {
            apiDataKey = "apikey";
          }
          let integration = removeEmptyIntegrations({
            id: state.integration_id || "",
            application: _application,
            url: props.integration_form.url || get(this.integrationMap, [props.integration_type, "url"], ""),
            username: props.integration_form.username || "",
            [apiDataKey]: props.integration_form.apikey || "",
            metadata: removeEmptyIntegrations(props.integration_form.metadata || {})
          });

          if (!!this.integrationMap[_application]?.["support_multiple_api_keys"]) {
            integration = {
              ...integration,
              authentication: "multiple_api_keys",
              keys: props.integration_form?.keys || []
            };
          }

          if (this.integrationMap[_application]?.["mapDataForYAML"]) {
            integration = this.integrationMap[_application]?.mapDataForYAML(integration);
          }

          let config = {
            satellite: {
              tenant: window.isStandaloneApp
                ? localStorage.getItem("levelops_user_org") || ""
                : (props.accountInfo?.identifier || "").toLowerCase(),
              api_key: createApikey,
              url: getBaseAPIUrl("")
            },
            integrations: [removeEmptyObject(integration)]
          };

          // Special key for jira only
          if (_application === "jira") {
            config["jira"] = { allow_unsafe_ssl: true };
          }

          const ymlString = yaml.dump(config, { lineWidth: -1 });
          let file = new File([ymlString], `satellite.yml`, { type: "text/plain;charset=utf-8" });
          FileSaver.saveAs(file);
        }
      }
      return {
        ...state,
        apikey_loading: apiLoading,
        apikey_error: apiError
      };
    }

    if (state.create_tags_loading) {
      let tagIds = [];
      let createTagsLoading = false;
      state.create_tags.forEach(tag => {
        if (getLoading(props.rest_api, "tags", "create", tag)) {
          createTagsLoading = true;
        } else {
          if (!getError(props.rest_api, "tags", "create", tag)) {
            tagIds.push(props.rest_api.tags.create[tag].data.id);
          }
        }
      });
      if (!createTagsLoading) {
        console.log("Created new tags");
        let newIntegration = new RestIntegrations();
        let form = { ...props.integration_form };
        newIntegration.name = form.name;
        newIntegration.description = form.description;
        delete form.name;
        delete form.description;
        delete form.size;
        delete form.validating_name;
        delete form.name_exist;
        delete form.__altered;
        newIntegration.application = props.integration_type;
        newIntegration.method = this.integrationMap[props.integration_type].type.toLowerCase();
        newIntegration.supportMultipleApiKeys =
          !!this.integrationMap[props.integration_type]?.support_multiple_api_keys;
        if (!form.hasOwnProperty("url")) {
          newIntegration.url = this.integrationMap[props.integration_type].url;
        }
        const tags = [...form.tags.filter(tag => !tag.key.includes("create:")).map(tag => tag.key), ...tagIds];
        newIntegration.formData = {
          ...form,
          tags: tags
        };
        props.integrationsCreate(newIntegration);
        return {
          ...state,
          create_tags_loading: false,
          create_loading: true,
          integrationData: newIntegration
        };
      }
    }
  }

  prev() {
    const prevStep = this.state.step - 1;
    this.setState({
      step: prevStep
    });
  }

  next() {
    const nextStep = this.state.step + 1;
    this.setState({
      step: nextStep
    });
  }

  validCredentials() {
    const form = this.props.integration_form;
    const fields = this.integrationMap[this.props.integration_type].form_fields;
    let valid = true;
    fields.forEach(field => {
      valid =
        valid &&
        ((field.hasOwnProperty("required") && !field.required) ||
          (!field?.arrayType && form[field.key] !== "" && form[field.key] !== undefined) ||
          (!!field?.arrayType && form[field.key].length > 0));

      const data = form[field.key];
      if (field.arrayType) {
        valid = valid && data.every(value => Object.values(value).every(_value => _value != "" && _value != undefined));
      }
    });
    const metadataFields = get(this.integrationMap, [this.props.integration_type, "metadata_fields"], []);
    const timeZoneField = metadataFields.find(field => field.key === "timezone");
    const timeZoneData = get(form, ["metadata", "timezone"], []);
    if (form?.satellite && timeZoneField && (!timeZoneData.length || !timeZoneData[0])) {
      valid = false;
    }

    const harnessAccountIdFiled = metadataFields.find(field => field.key === "accountId");
    const harnessAccountIdData = get(form, ["metadata", "accountId"], []);
    if (harnessAccountIdFiled && (!harnessAccountIdData.length || !harnessAccountIdData[0])) {
      valid = false;
    }

    return valid;
  }

  validInformation() {
    const form = this.props.integration_form;
    return form.name !== undefined && form.name !== "";
  }

  createIntegration(disablePreflightCheck = false) {
    let newIntegration = new RestIntegrations();
    let form = { ...this.props.integration_form };
    Object.keys(form).forEach(key => {
      if (stringFields.includes(key) && isString(form[key])) {
        form[key] = form[key].trim();
      }
    });
    newIntegration.name = (form.name || "").trim();
    newIntegration.description = form.description;
    delete form.name;
    delete form.description;
    delete form.size;
    delete form.validating_name;
    delete form.name_exist;
    delete form.__altered;
    newIntegration.formData = {
      ...form,
      tags: form.tags ? form.tags.map(tag => tag.key) : []
    };
    const integrationType = this.props.integration_type;
    newIntegration.application = this.integrationMap?.[integrationType]?.application || integrationType;
    newIntegration.method = this.integrationMap[this.props.integration_type].type.toLowerCase();
    newIntegration.supportMultipleApiKeys =
      !!this.integrationMap[this.props.integration_type]?.support_multiple_api_keys;
    if (!form.hasOwnProperty("url")) {
      newIntegration.url = this.integrationMap[this.props.integration_type].url;
    }

    if (this.props.integration_type === IntegrationTypes.AZURE) {
      newIntegration.url = "https://dev.azure.com";
    }

    if (this.integrationMap[integrationType].hasOwnProperty("mapIntegrationForm")) {
      newIntegration.formData = this.integrationMap[integrationType].mapIntegrationForm(newIntegration.formData);
      this.props.integrationForm({ ...newIntegration.formData });
    }

    // create any needed new tags here
    const createTags = newIntegration.formData.tags.filter(tag => tag.toString().includes("create:"));

    if (disablePreflightCheck) {
      newIntegration.formData.skip_preflight_check = true;
    }
    if (createTags.length > 0) {
      this.setState(
        {
          create_tags_loading: true,
          create_tags: createTags
        },
        () => {
          createTags.forEach(tag => {
            let newTag = new RestTags();
            newTag.name = tag.replace("create:", "");
            this.props.tagsCreate(newTag, tag);
          });
        }
      );
    }
    this.setState(
      {
        create_loading: createTags.length === 0,
        create_tags_loading: createTags.length > 0,
        create_tags: createTags,
        integrationData: newIntegration
      },
      () => {
        if (createTags.length === 0) {
          this.props.integrationsCreate(newIntegration);
        }
      }
    );
  }

  componentWillUnmount() {
    this.props.integrationComplete();
    if (this.props.integration_type !== "jenkins") {
      this.props.restapiClear("integrations", "create", "0");
    }
  }

  cancelHandler() {
    this.props.integrationForm({});
    this.props.history.goBack();
  }

  render() {
    if (this.state.created) {
      const status = !this.state.create_loading && !this.state.create_error ? SUCCESS : ERROR;
      if (status === SUCCESS) {
        const autoRegisterWebhook = !!this.state.integrationData?.formData?.metadata?.auto_register_webhook;
        if (
          (!this.state.apikey_loading && this.props.integration_type !== "github") ||
          (!this.state.apikey_loading && autoRegisterWebhook)
        ) {
          this.props.history.push(this.state.return);
        }
      }
      const title =
        !this.state.create_loading && !this.state.create_error
          ? SUCCESS_ADD_INTEGRATION
          : FAILED_ADD_INTEGRATION_WARNING;
      const subTitle =
        !this.state.create_loading && !this.state.create_error
          ? `${this.props.integration_type.toUpperCase()} integration ${this.props.integration_form.name}`
          : PRE_FLIGHT_WARNING;
      const checks =
        (this.props.rest_api.integrations.create["0"].data.preflight_check &&
          this.props.rest_api.integrations.create["0"].data.preflight_check.checks) ||
        [];
      const extras =
        !this.state.create_loading && !this.state.create_error ? (
          <>
            {this.props.integration_type === "github" && (
              <GithubWebhookInfo integration_id={this.state.integration_id} />
            )}
            <AntButton type="primary" onClick={e => this.props.history.push(this.state.return)}>
              {this.state.return_title}
            </AntButton>
          </>
        ) : (
          <Row type="flex" justify="center" gutter={[10, 10]}>
            <Col span={12}>
              <PreFlightCheck checks={checks} />
            </Col>
            <Col span={24}>
              <AntButton
                type="primary"
                onClick={e => {
                  this.props.restapiClear("integrations", "create", "0");
                  this.setState({
                    created: false,
                    create_loading: false,
                    create_error: false,
                    step: 0
                  });
                }}>
                Fix Credentials
              </AntButton>
            </Col>
            <Col span={24}>
              <AntButton
                type="primary"
                onClick={e => {
                  this.props.restapiClear("integrations", "create", "0");
                  this.setState({
                    created: false,
                    create_loading: false,
                    create_error: false
                  });
                  this.createIntegration(true);
                }}>
                Skip Preflight Check (Warning: the scan may not work correctly)
              </AntButton>
            </Col>
          </Row>
        );
      return <Result status={status} title={title} subTitle={subTitle} extra={extras} />;
    }
    if (this.state.create_loading) {
      return <Loader />;
    }

    const firstStepTitle = get(this.integrationMap, [this.props.integration_type, "firstStepTitle"], "Credentials");
    const newDetailPage = get(this.integrationMap, [this.props.integration_type, "newDetailsPage"], false);
    const finalStepButtontext = get(
      this.integrationMap,
      [this.props.integration_type, "finalStepButtontext"],
      "Add Integration"
    );
    const backButtonText = get(this.integrationMap, [this.props.integration_type, "backButtonText"], "Prev");
    const supportCancel = get(this.integrationMap, [this.props.integration_type, "supportCancel"], false);
    const stepButtonBothEnd = get(this.integrationMap, [this.props.integration_type, "stepButtonBothEnd"], true);

    let steps = [];

    // if integration creation is success full

    if (this.props.integration_type === "jenkins") {
      steps = [
        {
          title: "Details",
          component: SatelliteIntegrationDetails,
          next: "Next: Create and Add Nodes",
          props: {
            integration: ""
          },
          valid: () => {
            return (
              this.props.integration_form.name !== undefined &&
              this.props.integration_form.name !== "" &&
              !this.props.integration_form.validating_name &&
              !this.props.integration_form.name_exist
            );
          }
        },
        {
          title: "Add Nodes",
          component: undefined,
          next: "",
          props: {
            integration: ""
          }
        }
      ];
    } else {
      steps = [
        {
          title: firstStepTitle,
          component: IntegrationCredentials,
          next: "Next",
          valid: this.validCredentials,
          props: {
            integration: ""
          }
        },
        {
          title: "Details",
          component: newDetailPage ? SatelliteIntegrationDetails : IntegrationDetails,
          next: finalStepButtontext,
          valid: () => {
            return (
              this.props.integration_form.name !== undefined &&
              this.props.integration_form.name !== "" &&
              !this.props.integration_form.validating_name &&
              !this.props.integration_form.name_exist
            );
          },
          props: {
            integration: "",
            createIntegration: this.createIntegration
          }
        }
      ];
    }

    const nextFunction =
      this.state.step === steps.length - 1 || this.props.integration_type === "jenkins"
        ? this.createIntegration
        : this.next;
    return (
      <div>
        {this.props.integration_type !== undefined && this.props.integration_type !== null && (
          <>
            <Steps current={this.state.step}>
              {steps.map(step => (
                <Step key={step.title} title={step.title} />
              ))}
            </Steps>
            <div style={{ minHeight: this.props.integration_type === "jenkins" ? "280px" : "500px" }}>
              {React.createElement(steps[this.state.step].component, {
                ...steps[this.state.step].props,
                valid: steps[this.state.step].valid
              })}
            </div>
            <Row type="flex" justify={stepButtonBothEnd ? "space-between" : "end"}>
              <Col>
                {this.state.step !== 0 && (
                  <AntButton type="secondary" className="mr-10" onClick={e => this.prev()}>
                    {backButtonText}
                  </AntButton>
                )}
              </Col>
              <Col>
                {supportCancel && this.state.step === 0 && (
                  <AntButton type="secondary" className="mr-10" onClick={this.cancelHandler}>
                    Cancel
                  </AntButton>
                )}
                {((this.state.step === 1 && !this.props.integration_form.satellite) ||
                  this.state.step === 0 ||
                  !newDetailPage) && (
                  <AntButton type="primary" onClick={e => nextFunction()} disabled={!steps[this.state.step].valid()}>
                    {steps[this.state.step].next}
                  </AntButton>
                )}
              </Col>
            </Row>
          </>
        )}
      </div>
    );
  }
}

const mapStatetoProps = state => {
  return {
    ...mapIntegrationStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapIntegrationDispatchtoProps(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

const IntegrationAddPageWrapper = props => {
  const { accountInfo } = useAppStore();
  return <IntegrationAddPage {...props} accountInfo={accountInfo} />;
};

export default connect(mapStatetoProps, mapDispatchtoProps)(IntegrationAddPageWrapper);
