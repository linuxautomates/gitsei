import { Form, Input, Switch, Typography, Tabs, Radio, Icon } from "antd";
import { RestDashboard } from "classes/RestDashboards";
import { ERROR, NAME_EXISTS_ERROR, REQUIRED_FIELD, SUCCESS, EMAIL_ERROR } from "constants/formWarnings";
import { debounce, get, isEqual } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dashboardsList, newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { getDashboard, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntCol, AntModal, AntRow, AntText } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import queryString from "query-string";
import { RouteComponentProps, withRouter } from "react-router-dom";
import "./dashboard-settings-modal.scss";
import {
  ALLOWED_LIMIT,
  DashboardSettingsModalTitleType,
  onOkTextType,
  ALL_ACCESS_USERS,
  DASHBOARD_SETTINGS_USER_PERMISSIONS,
  DASHBOARD_SETTINGS_PERMISSIONS
} from "./helper";
import { ticketCategorizationSchemesList } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { DASHBOARD_OU_FIELD } from "dashboard/constants/header-widget.constant";
import { EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID, INTEGRATION_MONITORING_KEY } from "./constant";
import LocalStoreService from "services/localStoreService";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import DashboardPermissionTab from "./DashboardPermissionTab";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";

const { TabPane } = Tabs;

interface DashboardCreateModalProps {
  dashboardId: any;
  visible: boolean;
  toggleModal: (param?: boolean) => void;
}

