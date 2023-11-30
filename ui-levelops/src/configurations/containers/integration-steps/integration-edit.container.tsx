import React, { useState, useEffect, useMemo, useCallback } from "react";
import { RouteComponentProps } from "react-router-dom";
import { parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { useDispatch } from "react-redux";
import { integrationsGet } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get, capitalize } from "lodash";
import { IntegrationDetailResponse } from "reduxConfigs/actions/restapi/response-types/integrationResponseTypes";
import { INTEGRATION_EDIT_TABS } from "../../../constants/integrationEdit";
import { Tabs } from "antd";
import queryString from "query-string";

import "./integration-edit.style.scss";
import {
  IntegrationDetailsNew,
  IntegrationDetailsOld,
  IntegrationStatus,
  IngestionMonitoring,
  CustomFieldsMapping,
  HygieneRules
} from ".";
import { WebRoutes } from "../../../routes/WebRoutes";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import Loader from "components/Loader/Loader";
import { AntText, IntegrationIcon } from "../../../shared-resources/components";
import { SELF_ONBOARDING_INTEGRATIONS } from "configurations/pages/self-onboarding/constants";
import IntegrationEditConnectContainer from "configurations/pages/self-onboarding/containers/integration-edit-connect-container/IntegrationEditConnectContainer";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IntegrationEditProps extends RouteComponentProps {}

