import { Button, Col, Form, Input, notification, Popconfirm, Popover, Row } from "antd";
import { RestUsers } from "classes/RestUsers";
import Loader from "components/Loader/Loader";
import { StateCreateForm } from "configurations/containers/global-settings";
import ErrorWrapper from "hoc/errorWrapper";
import { get } from "lodash";
import moment from "moment";
import React, { useCallback, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { statesDelete } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { stateDeleteState } from "reduxConfigs/selectors/statesSelector";
import {
  AntButton,
  AntCard,
  AntCol,
  AntForm,
  AntFormItem,
  AntInput,
  AntRow,
  AntSelect,
  AntSwitch,
  AntText,
  TableRowActions
} from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { restAPILoadingState } from "utils/stateUtil";
import { getTimezone } from "utils/timeUtils";
import { ERROR, SUCCESS, URL_WARNING } from "../../../../constants/formWarnings";
import { configsList, configsUpdate, usersBulkUpdate } from "reduxConfigs/actions/restapi";
import { getConfigListSelector } from "reduxConfigs/selectors/configSelector";
import { validateURL } from "../../../../utils/stringUtils";
import MFASettingsModal from "../MFASettingsModal";
import "./global-settings.style.scss";
import { scmGlobalCodeChangeSizeUnits } from "../global.constants";
import { GLOBAL_SETTINGS_UUID } from "../../../../dashboard/constants/uuid.constants";
import { DEFAULT_SCM_SETTINGS_OPTIONS } from "../../../../dashboard/constants/defaultFilterOptions";
import { GithubPicker } from "react-color";
import { colorPalletteShades } from "shared-resources/charts/chart-themes";
import queryString from "query-string";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { AUTHENTICATION_WARNING_MESSAGE } from "configurations/pages/constant";
import { getIsStandaloneApp } from "helper/helper";

interface GlobalSettingsPageProps extends RouteComponentProps {}

export const GlobalSettingsPage: React.FC<GlobalSettingsPageProps> = (props: GlobalSettingsPageProps) => {
  const [deleteState, setDeleteState] = useState<string | undefined>(undefined);
  const [deleting, setDeleting] = useState(false);
  const [loadingConfig, setLoadingConfig] = useState(true);

  const [showStateModal, setStateModalVisibility] = useState(false);
  const [selectedState, setSelectedState] = useState<string | undefined>(undefined);
  const [passwordEnabled, setPasswordEnabled] = useState(true);
  const [ssoEnabled, setSsoEnabled] = useState(true);
  const [defaultRole, setDefaultRole] = useState<undefined | string>(undefined);
  const [mfaEnabled, setMFAEnabled] = useState<boolean>(false);
  const [mfaEnrollmentTime, setMFAEnrollmentTime] = useState<number>();
  const [mfaSettingsModal, setMfaSettingsModal] = useState<boolean>(false);
  const [mfaEmailCheck, setMFAEmailCheck] = useState<boolean>(true);
  const [scmData, setSCMData] = useState<any>(DEFAULT_SCM_SETTINGS_OPTIONS);
  const [dashboardColorScheme, setDashboardColorScheme] = useState<any[]>([]);

  const [urlPrefixes, setUrlPrefixes] = useState<string[]>([]);

  const { pathname } = props.location;

  const query = queryString.parse(props.location.search);

  const dispatch = useDispatch();
  const configListState = useSelector(getConfigListSelector);
  const stateDltState = useSelector(stateDeleteState);

  const isStandaloneApp = getIsStandaloneApp();

  useEffect(() => {
    const { loading } = restAPILoadingState(configListState, GLOBAL_SETTINGS_UUID);
    if (!loading && loadingConfig) {
      setLoadingConfig(false);
      const configs = get(configListState, [GLOBAL_SETTINGS_UUID, "data", "records"], []);
      const defaultRole = configs.find((config: any) => config.name === "AUTO_PROVISIONED_ROLE");
      const defaultSAML = configs.find((config: any) => config.name === "DEFAULT_SAML_ENABLED");
      const defaultPassword = configs.find((config: any) => config.name === "DEFAULT_PASSWORD_ENABLED");
      const mfa_enabled = configs.find((config: any) => config.name === "MFA_ENFORCED");
      const mfa_enrollment = configs.find((config: any) => config.name === "MFA_ENROLLMENT_WINDOW");
      const mfa_email_check = configs.find((config: any) => config.name === "NOTIFY_NON_MFA_USER");
      const scm_global_settings = configs.find((config: any) => config.name === "SCM_GLOBAL_SETTINGS");
      const dashboardGlobalSettings = configs.find((config: any) => config.name === "DASHBOARD_COLOR_SCHEME");
      const extensionWhiteListedUrls: string =
        configs.find((config: any) => config.name === "EXTENSION_WHITELIST")?.value || "";
      setDefaultRole(defaultRole ? defaultRole.value : "ADMIN");
      setPasswordEnabled(defaultPassword ? defaultPassword.value === "true" : false);
      setSsoEnabled(defaultSAML ? defaultSAML.value === "true" : false);
      setUrlPrefixes(
        extensionWhiteListedUrls
          .split(",")
          .map(k => k.trim())
          .filter(val => !!val)
      );
      setMFAEmailCheck(mfa_email_check?.value === "true");
      setMFAEnabled(mfa_enabled && mfa_enabled?.value !== "0");
      setMFAEnrollmentTime(parseInt(mfa_enrollment?.value));
      if (scm_global_settings?.value) {
        let data = scm_global_settings.value;
        if (typeof scm_global_settings.value === "string") {
          data = JSON.parse(scm_global_settings.value);
        }
        setSCMData(data);
      }
      if (dashboardGlobalSettings?.value) {
        let data = dashboardGlobalSettings.value;
        if (typeof dashboardGlobalSettings.value === "string") {
          data = JSON.parse(dashboardGlobalSettings.value);
        }
        setDashboardColorScheme(data);
      }
    }
  }, [configListState]);

  useEffect(() => {
    if (deleting) {
      const { loading } = restAPILoadingState(stateDltState, deleteState);
      if (!loading) {
        setDeleting(false);
        setDeleteState(undefined);
      }
    }
  }, [stateDltState]);

  useEffect(() => {
    dispatch(
      setPageSettings(pathname, {
        title: "Customize"
      })
    );
    dispatch(configsList({}, GLOBAL_SETTINGS_UUID as any));
    return () => {
      dispatch(restapiClear("configs", "list", "0"));
      dispatch(restapiClear("states", "list", "0"));
      dispatch(restapiClear("states", "delete", "-1"));
      dispatch(restapiClear("states", "create", "0"));
      dispatch(restapiClear("states", "get", "-1"));
      dispatch(restapiClear("states", "update", "-1"));
      dispatch(restapiClear("configs", "list", GLOBAL_SETTINGS_UUID));
      dispatch(clearPageSettings(pathname));
    };
  }, []);

  useEffect(() => {
    if (!loadingConfig) {
      const scrollTo = query?.scrollTo;
      if (scrollTo) {
        // ADD MORE IF NEEDED LIKE THIS ONLY
        if (scrollTo === "DASHBOARD_COLOR_SCHEME") {
          const element = document.getElementsByClassName("global-dashboard-color-scheme")?.[0];
          if (element) {
            element.scrollIntoView({ behavior: "smooth" });
          }
        }
      }
    }
  }, [loadingConfig]);

  const handleStateEdit = (stageId: string) => {
    setSelectedState(stageId);
    setStateModalVisibility(true);
  };

  const handleStateRemove = (stageId: string) => {
    setDeleteState(stageId);
    setDeleting(true);
    dispatch(statesDelete(stageId));
  };

  const addUrlPrefix = () => {
    setUrlPrefixes([...urlPrefixes, ""]);
  };

  const addDashboardColorScheme = () => {
    setDashboardColorScheme([...dashboardColorScheme, { key: "", value: colorPalletteShades[0] }]);
  };

  const removeUrlPrefix = (index: number) => {
    const url_prefixes = [...urlPrefixes];
    url_prefixes.splice(index, 1);
    setUrlPrefixes(url_prefixes);
  };

  const removeDashboardColorScheme = (index: number) => {
    const colorScheme = [...dashboardColorScheme];
    colorScheme.splice(index, 1);
    setDashboardColorScheme(colorScheme);
  };

  const updateUrlPrefix = (index: number, updatedValue: string) => {
    let url_prefixes = [...urlPrefixes];
    url_prefixes[index] = updatedValue;
    setUrlPrefixes(url_prefixes);
  };

  const updateColorScheme = (index: number, key: string, updatedValue: string) => {
    let colorSchemes = [...dashboardColorScheme];
    colorSchemes[index] = { ...(colorSchemes?.[index] || {}), [key]: updatedValue };
    setDashboardColorScheme(colorSchemes);
  };

  const validateField = (field: string, value: string) => {
    if (field === "url-prefix") {
      if (!!value && value !== "" && !validateURL(value)) {
        return ERROR;
      }
      return "";
    }
    return SUCCESS;
  };

  const renderStatesActions = (stage: any) => {
    const actions = [
      {
        type: "edit",
        id: stage.id,
        onClickEvent: (stageId: string) => handleStateEdit(stageId)
      },
      {
        type: "delete",
        id: stage.id,
        onClickEvent: (stageId: string) => handleStateRemove(stageId)
      }
    ];
    // @ts-ignore
    return <TableRowActions actions={actions} />;
  };

  const handleStateSave = () => {
    setStateModalVisibility(false);
  };

  const handleDefaultRoleChange = (role: string) => {
    dispatch(configsUpdate({ name: "AUTO_PROVISIONED_ROLE", value: role }));
    setDefaultRole(role);
  };

  const handleSSOChange = (checked: boolean) => {
    if (!passwordEnabled && !checked) {
      showError();
      return;
    }
    dispatch(configsUpdate({ name: "DEFAULT_SAML_ENABLED", value: checked }));
    setSsoEnabled(checked);
  };

  const handleMFASettingsChange = useCallback((timestamp: number, emailCheck?: boolean) => {
    setMFAEnabled(true);
    dispatch(configsUpdate({ name: "MFA_ENFORCED", value: moment().unix() }));
    dispatch(
      configsUpdate({
        name: "MFA_ENROLLMENT_WINDOW",
        value: timestamp
      })
    );
    dispatch(
      configsUpdate({
        name: "NOTIFY_NON_MFA_USER",
        value: !!emailCheck
      })
    );
    setMFAEnrollmentTime(timestamp);
    setMfaSettingsModal(false);
  }, []);

  const handleDisableMFA = useCallback(() => {
    setMFAEnabled(false);
    dispatch(configsUpdate({ name: "MFA_ENFORCED", value: 0 }));
  }, []);

  const handlePasswordChange = (checked: boolean) => {
    if (!ssoEnabled && !checked) {
      showError();
      return;
    }
    dispatch(configsUpdate({ name: "DEFAULT_PASSWORD_ENABLED", value: checked }));
    setPasswordEnabled(checked);
  };

  const handleUpdateForAllUsers = useCallback(() => {
    dispatch(
      usersBulkUpdate({
        saml_auth_enabled: ssoEnabled,
        password_auth_enabled: passwordEnabled
      })
    );
  }, [ssoEnabled, passwordEnabled]);

  const handleCreateStateClick = useCallback(() => {
    setSelectedState(undefined);
    setStateModalVisibility(true);
  }, []);

  const handleSCMSettingsChange = useCallback(
    (type: string) => {
      return (value: any) => {
        setSCMData({
          ...scmData,
          [type]: value
        });
      };
    },
    [scmData]
  );

  const showError = () => {
    notification.error({
      message: "Password and SSO cannot be disabled at the same time"
    });
  };

  const stateViewModal = () => {
    if (!showStateModal) {
      return null;
    }
    return (
      <StateCreateForm
        // @ts-ignore
        onCancel={() => setStateModalVisibility(false)}
        onOk={handleStateSave}
        stateId={selectedState}
      />
    );
  };

  const statesColumns = [
    {
      title: "Name",
      key: "name",
      dataIndex: "name"
    },
    {
      title: "Actions",
      key: "id",
      width: 150,
      render: (id: number) => renderStatesActions(id)
    }
  ];

  const renderSSOSettings = () => {
    if (loadingConfig) {
      return <Loader />;
    }
    return (
      <AntCard
        title="Authentication Settings"
        extra={
          <>
            {passwordEnabled ? (
              <AntButton type="primary" onClick={handleUpdateForAllUsers}>
                Update for all users
              </AntButton>
            ) : (
              <Popconfirm
                title={AUTHENTICATION_WARNING_MESSAGE}
                onConfirm={handleUpdateForAllUsers}
                okText="Yes"
                cancelText="No">
                <AntButton type="primary">Update for all users</AntButton>
              </Popconfirm>
            )}
          </>
        }
        className="m-10">
        <div className="sso-container flex align-center justify-space-between">
          <AntCol span={8}>
            <div className={"w-80"}>
              <div className={"flex align-center"}>
                <AntSwitch onChange={handleSSOChange} checked={ssoEnabled} />
                <AntText className={"ml-10"}>SSO</AntText>
              </div>
              <div className={"mt-10"}>
                Authenticate using a SSO Identity Provider. Most secure and recommended option.
              </div>
            </div>
          </AntCol>
          <AntCol span={8}>
            <div className={"w-80"}>
              <div className={"flex align-center"}>
                <AntSwitch onChange={handlePasswordChange} checked={passwordEnabled} />
                <AntText className={"ml-10"}>Password</AntText>
              </div>
              <div className={"mt-10"}>
                Authenticate using email and password. Ideal for small companies without an IdP.
              </div>
            </div>
          </AntCol>
          <AntCol span={8}>
            {passwordEnabled && getRBACPermission(PermeableMetrics.MFA_ENABLE_DISABLED_CONFIGURATION) && (
              <>
                {!mfaEnabled ? (
                  <div className={"w-80"}>
                    <AntButton onClick={() => setMfaSettingsModal(true)} type={"ghost"}>
                      Enable MFA
                    </AntButton>
                    <div className={"mt-10"}>
                      Enable MFA for extra layer of security while using password based authentication.
                    </div>
                  </div>
                ) : (
                  <div className={"w-80"}>
                    <Popconfirm
                      title="Are you sure you want to disable MFA?"
                      onConfirm={handleDisableMFA}
                      okText="Yes"
                      cancelText="Cancel">
                      <AntButton type={"danger"} ghost>
                        Disable MFA
                      </AntButton>
                    </Popconfirm>
                    <div className={"mt-10"}>
                      MFA Enrollment Period ends at:{" "}
                      {`${moment
                        .unix(mfaEnrollmentTime ? mfaEnrollmentTime : moment().add(2, "d").endOf("d").unix())
                        .format("D MMM, YYYY HH:mm")} ${getTimezone()}`}
                    </div>
                  </div>
                )}
              </>
            )}
          </AntCol>
        </div>
      </AntCard>
    );
  };

  const renderStages = () => {
    if (showStateModal) {
      return null;
    }
    return (
      <AntCard title="State settings" className="m-10">
        <AntCol className="gutter-row state-settings" span={24}>
          <div className="states-container flex align-center justify-space-between">
            <AntText>States</AntText>
            <AntButton onClick={handleCreateStateClick} type="primary">
              Create State
            </AntButton>
          </div>
          <ServerPaginatedTable
            pageName="states"
            method="list"
            uri="states"
            columns={statesColumns}
            hasFilters={false}
          />
        </AntCol>
      </AntCard>
    );
  };

  const renderRBAC = () => {
    return (
      <AntCard title="User role settings" className="m-10">
        <AntCol className="gutter-row" span={24}>
          <AntForm layout="vertical">
            <AntFormItem label="Default Role" wrapperCol={{ span: 6 }}>
              <AntSelect
                id="select-type-role"
                options={RestUsers.TYPES.map(option => ({ label: option, value: option }))}
                onChange={handleDefaultRoleChange}
                value={defaultRole}
              />
            </AntFormItem>
          </AntForm>
        </AntCol>
      </AntCard>
    );
  };

  const renderChromeExtensionSettings = () => {
    return (
      <AntCard
        className="m-10"
        title={
          <div>
            <div className="flex align-center justify-end">
              <div className="ant-card-head-title" style={{ padding: "0", fontSize: "17px" }}>
                Chrome extension whitelisted URLs
              </div>
              <Button
                type={"primary"}
                icon={"save"}
                disabled={!!urlPrefixes.filter(urlPrefix => validateField("url-prefix", urlPrefix) === ERROR).length}
                onClick={e =>
                  dispatch(
                    configsUpdate({
                      name: "EXTENSION_WHITELIST",
                      value: urlPrefixes.filter(urlPrefix => !!urlPrefix).toString() || " "
                    })
                  )
                }>
                Save
              </Button>
            </div>
            <div className="chrome-extension-setting-description">
              Configure the list of URL prefixes for which the SEI extension is enabled
            </div>
          </div>
        }>
        <AntCol className="gutter-row" span={24}>
          <AntForm layout="vertical">
            {urlPrefixes.map((urlPrefix, index) => (
              <AntFormItem
                label=""
                wrapperCol={{ span: 24 }}
                validateStatus={validateField("url-prefix", urlPrefix)}
                help={validateField("url-prefix", urlPrefix) === ERROR && URL_WARNING}>
                <Row type="flex" justify="space-between" gutter={[10, 10]} align="middle">
                  <Col span={22}>
                    <Input
                      value={urlPrefix}
                      placeholder={"Url Prefix"}
                      onChange={event => updateUrlPrefix(index, event.currentTarget.value)}
                    />
                  </Col>
                  <Col span={2}>
                    <Button type={"link"} icon={"delete"} onClick={e => removeUrlPrefix(index)} />
                  </Col>
                </Row>
              </AntFormItem>
            ))}
            <Form.Item>
              <Row justify={"start"}>
                <Col span={22}>
                  <Button icon={"plus"} type={"default"} className="mt-10" block={true} onClick={addUrlPrefix}>
                    Add URL Prefix
                  </Button>
                </Col>
              </Row>
            </Form.Item>
          </AntForm>
        </AntCol>
      </AntCard>
    );
  };

  const renderSCMSettings = () => {
    const unit = get(scmData, "code_change_size_unit", "lines");
    const unitText = unit === "lines" ? " lines of code" : " files";

    return (
      <AntCard
        title={
          <div>
            <div className="flex align-center justify-end">
              <div className="ant-card-head-title" style={{ padding: "0", fontSize: "17px" }}>
                SCM Settings
              </div>
              <Button
                type={"primary"}
                icon={"save"}
                onClick={e =>
                  dispatch(
                    configsUpdate({
                      name: "SCM_GLOBAL_SETTINGS",
                      value: JSON.stringify(scmData) || {}
                    })
                  )
                }>
                Save
              </Button>
            </div>
          </div>
        }
        className="m-10">
        <AntCol className="gutter-row" span={24}>
          <AntForm layout="vertical">
            <Form.Item key="code_change_size" label={"Code Change Size"}>
              <div className="code-change-size-container">
                <Form.Item key="code_change_size_unit" label={"Units"}>
                  <AntSelect
                    showArrow
                    allowClear
                    value={unit}
                    options={scmGlobalCodeChangeSizeUnits}
                    mode="single"
                    onChange={handleSCMSettingsChange("code_change_size_unit")}
                  />
                </Form.Item>
                <Form.Item key="code-change-values" label={"Options"}>
                  <div className="code-change-options">
                    <AntText className="code-change-option">
                      {"Small - less than "}
                      <AntInput
                        type="number"
                        value={get(scmData, ["code_change_size_small"], 50)}
                        onChange={handleSCMSettingsChange("code_change_size_small")}
                      />
                      {unitText}
                    </AntText>
                    <AntText className="code-change-option">
                      {"Medium - less than "}
                      <AntInput
                        type="number"
                        value={get(scmData, ["code_change_size_medium"], 150)}
                        onChange={handleSCMSettingsChange("code_change_size_medium")}
                      />
                      {unitText}
                    </AntText>
                    <AntText className="code-change-option">{"Large - everything else"}</AntText>
                  </div>
                </Form.Item>
              </div>
            </Form.Item>
            <Form.Item key="comment-density" label={"PR Code Density"}>
              <div className="comment-density-container">
                <AntText className="comment-density-item">
                  {"Shallow - on average less than "}
                  <AntInput
                    type="number"
                    value={get(scmData, ["comment_density_small"], 1)}
                    onChange={handleSCMSettingsChange("comment_density_small")}
                  />
                  {" comments per PR"}
                </AntText>
                <AntText className="comment-density-item">
                  {"Good - on average less than "}
                  <AntInput
                    type="number"
                    value={get(scmData, ["comment_density_medium"], 5)}
                    onChange={handleSCMSettingsChange("comment_density_medium")}
                  />
                  {" comments per PR"}
                </AntText>
                <AntText className="comment-density-item">{"Heavy - everything else"}</AntText>
              </div>
            </Form.Item>
          </AntForm>
        </AntCol>
      </AntCard>
    );
  };

  const renderDashboardColorScheme = () => {
    return (
      <AntCard
        className="m-10 global-dashboard-color-scheme"
        title={
          <div>
            <div className="flex align-center justify-end">
              <div className="ant-card-head-title" style={{ padding: "0", fontSize: "17px" }}>
                Color Scheme for Insights
              </div>
              <Button
                type={"primary"}
                icon={"save"}
                onClick={e =>
                  dispatch(
                    configsUpdate({
                      name: "DASHBOARD_COLOR_SCHEME",
                      value: dashboardColorScheme?.length
                        ? dashboardColorScheme.filter((item: any) => item?.key?.length && item?.value?.length)
                        : []
                    })
                  )
                }>
                Save
              </Button>
            </div>
            <div className="chrome-extension-setting-description">
              Specify custom colors based on chart labels. This applies to stacked bar charts and pie charts.
            </div>
          </div>
        }>
        <AntCol className="gutter-row" span={24}>
          <AntForm layout="vertical" className={"global-dashboard-color-scheme_item"}>
            {dashboardColorScheme.map((scheme, index) => (
              <AntFormItem key={index} wrapperCol={{ span: 24 }}>
                <Row type="flex" justify="space-between" gutter={[10, 10]} align="middle">
                  <Col span={12}>
                    <Input
                      value={scheme?.key}
                      placeholder={"Label"}
                      onChange={event => updateColorScheme(index, "key", event.target.value)}
                    />
                  </Col>
                  <Col span={8}>
                    <div className={"flex align-center"}>
                      <div
                        style={{
                          height: 20,
                          width: 20,
                          borderRadius: "100%",
                          backgroundColor: scheme?.value,
                          marginRight: 10
                        }}
                      />
                      <Popover
                        title={null}
                        overlayClassName={"global-dashboard-color-scheme_item-popover"}
                        content={
                          <GithubPicker
                            width={"187px"}
                            color={scheme.value}
                            triangle={"hide"}
                            onChangeComplete={color => updateColorScheme(index, "value", color.hex)}
                            colors={colorPalletteShades}
                          />
                        }>
                        <AntButton>Select</AntButton>
                      </Popover>
                    </div>
                  </Col>
                  <Col span={2}>
                    <Button type={"link"} icon={"delete"} onClick={e => removeDashboardColorScheme(index)} />
                  </Col>
                </Row>
              </AntFormItem>
            ))}
            <Form.Item>
              <Row justify={"start"}>
                <Col span={22}>
                  <Button
                    icon={"plus"}
                    type={"default"}
                    className="mt-10"
                    block={true}
                    onClick={addDashboardColorScheme}>
                    Add Label
                  </Button>
                </Col>
              </Row>
            </Form.Item>
          </AntForm>
        </AntCol>
      </AntCard>
    );
  };

  if (loadingConfig || deleting) {
    return <Loader />;
  }

  return (
    <>
      {mfaSettingsModal && (
        <MFASettingsModal
          visible={mfaSettingsModal}
          mfaEnrollmentPeriod={mfaEnrollmentTime}
          onCancel={() => setMfaSettingsModal(false)}
          onSave={handleMFASettingsChange}
          globalSetting
          emailCheck={mfaEmailCheck}
        />
      )}
      <AntRow className="global-settings" gutter={[16, 16]}>
        {isStandaloneApp &&
          <>
            {renderSSOSettings()}
            {renderRBAC()}
            {renderSCMSettings()}
          </>
        }
        {renderDashboardColorScheme()}
        {isStandaloneApp &&
          <>
            {renderStages()}
            {stateViewModal()}
            {renderChromeExtensionSettings()}
          </>
        }
      </AntRow>
    </>
  );
};

export default ErrorWrapper(GlobalSettingsPage);
