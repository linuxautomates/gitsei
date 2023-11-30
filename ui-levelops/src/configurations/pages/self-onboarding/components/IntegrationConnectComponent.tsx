import { Form, Collapse, Divider, Checkbox } from "antd";
import { USER_ORG } from "constants/localStorageKeys";
import { map } from "lodash";
import React from "react";
import { useHistory } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { AntText, AntButton, AntInput } from "shared-resources/components";
import getUniqueId from "utils/uniqueID";
import queryString from "query-string";
import { getAuthorizationConfigs, INTEGRATION_SCOPE, SelfOnboardingFormFields } from "../constants";
import { sanitizeObject } from "utils/commonUtils";
import { IntegrationTypes } from "constants/IntegrationTypes";
import envConfig from "env-config";
import { useAppStore } from "contexts/AppStoreContext";
import { getIsStandaloneApp } from "helper/helper";

interface IntegrationConnectProps {
  integration: string | undefined;
  getFromSelfOnboardingForm: (key: string) => any;
  updateSelfOnboardingForm: (key: string, value: any) => void;
}

const IntegrationConnectComponent: React.FC<IntegrationConnectProps> = (props: IntegrationConnectProps) => {
  const authConfigs = getAuthorizationConfigs();
  const { integration, getFromSelfOnboardingForm, updateSelfOnboardingForm } = props;
  const isSatelliteIntegration = !!getFromSelfOnboardingForm(SelfOnboardingFormFields.SATELLITE_INTEGRATION);
  const history = useHistory();
  const { accountInfo, currentMode } = useAppStore();

  const onOauthRedirectHandler = () => {
    let state = "";
    if (getIsStandaloneApp()) {
      state = localStorage.getItem(USER_ORG)?.concat(getUniqueId()) ?? getUniqueId();
    } else if (accountInfo) {
      const { identifier = "" } = accountInfo;
      const uniqueID = getUniqueId();
      state = [identifier, currentMode, uniqueID].join("$");
    }
    const queryParams = getOauthParamsByIntegration(integration ?? "", state);
    const currentInformation = {
      name: integration
    };
    sessionStorage.setItem(state, JSON.stringify(currentInformation));

    const redirectURL = `${authConfigs[integration ?? ""].oauth}?${queryString.stringify(sanitizeObject(queryParams))}`;

    window.location.replace(redirectURL);
  };

  const onNextStep = () => {
    const nextPage = isSatelliteIntegration ? 2 : 1;
    history.push(WebRoutes.self_onboarding.root(integration, nextPage));
  };

  return (
    <div className="connection-container">
      <div className="flex-row">
        <div className="flex-col">
          <div className="flex-col">
            <AntText className="text_lebel">Authorize using OAUTH</AntText>
            <AntButton
              type="primary"
              className="mt-10"
              onClick={onOauthRedirectHandler}
              disabled={isSatelliteIntegration}>
              Authorize
            </AntButton>
          </div>
          {integration != "bitbucket" && (
            <div>
              <Checkbox
                checked={isSatelliteIntegration}
                onChange={e =>
                  updateSelfOnboardingForm(SelfOnboardingFormFields.SATELLITE_INTEGRATION, e.target.checked)
                }>
                Use Satellite
                <br />
              </Checkbox>
            </div>
          )}
        </div>
        {integration != "bitbucket" ? (
          <>
            <div className="flex direction-column mt-20">
              <Divider type="vertical" style={{ height: "4rem" }} />
              <AntText className="">{`OR`}</AntText>
              <Divider type="vertical" style={{ height: "4rem" }} />
            </div>
            <div>
              <AntText className="text_lebel"> {`CONNECT WITH ACCESS TOKENS`}</AntText>
              <Form layout={"vertical"}>
                <ol>
                  <li className="mt-10">
                    <AntText>
                      {`Create an `}
                      <a href={authConfigs[integration ?? ""].pac} className="link" target="_blank">
                        Personal Access Token
                      </a>
                    </AntText>
                  </li>
                  <li className="mt-10">
                    <AntText>{`Make sure these scopes are selected `}</AntText>
                    <div className="flex">
                      {map(INTEGRATION_SCOPE[integration ?? "github"], key => (
                        <AntText className="scope">{key}</AntText>
                      ))}
                    </div>
                  </li>
                  <li className="mt-10">
                    <AntText>{`Enter the Access Token`}</AntText>
                    <AntInput
                      type="password"
                      id="password"
                      placeholder="*******************"
                      value={getFromSelfOnboardingForm(SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN)}
                      onChange={(e: any) =>
                        updateSelfOnboardingForm(SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN, e.target.value)
                      }
                      autocomplete="new-password"
                      data-lpignore={true}
                    />
                  </li>
                </ol>
                <AntButton
                  type="default"
                  className="ml-10"
                  style={{ marginLeft: "2.5rem" }}
                  onClick={onNextStep}
                  disabled={!getFromSelfOnboardingForm(SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN)}>
                  Connect
                </AntButton>
              </Form>
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
};

const getOauthParamsByIntegration = (integration: string, state: Object) => {
  const authConfigs = getAuthorizationConfigs();
  let envPath = null;
  try {
    envPath = window.location.origin;
  } catch (err) {
    // Not able to get from window.location, using env variable as fallback
    envPath = envConfig.get("UI_URL");
  }

  let redirect_uri = `${envPath}/integration-callback`;
  if (!getIsStandaloneApp()) {
    redirect_uri = `${envPath}/api/sei-integration/redirect/${integration}`;
  }

  let commonObject = {
    client_id: authConfigs[integration ?? ""].client_id,
    scope: authConfigs[integration ?? ""].scope.join(" "),
    state,
    redirect_uri
  };

  switch (integration) {
    case IntegrationTypes.GITHUB: {
      return {
        ...commonObject
      };
    }
    case IntegrationTypes.GITLAB: {
      return {
        ...commonObject,
        response_type: "code"
      };
    }
    case IntegrationTypes.BITBUCKET: {
    }
    default: {
      return {
        ...commonObject,
        response_type: "code"
      };
    }
  }
};

export default IntegrationConnectComponent;