const IntegrationEditContainer: React.FC<IntegrationEditProps> = ({ history, location, match }) => {
  let id: string | undefined = undefined;

  const { id: integrationId } = parseQueryParamsIntoKeys(location.search, ["id"]);
  if (integrationId) {
    id = integrationId[0];
  }

  const dispatch = useDispatch();

  const [loadingIntegration, setLoadingIntegration] = useState<boolean>(true);
  const [integration, setIntegration] = useState<IntegrationDetailResponse>();
  const [selectedTab, setSelectedTab] = useState<INTEGRATION_EDIT_TABS>(INTEGRATION_EDIT_TABS.CONFIGURATION);
  const [application, setApplication] = useState<string>("");
  const enableCustomFieldsHygieneAsTabs = useHasEntitlements(
    Entitlement.CUSTOMFIELDS_HYGIENE_AS_TABS,
    EntitlementCheckType.AND
  );

  const integrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "get",
    uuid: id
  });

  const configurationTab = useMemo(() => {
    return (
      <Tabs.TabPane key={INTEGRATION_EDIT_TABS.CONFIGURATION} tab={"Configuration"}>
        <div>
          {SELF_ONBOARDING_INTEGRATIONS.includes(application) ? (
            <IntegrationEditConnectContainer
              integration={application}
              integrationStep={integration?.satellite ? 2 : 1}
            />
          ) : enableCustomFieldsHygieneAsTabs ? (
            <IntegrationDetailsNew application={application} history={history} location={location} match={match} />
          ) : (
            <IntegrationDetailsOld history={history} location={location} match={match} />
          )}
        </div>
      </Tabs.TabPane>
    );
  }, [enableCustomFieldsHygieneAsTabs, location, history, application, integration]);

  const monitoringTab = useMemo(() => {
    return (
      <Tabs.TabPane key={INTEGRATION_EDIT_TABS.MONITORING} tab={"Monitoring"}>
        <IngestionMonitoring id={id as string} application={application} />
      </Tabs.TabPane>
    );
  }, [id, application]);

  const customFieldsTab = useMemo(() => {
    if (
      enableCustomFieldsHygieneAsTabs &&
      application &&
      [
        IntegrationTypes.JIRA,
        IntegrationTypes.ZENDESK,
        IntegrationTypes.HELIX,
        IntegrationTypes.AZURE,
        IntegrationTypes.TESTRAILS
      ].includes(application as IntegrationTypes)
    ) {
      return (
        <Tabs.TabPane key={INTEGRATION_EDIT_TABS.CUSTOM_FIELDS} tab={"Custom Fields"}>
          <CustomFieldsMapping application={application} history={history} location={location} match={match} />
        </Tabs.TabPane>
      );
    }
    return null;
  }, [application, enableCustomFieldsHygieneAsTabs]);

  const customHygieneTab = useMemo(() => {
    if (
      enableCustomFieldsHygieneAsTabs &&
      application &&
      [IntegrationTypes.JIRA, IntegrationTypes.AZURE].includes(application as IntegrationTypes)
    ) {
      return (
        <Tabs.TabPane key={INTEGRATION_EDIT_TABS.HYGIENE_RULES} tab={"Hygiene Rules"}>
          <HygieneRules application={application} history={history} location={location} match={match} />
        </Tabs.TabPane>
      );
    }
    return null;
  }, [enableCustomFieldsHygieneAsTabs, application]);

  const onTabChange = useCallback(key => {
    history.push({ search: "?id=" + id + "&tab=" + key });
  }, []);

  const setBreadcrumbs = (_id: string, _application: string) => {
    const settings = {
      title: "",
      bread_crumbs: [
        {
          label: "Settings",
          path: WebRoutes.settings.root()
        },
        {
          label: "Integrations",
          path: WebRoutes.settings.integrations()
        },
        {
          label: `${capitalize(_application)} Integration`,
          path: WebRoutes.settings.integration_edit(_id)
        }
      ],
      bread_crumbs_position: "before",
      withBackButton: false,
      showBottomSeparator: false
    };
    dispatch(setPageSettings(location.pathname, settings));
  };

  useEffect(() => {
    if (id) {
      dispatch(integrationsGet(id));
    } else {
      setLoadingIntegration(false);
    }
    return () => {
      dispatch(restapiClear("jira_integration_config", "list", "0"));
      dispatch(restapiClear("integrations", "get", id));
    };
  }, []);

  useEffect(() => {
    if (loadingIntegration && id) {
      const loading = get(integrationState, "loading", true);
      const error = get(integrationState, "error", true);
      if (!loading && !error) {
        const data = get(integrationState, ["data"], {});
        const _application = get(data, "application", "");
        setApplication(_application);
        setIntegration(data);
        setBreadcrumbs(id, _application);
        setLoadingIntegration(false);
      }
    }
  }, [integrationState, application, id]);

  useEffect(() => {
    const values = queryString.parse(location.search);
    let tab: INTEGRATION_EDIT_TABS = (values.tab as INTEGRATION_EDIT_TABS) || INTEGRATION_EDIT_TABS.CONFIGURATION;
    if (
      !enableCustomFieldsHygieneAsTabs &&
      [INTEGRATION_EDIT_TABS.CUSTOM_FIELDS, INTEGRATION_EDIT_TABS.HYGIENE_RULES].includes(tab)
    ) {
      tab = INTEGRATION_EDIT_TABS.CONFIGURATION;
    }
    if (selectedTab !== tab) {
      setSelectedTab(tab);
    }
  }, [location, enableCustomFieldsHygieneAsTabs]);

  if (loadingIntegration) {
    return <Loader />;
  }

  return (
    <div className="integration-edit-container">
      <div className="integration-edit-container__header flex">
        {
          // @ts-ignore
          <IntegrationIcon size="extra-large" type={application} key="integration-icon" />
        }
        <div className="flex direction-column info">
          <AntText className="info--title">{integration?.name}</AntText>
          <AntText className="info--description">{integration?.description}</AntText>
        </div>
        <div className="status">
          <IntegrationStatus id={id as string} application={application as string} />
        </div>
      </div>
      <Tabs activeKey={selectedTab} animated={false} onChange={onTabChange} size="small">
        {configurationTab}
        {customFieldsTab}
        {customHygieneTab}
        {monitoringTab}
      </Tabs>
    </div>
  );
};

export default IntegrationEditContainer;
