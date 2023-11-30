import React, { useCallback, useEffect, useMemo, useState } from "react";
import Meta from "antd/lib/card/Meta";
import { forEach, get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useHistory } from "react-router-dom";
import queryString from "query-string";
import { formClear, formInitialize, formUpdateField } from "reduxConfigs/actions/formActions";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { integrationsGet, integrationsUpdate, restapiClear } from "reduxConfigs/actions/restapi";
import { selfOnBoardingFormSelector } from "reduxConfigs/selectors/selfOnboardingIntegrationFormSelector";
import { AntText, AntCard, AntButton, IntegrationIcon } from "shared-resources/components";
import { SELF_ONBOARDING_INTEGRATION_FORM, INTEGRATION_STEP_CONFIGS, SelfOnboardingFormFields } from "../../constants";
import { IntegrationStepsConfigType } from "../../types/integration-step-components-types";
import { WebRoutes } from "routes/WebRoutes";
import { getBreadcumsForEditIntegrationPage } from "../../helpers/getBreadcrumbForEditIntegrationPage";
import { Spin } from "antd";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  getIntegrationUpdatePayload,
  getIntialSelfOnboardingFormState
} from "../../helpers/getIntegrationCreateUpdatePayload";
import { clearCachedIntegration } from "reduxConfigs/actions/cachedIntegrationActions";

interface IntegrationEditConnectContainerProps {
  integration: string;
  integrationStep: number;
}

const IntegrationEditConnectContainer: React.FC<IntegrationEditConnectContainerProps> = (
  props: IntegrationEditConnectContainerProps
) => {
  const { integration, integrationStep } = props;
  const [integrationLoading, setIntegrationLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const { id: integration_id } = queryString.parse(location.search);
  const selfOnboardingFormState = useSelector(selfOnBoardingFormSelector);

  const integrationUpdateState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "update",
    uuid: integration_id
  });

  const integrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "get",
    uuid: integration_id
  });

  const getFromSelfOnboardingForm = useCallback(
    (key: string) => get(selfOnboardingFormState, [key], undefined),
    [selfOnboardingFormState]
  );

  const updateSelfOnboardingForm = useCallback(
    (key: string, value: any) => {
      dispatch(formUpdateField(SELF_ONBOARDING_INTEGRATION_FORM, key, value));
    },
    [selfOnboardingFormState]
  );

  useEffect(() => {
    const data = get(integrationState, ["data"], {});
    if (integration_id) {
      setIntegrationLoading(true);
      if (!Object.keys(data).length) dispatch(integrationsGet(integration_id));
    }
    return () => {
      dispatch(formClear(SELF_ONBOARDING_INTEGRATION_FORM));
    };
  }, []);

  useEffect(() => {
    if (integrationLoading) {
      const loading = get(integrationState, "loading", true);
      const error = get(integrationState, "error", true);
      if (!loading) {
        if (!error) {
          const data = get(integrationState, ["data"], {});
          const initialFormState = getIntialSelfOnboardingFormState(data);
          dispatch(formInitialize(SELF_ONBOARDING_INTEGRATION_FORM, initialFormState));
          updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID, integration_id);
        }
        setIntegrationLoading(false);
      }
    }
  }, [integrationState, integrationLoading]);

  useEffect(() => {
    const loading = get(integrationUpdateState, ["loading"], true);
    const error = get(integrationUpdateState, ["error"], true);
    if (!loading && !error) {
      dispatch(restapiClear("integrations", "update", integration_id as string));
      dispatch(clearCachedIntegration(integration_id as string));
      history.push(`${WebRoutes.integration.list()}?tab=your_integrations`);
    }
  }, [integrationUpdateState, integration_id]);

  const integrationConnectStepConfig: IntegrationStepsConfigType = INTEGRATION_STEP_CONFIGS[integrationStep];

  const handleIntegrationUpdate = () => {
    dispatch(
      integrationsUpdate(
        getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID),
        getIntegrationUpdatePayload(selfOnboardingFormState)
      )
    );
  };

  useEffect(() => {
    const settings = {
      title: "",
      bread_crumbs: getBreadcumsForEditIntegrationPage((integration_id ?? "") as string, integration as string),
      bread_crumbs_position: "before",
      withBackButton: false
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, [integration, integration_id]);

  const renderTitle = useMemo(() => {
    const title = get(integrationConnectStepConfig, ["title"], "");
    return (
      <div className="header">
        {
          // @ts-ignore
          <IntegrationIcon size="large" type={integration} key="integration-icon" />
        }
        <AntText className="section-header">
          {typeof title === "function" ? title((integration ?? "") as string) : title}
        </AntText>
      </div>
    );
  }, [integrationStep, integrationConnectStepConfig, integration]);

  const isSaveButtonDisabled = useMemo(() => {
    if (integrationConnectStepConfig?.required) {
      let disabled = false;
      forEach(integrationConnectStepConfig?.required, key => {
        disabled = disabled || !getFromSelfOnboardingForm(key);
        if (key === SelfOnboardingFormFields.INTEGRATION_NAME) {
          disabled = disabled || !getFromSelfOnboardingForm(SelfOnboardingFormFields.VALID_NAME);
        }
      });
      return disabled;
    }
    return false;
  }, [integrationConnectStepConfig, selfOnboardingFormState]);

  if (integrationLoading)
    return (
      <div className="flex align-center justify-center">
        <Spin />
      </div>
    );

  return (
    <div className="integration-connect-parent-container">
      <div className="integration-connect-container">
        <AntCard>
          <Meta title={renderTitle} description={integrationConnectStepConfig?.description} />
          {React.createElement(integrationConnectStepConfig?.component, {
            integration,
            getFromSelfOnboardingForm,
            updateSelfOnboardingForm,
            selfOnboardingForm: selfOnboardingFormState
          })}
          <div className="integration-nav-buttons">
            <AntButton
              type="default"
              onClick={() => {
                history.push(`${WebRoutes.integration.list()}?tab=your_integrations`);
              }}>
              Cancel
            </AntButton>
            <AntButton type="primary" onClick={handleIntegrationUpdate} disabled={isSaveButtonDisabled}>
              Save
            </AntButton>
          </div>
        </AntCard>
      </div>
    </div>
  );
};

export default IntegrationEditConnectContainer;
