import { Icon, Input, message, Modal, notification, Tabs, Tooltip, Upload } from "antd";
import Form from "antd/lib/form";
import { debounce, get, last } from "lodash";
import queryString from "query-string";
import React from "react";
import { connect } from "react-redux";
import { PropelsList, RunsList, TriggersList } from "workflow/containers";
import { RestPropel } from "../../../classes/RestPropel";
import Loader from "../../../components/Loader/Loader";
import { checkTemplateNameExists } from "../../../configurations/helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../../../constants/formWarnings";
import { PROPELS_ROUTES, getBaseUrl } from "constants/routePaths";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { propelsSelector } from "reduxConfigs/selectors/restapiSelector";
import { AntCol, AntText } from "../../../shared-resources/components";
import { propelValidation } from "./helper";
import "./propel.style.scss";
import StringsEn from "../../../locales/StringsEn";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, PropelsActions } from "dataTracking/analytics.constants";
import { Entitlement, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { userEntitlementsState } from "reduxConfigs/selectors/entitlements.selector";
import { checkEntitlements } from "custom-hooks/helpers/entitlements.helper";

const { TabPane } = Tabs;
const { Dragger } = Upload;

class PropelLandingPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      active_key: "triggers",
      import_visible: false,
      import_json: undefined,
      loading_json: false,
      json_import_id: undefined,
      name_field_blur: true,
      import_json_name: undefined,
      import_json_name_checking: false,
      import_json_name_exist: false,
      importing_json: false,
      actions_disabled: false
    };

    this.getValidateStatus = this.getValidateStatus.bind(this);
    this.checkPropelName = this.checkPropelName.bind(this);
    this.isPropels = this.isPropels.bind(this);
    this.setPageActions = this.setPageActions.bind(this);
    this.debounceCheckName = debounce(this.checkPropelName, 300);
  }

  isPropels() {
    return checkEntitlements(this.props.entitlement, Entitlement.PROPELS);
  }

  setPageActions(actions_disabled) {
    let action_buttons = {
      automation_rules: {
        key: "automation_rules",
        type: "secondary",
        label: "Automation Rules",
        hasClicked: false,
        disabled: actions_disabled,
        tooltip: actions_disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""
      },
      import: {
        type: "secondary",
        label: "Import",
        icon: "import",
        hasClicked: false,
        disabled: actions_disabled,
        tooltip: actions_disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""
      }
    };
    this.props.setPageSettings(this.props.location.pathname, {
      title: StringsEn.propels,
      action_buttons: action_buttons
    });
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.props.entitlement !== prevProps.entitlement) {
      const isAllowed = this.isPropels();
      this.setState({ actions_disabled: !isAllowed });
    }
    console.log("componentDidUpdate", prevState.actions_disabled, " - this", this.state.actions_disabled);
    if (prevState.actions_disabled !== this.state.actions_disabled) {
      this.setPageActions(this.state.actions_disabled);
    }
  }

  componentDidMount() {
    this.setPageActions(this.state.actions_disabled);
    // if (this.state.active_key === "automation_rules") {
    //   action_buttons = {
    //     add_automation_rule: {
    //       key: "add_automation_rule",
    //       type: "secondary",
    //       label: "Add Automation Rule",
    //       hasClicked: false
    //     },
    //     ...action_buttons
    //   };
    // }
  }

  static getDerivedStateFromProps(props, state) {
    const values = queryString.parse(props.location.search);
    const tab = values.tab ? values.tab : !state.actions_disabled ? "triggers" : "propels";
    const page = props.page[props.location.pathname];
    if (state.active_key !== tab) {
      let action_buttons = {
        automation_rules: {
          key: "automation_rules",
          type: "secondary",
          label: "Automation Rules",
          hasClicked: false,
          disabled: state.actions_disabled,
          tooltip: state.actions_disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""
        },
        import: {
          key: "import",
          type: "secondary",
          label: "Import",
          icon: "import",
          hasClicked: false,
          disabled: state.actions_disabled,
          tooltip: state.actions_disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""
        }
      };

      // if (tab === "automation_rules") {
      //   action_buttons = {
      //     add_automation_rule: {
      //       type: "secondary",
      //       label: "Add Automation Rule",
      //       hasClicked: false
      //     },
      //     ...action_buttons
      //   };
      // }
      props.setPageSettings(props.location.pathname, {
        ...page,
        action_buttons: action_buttons
      });

      return {
        ...state,
        active_key: tab
      };
    }
    const importButtonClick = get(page, ["action_buttons", "import", "hasClicked"], false);
    if (importButtonClick) {
      console.log("Import button was clicked");
      props.setPageButtonAction(props.location.pathname, "import", { hasClicked: false });
      return {
        ...state,
        import_visible: true
      };
    }

    const addButtonClick = get(page, ["action_buttons", "add_automation_rule", "hasClicked"], false);
    if (addButtonClick) {
      props.setPageButtonAction(props.location.pathname, "add_automation_rule", { hasClicked: false });
      props.history.push(`${getBaseUrl()}${PROPELS_ROUTES.AUTOMATION_RULES}/create`);
    }

    // Automation Rules button click.
    const automationRulesButtonClick = get(page, ["action_buttons", "automation_rules", "hasClicked"], false);
    if (automationRulesButtonClick) {
      props.setPageButtonAction(props.location.pathname, "automation_rules", { hasClicked: false });
      props.history.push(`${getBaseUrl()}${PROPELS_ROUTES.AUTOMATION_RULES}`);
    }

    if (state.import_json_name_checking) {
      const { loading, error } = get(props.propels.list, ["name_list"], { loading: true, error: true });
      if (!loading && !error) {
        const data = props.propels.list["name_list"].data.records;
        props.restapiClear("propels", "list", "name_list");
        return {
          ...state,
          import_json_name_checking: false,
          import_json_name_exist: checkTemplateNameExists(state.import_json_name, data)
        };
      }
    }

    if (state.importing_json) {
      const { loading, error } = get(props.propels.create, ["0"], { loading: true, error: true });
      if (!loading && !error) {
        const propel = props.propels.create["0"].data.permanent_id;
        props.history.push(`${getBaseUrl()}/propels/propels-editor?propel=${propel}`);
        props.restapiClear("propels", "create", "0");
        notification.success({
          message: "Importing propel",
          description: "Finished importing propel from given json file..."
        });
        // GA event PROPEL_ADD
        emitEvent(AnalyticsCategoryType.PROPELS, PropelsActions.PROPEL_ADD);
        return {
          ...state,
          importing_json: false
        };
      }
    }
  }

  getValidateStatus = () => {
    if (this.state.import_json_name_checking) {
      return "validating";
    } else if (!this.state.name_field_blur) {
      return "";
    } else if (
      this.state.name_field_blur &&
      this.state.import_json_name.length > 0 &&
      !this.state.import_json_name_exist
    ) {
      return "success";
    } else return "error";
  };

  checkPropelName = () => {
    const filters = {
      filter: {
        partial: {
          name: this.state.import_json_name
        }
      }
    };
    this.props.propelsList(filters, "name_list");
    this.setState({ import_json_name_checking: true });
  };

  handleImportPropelJSON() {
    const importData = this.state.import_json;
    importData.name = this.state.import_json_name;
    importData.nodes_dirty = true;
    importData.enabled = false;
    this.props.prepelsCreate(importData);
    this.setState({
      import_visible: false,
      import_json: undefined,
      import_json_name: undefined,
      import_json_name_checking: false,
      import_json_name_exist: false,
      importing_json: true
    });
    notification.success({
      message: "Importing propel",
      description: "Started importing the propel from given json file..."
    });
  }

  handleModalOnCancel() {
    this.setState({
      import_visible: false,
      import_json: undefined,
      import_json_name: undefined,
      import_json_name_checking: false,
      import_json_name_exist: false
    });
  }

  getError = () => {
    if (this.state.import_json_name_exist === true) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  };

  get importModal() {
    const uploadProps = {
      name: "file",
      multiple: false,
      showUploadList: false,
      accept: ".json",
      customRequest: data => {
        const _fileName = get(data, ["file", "name"], "");
        const _chunks = _fileName.split(".");

        if (_chunks.length === 0 || last(_chunks) !== "json") {
          message.error("Upload a .json file");
          return;
        }

        const fileReader = new FileReader();
        fileReader.onload = e => {
          if (e && e.target) {
            const _jsonString = e.target.result;
            const _jsonData = JSON.parse(_jsonString);

            const { error } = propelValidation(_jsonData);
            if (error) {
              notification.error({
                message: "Importing propel error",
                description: `${error}. Rectify the file and try again.`
              });
              return;
            }
            this.checkPropelName();
            const restData = new RestPropel(_jsonData);
            this.setState({
              import_json: restData,
              loading_json: true,
              import_json_name: _jsonData.name || StringsEn.propelNew
            });
          }
        };
        fileReader.readAsText(data.file);
      }
    };

    return (
      <Modal
        title={StringsEn.propelImport}
        visible={this.state.import_visible}
        onOk={() => this.handleImportPropelJSON()}
        onCancel={() => this.handleModalOnCancel()}
        okButtonProps={{
          disabled: !this.state.import_json || this.state.import_json_name_checking || this.state.import_json_name_exist
        }}
        okText="Import"
        width="50vw"
        className="propel-modal">
        <div className={!this.state.import_json ? "modal-content-upload-file" : "modal-content-name-validation"}>
          {!this.state.import_json && (
            <Dragger {...uploadProps} style={{ margin: "2rem", width: "calc(100% - 4rem)" }}>
              <p className="ant-upload-drag-icon">
                <Icon type="cloud-upload" />
              </p>
              <AntText style={{ fontSize: "20px" }} type="secondary">
                JSON upload
              </AntText>
              <p className="ant-upload-text">Drag and Drop to upload or click to browse</p>
            </Dragger>
          )}
          {this.state.import_json && (
            <AntCol span={24}>
              <Form layout="vertical">
                <Form.Item
                  label={StringsEn.propelName}
                  validateStatus={this.getValidateStatus()}
                  required
                  hasFeedback={true}
                  help={this.getValidateStatus() === "error" && this.getError()}>
                  <Input
                    name={"Name"}
                    onFocus={() => this.setState({ field_name_blur: false })}
                    onBlur={() => this.setState({ field_name_blur: true })}
                    onChange={e => {
                      this.setState({ import_json_name: e.target.value });
                      this.debounceCheckName();
                    }}
                    value={this.state.import_json_name}
                  />
                </Form.Item>
              </Form>
            </AntCol>
          )}
        </div>
      </Modal>
    );
  }

  render() {
    if (this.state.importing_json) {
      return <Loader />;
    }

    return (
      <div className="propel">
        {this.importModal}
        <Tabs
          size={"small"}
          activeKey={this.state.active_key}
          animated={false}
          onChange={key => {
            this.props.history.push(`${getBaseUrl()}/propels?tab=${key}`);
          }}>
          {!this.state.actions_disabled && (
            <TabPane key={"triggers"} tab={StringsEn.propelAdd}>
              {this.state.active_key === "triggers" && <TriggersList />}
            </TabPane>
          )}
          <TabPane key={"propels"} tab={StringsEn.propelsMy}>
            {this.state.active_key === "propels" && (
              <PropelsList history={this.props.history} location={this.props.location} />
            )}
          </TabPane>
          <TabPane key={"runs"} tab={"My Runs"}>
            {this.state.active_key === "runs" && (
              <RunsList history={this.props.history} location={this.props.location} />
            )}
          </TabPane>
          {/* <TabPane key={"automation_rules"} tab={"Automation Rules"}>
            {this.state.active_key === "automation_rules" && <AutomationRulesList history={this.props.history} />}
          </TabPane> */}
        </Tabs>
      </div>
    );
  }
}

const mapStateToProps = state => {
  return {
    page: getPageSettingsSelector(state),
    propels: propelsSelector(state),
    entitlement: userEntitlementsState(state)
  };
};

const mapDispatchToProps = dispatch => {
  return {
    ...mapPageSettingsDispatchToProps(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(PropelLandingPage);
