import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import queryString from "query-string";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { AntButton, AntCard, AntText, IntegrationIcon } from "shared-resources/components";
import { getBreadcumsForConnectPage } from "../../helpers/getBreadCrumbsForIntegrationConnectPage";
import "./integrationConnectContainerStyles.scss";
import { IntegrationNavButtonType, IntegrationStepsConfigType } from "../../types/integration-step-components-types";
import { INTEGRATION_STEP_CONFIGS, SelfOnboardingFormFields, SELF_ONBOARDING_INTEGRATION_FORM } from "../../constants";
import { get, map, unset } from "lodash";
import Meta from "antd/lib/card/Meta";
import { formClear, formInitialize, formUpdateField } from "reduxConfigs/actions/formActions";
import { selfOnBoardingFormSelector } from "reduxConfigs/selectors/selfOnboardingIntegrationFormSelector";
import { integrationsCreate, integrationsUpdate, restapiClear } from "reduxConfigs/actions/restapi";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { getIntegrationCreatePayload, initialFormValues } from "../../helpers/getIntegrationCreateUpdatePayload";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { WebRoutes } from "routes/WebRoutes";
import { defaultDashboardSelector } from "reduxConfigs/selectors/dashboardSelector";
import { getBaseUrl } from "constants/routePaths";
import LocalStoreService from "services/localStoreService";

interface IntegrationConnectContainerProps {}

const IntegrationConnectContainer: React.FC<IntegrationConnectContainerProps> = (
  props: IntegrationConnectContainerProps
) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const params = useParams();

  const { integration, code, state } = queryString.parse(location.search);
  const integrationConnectStep = (params as any)?.id === ":id" ? 0 : parseInt((params as any)?.id);
  const selfOnboardingFormState = useSelector(selfOnBoardingFormSelector);
  const isSelfOnboardingUserState = useSelector(isSelfOnboardingUser);
  const defaultDashboardState = useSelector(defaultDashboardSelector);

  useEffect(() => {
    const initialValues = initialFormValues(integration as string);
    dispatch(formInitialize(SELF_ONBOARDING_INTEGRATION_FORM, initialValues));
    return () => {
      dispatch(formClear(SELF_ONBOARDING_INTEGRATION_FORM));
      dispatch(
        restapiClear("integrations", "update", getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID))
      );
    };
  }, []);

  const getFromSelfOnboardingForm = useCallback(
    (key: string) => get(selfOnboardingFormState, [key], undefined),
    [selfOnboardingFormState]
  );

  const updateSelfOnboardingForm = useCallback((key: string, value: any) => {
    dispatch(formUpdateField(SELF_ONBOARDING_INTEGRATION_FORM, key, value));
  }, []);

  const integrationUpdateState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "update",
    uuid: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID)
  });

  const integrationCreateState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "create",
    uuid: "0"
  });

  const redirectRoute = (integrationId?: string) => {
    const defaultDashboardId = get(defaultDashboardState, ["data", "id"]);
    const integration_id = getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID) ?? integrationId;
    const storage = new LocalStoreService();
    storage.setNewIntegrationAdded(integration_id);
    if (defaultDashboardId) {
      history.push(getBaseUrl());
    } else {
      history.push(getBaseUrl());
    }
  };

  useEffect(() => {
    const loading = get(integrationUpdateState, ["loading"], true);
    const error = get(integrationUpdateState, ["error"], true);
    if (!loading && !error) {
      redirectRoute();
    }
  }, [integrationUpdateState, selfOnboardingFormState]);

  useEffect(() => {
    const loading = get(integrationCreateState, ["loading"], true);
    const error = get(integrationCreateState, ["error"], true);
    if (!loading && !error && getFromSelfOnboardingForm(SelfOnboardingFormFields.SATELLITE_INTEGRATION)) {
      updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID, get(integrationCreateState, ["data", "id"]));
      dispatch(restapiClear("integrations", "create", "0"));
      history.push(WebRoutes.self_onboarding.root(integration as string, 3));
    }
  }, [integrationCreateState, selfOnboardingFormState]);

  const integrationConnectStepConfig: IntegrationStepsConfigType = INTEGRATION_STEP_CONFIGS[integrationConnectStep];

  const integrationCreatePayload = useCallback(
    (extraPayload: any, isUpdate = false) =>
      getIntegrationCreatePayload({
        selfOnboardingForm: selfOnboardingFormState,
        code,
        state,
        extraPayload,
        application: (integration ?? "") as string,
        isUpdate
      }),
    [selfOnboardingFormState, code, state, integration]
  );

  const handleSatelliteIntegrationCreate = () => {
    dispatch(
      integrationsCreate(integrationCreatePayload({ start_ingestion: isSelfOnboardingUserState ? false : true }))
    );
  };

  const handleIntegrationUpdate = () => {
    // using integrationCreatePayload because some fields similar in creation are required during updation
    let payload = integrationCreatePayload({ start_ingestion: true }, true);
    dispatch(integrationsUpdate(getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID), payload));
  };

  useEffect(() => {
    const settings = {
      title: "",
      bread_crumbs: getBreadcumsForConnectPage(integration as any, integrationConnectStep),
      bread_crumbs_position: "before",
      withBackButton: false
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, [integration, integrationConnectStep]);

  const renderTitle = useMemo(() => {
    const title = get(integrationConnectStepConfig, ["title"], "");
    const titleClassName = get(integrationConnectStepConfig, ["titleClassName"], "");
    return (
      <div className={`header ${titleClassName}`}>
        {
          // @ts-ignore
          <IntegrationIcon size="large" type={integration} key="integration-icon" />
        }
        <AntText className="section-header">
          {typeof title === "function" ? title((integration ?? "") as string) : title}
        </AntText>
      </div>
    );
  }, [integrationConnectStep, integrationConnectStepConfig, integration]);

  return (
    <div className="integration-connect-parent-container">
      <div className="integration-connect-container">
        <AntCard>
          <Meta title={renderTitle} description={integrationConnectStepConfig?.description} />
          {React.createElement(integrationConnectStepConfig?.component, {
            integration,
            getFromSelfOnboardingForm,
            updateSelfOnboardingForm,
            selfOnboardingForm: selfOnboardingFormState,
            integrationPayload: integrationCreatePayload({})
          })}
          {integrationConnectStepConfig?.nav_buttons.length > 0 && (
            <div className="integration-nav-buttons">
              {map(integrationConnectStepConfig?.nav_buttons, (buttonConfig: IntegrationNavButtonType) => {
                if (
                  buttonConfig.hidden?.({
                    integrationId: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID)
                  })
                ) {
                  return null;
                }
                return (
                  <AntButton
                    type={buttonConfig.type}
                    disabled={
                      buttonConfig.isDisabled &&
                      buttonConfig.isDisabled({
                        name: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME),
                        validName: getFromSelfOnboardingForm(SelfOnboardingFormFields.VALID_NAME),
                        url: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_URL)
                      })
                    }
                    onClick={buttonConfig.onClick({
                      history,
                      integration: integration as string,
                      onClick: getFromSelfOnboardingForm(SelfOnboardingFormFields.SATELLITE_INTEGRATION)
                        ? handleSatelliteIntegrationCreate
                        : handleIntegrationUpdate,
                      integration_id: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID),
                      dashboard_id: get(defaultDashboardState, ["data", "id"])
                    })}>
                    {buttonConfig.title}
                  </AntButton>
                );
              })}
            </div>
          )}
        </AntCard>
      </div>
    </div>
  );
};

export default IntegrationConnectContainer;