const DashboardSettingsModal: React.FC<DashboardCreateModalProps & RouteComponentProps> = (
  props: DashboardCreateModalProps & RouteComponentProps
) => {
  const { dashboardId, visible, toggleModal } = props;
  const [dashboardTitle, setDashboardTitle] = useState<any>(undefined);
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);
  const [nameExists, setNameExits] = useState<boolean>(false);
  const [nameSearchLoading, setNameSearchLoading] = useState<boolean>(false);
  const [query, setQuery] = useState<any>(undefined);
  const [dirty, setDirty] = useState<boolean>(false);
  const [defaultEffortInvestment, setDefaultEffortInvestment] = useState<any>({});

  const checkNameListId = useRef<any>(undefined);
  const defaultInParams = useRef<boolean>(false);
  const isDefault = useRef<boolean>(false);
  const haveDashboardTimeRange = useRef<boolean>(true);
  const haveEffortInvestmentProfile = useRef<boolean>(false);
  const haveEffortInvestmentUnit = useRef<boolean>(false);
  const haveIntegrationMonitoring = useRef<boolean>(false);
  const defaultRef = useRef<any>();
  const dashboardTimeRangeRef = useRef<any>();
  const effortInvestmentProfileRef = useRef<any>();
  const effortInvestmentUnitsRef = useRef<any>();
  const integrationMonitoringRef = useRef<any>();
  const ls = new LocalStoreService();
  const userId = ls.getUserId();
  const email = ls.getUserEmail();
  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const dispatch = useDispatch();
  const dashboard = useSelector(selectedDashboard);

  const { workspace_id } = queryString.parse(props.location.search);

  const [permissions, setPermissions] = useState<any>({
    rbac: { owner: dashboard?._metadata?.rbac?.owner || email, dashboardPermission: "admin", users: {} }
  });
  const checkNameListState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboards",
    method: "list",
    uuid: checkNameListId.current
  });

  const effortInvestmentSchemaState = useParamSelector(getGenericRestAPISelector, {
    uri: "ticket_categorization_scheme",
    method: "list",
    uuid: EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID
  });

  useEffect(() => {
    const curDefaultInParams = queryString.parse(props.location.search).default;
    defaultInParams.current = curDefaultInParams === "true";
    dispatch(
      ticketCategorizationSchemesList(
        { filter: { default: true }, page_size: 1000 },
        EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID
      )
    );
  }, []);

  useEffect(() => {
    const loading = get(effortInvestmentSchemaState, "loading", true);
    const error = get(effortInvestmentSchemaState, "error", true);
    if (!loading && !error) {
      const records = get(effortInvestmentSchemaState, ["data", "records"], {});
      const defaultValue = records.find((item: any) => item.default_scheme);
      setDefaultEffortInvestment(defaultValue);
    }
  }, [effortInvestmentSchemaState]);

  useEffect(() => {
    if (dashboard) {
      isDefault.current = dashboard.default;
      haveDashboardTimeRange.current = isDashboardTimerangeEnabled(dashboard._metadata);
      haveEffortInvestmentProfile.current = dashboard._metadata?.effort_investment_profile || false;
      haveEffortInvestmentUnit.current = dashboard._metadata?.effort_investment_unit || false;
      haveIntegrationMonitoring.current = get(dashboard, ["_metadata", INTEGRATION_MONITORING_KEY], false);
      const rbac = dashboard._metadata?.rbac;

      if (dashboardTitle === undefined) {
        setDashboardTitle(dashboard.name);
      }
      if (query === undefined) {
        setQuery(dashboard.query);
      }
      if (rbac) {
        setPermissions({ rbac });
      }
    }
  }, [dashboard]);

  const checkTemplateName = (title: any) => {
    const filters = {
      filter: {
        partial: {
          name: title?.trim()
        },
        workspace_id: !!workspace_id || !!selectedWorkspace?.id ? parseInt(workspace_id ?? selectedWorkspace?.id) : ""
      }
    };
    const nCheckNameListId = uuid();
    dispatch(dashboardsList(filters, nCheckNameListId));
    setNameSearchLoading(true);
    checkNameListId.current = nCheckNameListId;
  };

  useEffect(() => {
    if (dashboardTitle && nameSearchLoading) {
      const loading = get(checkNameListState, ["loading"], true);
      const error = get(checkNameListState, ["error"], true);
      if (!loading && !error) {
        const apiData = get(checkNameListState, ["data", "records"], []);
        const resNameExists =
          !!apiData?.filter((item: any) => item?.name?.toLowerCase() === dashboardTitle?.trim().toLowerCase())
            ?.length || false;
        checkNameListId.current = undefined;
        setNameExits(resNameExists);
        setNameSearchLoading(false);
      }
    }
  }, [checkNameListState]);

  const debounceCheckName = debounce(checkTemplateName, 300);

  const isNameLong = useMemo((): boolean => {
    return (dashboardTitle || "").trim().length > ALLOWED_LIMIT;
  }, [dashboardTitle]);

  const isNameEmpty = useMemo((): boolean => {
    return dashboardTitle?.trim().length === 0;
  }, [dashboardTitle]);

  const getValidateStatus = useMemo(() => {
    if (!nameFieldBlur) return "";
    if (nameFieldBlur && (dashboardTitle || "").length > 0 && !nameExists) return SUCCESS;
    return ERROR;
  }, [dashboardTitle, nameExists, nameFieldBlur]);

  const getError = useMemo(() => {
    if (dashboardTitle === "" || !nameExists) return REQUIRED_FIELD;
    return NAME_EXISTS_ERROR;
  }, [nameExists, dashboardTitle]);

  const handleDashboardTitleChange = useCallback(
    (e: any) => {
      setDashboardTitle(e.target.value);
      debounceCheckName(e.target.value);
    },
    [dashboardTitle, query]
  );

  const handleNameFieldBlurChange = useCallback(() => {
    setNameFieldBlur(true);
  }, [dashboardTitle]);

  const getMetaInfo = () => {
    let _metadata: any = {
      dashboard_time_range: dashboardTimeRangeRef.current?.rcSwitch?.state?.checked,
      effort_investment_profile: effortInvestmentProfileRef.current?.rcSwitch?.state?.checked,
      effort_investment_unit: effortInvestmentUnitsRef.current?.rcSwitch?.state?.checked,
      [INTEGRATION_MONITORING_KEY]: integrationMonitoringRef.current?.rcSwitch?.state?.checked,
      dashboard_time_range_filter: dashboard?._metadata?.dashboard_time_range_filter,
      effort_investment_unit_filter: dashboard?._metadata?.effort_investment_unit_filter,
      rbac: permissions.rbac,
      ou_category_id: dashboard?._metadata?.ou_category_id
    };

    const dashboardTimeRange = isDashboardTimerangeEnabled(_metadata);
    if ((dashboardTimeRange && !_metadata.dashboard_time_range_filter) || !dashboardTimeRange) {
      _metadata.dashboard_time_range_filter = "last_30_days";
    }

    if (_metadata.effort_investment_profile && dirty) {
      _metadata.effort_investment_profile_filter = defaultEffortInvestment?.id;
    }
    if (!_metadata.effort_investment_profile) {
      _metadata.effort_investment_profile_filter = undefined;
    }

    if (
      (_metadata.effort_investment_unit && !_metadata.effort_investment_unit_filter) ||
      !_metadata.effort_investment_unit
    ) {
      _metadata.effort_investment_unit_filter = "%_of_engineers";
    }

    return _metadata;
  };

  const handleOnOk = useCallback(() => {
    const form = {
      name: dashboardTitle,
      default: defaultRef.current?.rcSwitch?.state?.checked,
      public: [DASHBOARD_SETTINGS_PERMISSIONS.LIMITED, DASHBOARD_SETTINGS_PERMISSIONS.PUBLIC].includes(
        permissions.rbac.dashboardPermission
      ),
      query,
      metadata: getMetaInfo()
    };
    dispatch(newDashboardUpdate(dashboardId, form, true));
    setDirty(false);
    toggleModal();
  }, [dashboardTitle, defaultRef.current, permissions?.rbac?.dashboardPermission, query, dirty, getMetaInfo]);

  const handleCancel = useCallback(() => {
    setDashboardTitle(dashboard?.name);
    setDirty(false);
    toggleModal();
  }, [dirty]);

  const isPermissionsTabAllowed = getPermissionsTabAllowed({
    rbac: dashboard?._metadata?.rbac,
    email,
    userId,
    owner_id: dashboard?.owner_id
  });

  const basicTabContent = (
    <AntRow gutter={[16, 16]}>
      <AntCol span={24}>
        <Form layout="vertical">
          <Form.Item
            label={DashboardSettingsModalTitleType.NAME}
            required
            validateStatus={getValidateStatus}
            hasFeedback={true}
            help={getValidateStatus === ERROR && getError}
            colon={false}>
            <Input
              name={DashboardSettingsModalTitleType.NAME}
              onChange={handleDashboardTitleChange}
              value={dashboardTitle}
              onFocus={handleNameFieldBlurChange}
            />
            {isNameLong && <span className="error-msg">Maximum character limit {ALLOWED_LIMIT} reached.</span>}
          </Form.Item>
          <Form.Item label={DashboardSettingsModalTitleType.PARAMETERS} colon={false}>
            <div className="form-content">
              <Typography.Text className="form-content__text">Insight Time Range</Typography.Text>
              <Switch
                className="form-content__switch"
                ref={input => (dashboardTimeRangeRef.current = input)}
                defaultChecked={haveDashboardTimeRange.current}
              />
            </div>
            <div className="form-content">
              <Typography.Text className="form-content__text">Effort Investment Profile</Typography.Text>
              <Switch
                onChange={value => {
                  setDirty(true);
                }}
                className="form-content__switch"
                ref={input => (effortInvestmentProfileRef.current = input)}
                defaultChecked={haveEffortInvestmentProfile.current}
              />
            </div>
            <div className="form-content">
              <Typography.Text className="form-content__text">Effort Investment Units</Typography.Text>
              <Switch
                className="form-content__switch"
                ref={input => (effortInvestmentUnitsRef.current = input)}
                defaultChecked={haveEffortInvestmentUnit.current}
              />
            </div>
            <div className="form-content">
              <AntText className="form-content__text">Integration Monitoring</AntText>
              <Switch
                className="form-content__switch"
                ref={input => (integrationMonitoringRef.current = input)}
                defaultChecked={haveIntegrationMonitoring.current}
              />
            </div>
          </Form.Item>
        </Form>
      </AntCol>
    </AntRow>
  );

  return (
    <>
      {visible && (
        <AntModal
          width={600}
          title={
            dashboard
              ? DashboardSettingsModalTitleType.DASHBOARD_SETTINGS
              : DashboardSettingsModalTitleType.CREATE_DASHBOARD
          }
          mask={true}
          maskClosable={false}
          visible={visible}
          onOk={handleOnOk}
          okButtonProps={{
            disabled: isNameEmpty || isNameLong || nameExists
          }}
          // TODO handle redirect to list page if there is no existing dashboard
          onCancel={handleCancel}
          okText={dashboard ? onOkTextType.UPDATE : onOkTextType.CREATE}
          closable={true}>
          {window.isStandaloneApp ? (
            <Tabs defaultActiveKey="1">
              <TabPane tab="Basic" key="1">
                {basicTabContent}
              </TabPane>
              <TabPane tab="Permissions" key="2" disabled={!isPermissionsTabAllowed}>
                <DashboardPermissionTab
                  email={email}
                  permissions={permissions}
                  setPermissions={(key: string, val: any) => {
                    setPermissions(val);
                  }}
                />
              </TabPane>
            </Tabs>
          ) : (
            basicTabContent
          )}
        </AntModal>
      )}
    </>
  );
};

const getPermissionsTabAllowed = (data: any) => {
  let { rbac, email, userId, owner_id } = data;

  // true if the user is owner under rbac object
  if (rbac?.[DASHBOARD_SETTINGS_USER_PERMISSIONS.OWNER] === email) return true;

  // true if the email is in the rbac.users object as "owner"
  if (rbac?.users && rbac?.users?.[email as any]?.permission === DASHBOARD_SETTINGS_USER_PERMISSIONS.OWNER) return true;

  // true if the original dashboard creator is this user
  if (userId === owner_id) return true;

  // special internal emails/cases
  if (ALL_ACCESS_USERS.includes(email || "")) return true;

  // Anything else is not allowed
  return false;
};

export default withRouter(DashboardSettingsModal);
