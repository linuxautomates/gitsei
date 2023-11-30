import React, { useEffect } from "react";
import queryString from "query-string";
import { get } from "lodash";
import { useAppStore } from "contexts/AppStoreContext";
import Loader from "components/Loader/Loader";
import { USER_ORG } from "constants/localStorageKeys";
import { getIntegrationUrlMap } from "constants/integrations";
import getUniqueId from "utils/uniqueID";
import { getIsStandaloneApp } from "helper/helper";
import "./credentials-oauth.style.scss";

const CredentialsOauthComponent = props => {
  const integrationMap = getIntegrationUrlMap();
  const configDocUrl = get(integrationMap, [props.type, "config_docs_url"], undefined);
  const { accountInfo, currentMode } = useAppStore();

  useEffect(() => {
    const { type, formData } = props;
    let random = "";
    if (getIsStandaloneApp()) {
      random = localStorage
        .getItem(USER_ORG)
        .concat(Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15));
    } else if (accountInfo) {
      const { identifier = "" } = accountInfo;
      const uniqueID = getUniqueId();
      random = [identifier, currentMode, uniqueID].join("$");
    }
    props.setIntegrationState && props.setIntegrationState(random);
    const currentInformation = {
      name: type,
      form_data: formData
    };
    sessionStorage.setItem(random, JSON.stringify(currentInformation));
    const queryParams = integrationMap[type].query_params;
    queryParams.state = random.toString();
    queryParams.client_id = integrationMap[type].client_id || "";
    const redirectURL = `${integrationMap[type].url || ""}?${queryString.stringify(queryParams)}`;
    setTimeout(() => {
      window.location.replace(redirectURL);
    }, 2000);
  }, []);
  const { className = "credentials-oauth" } = props;

  return (
    <div className={className}>
      Redirecting you to {props.type.toUpperCase()} page to authorize this integration ...
      <Loader />
      {configDocUrl && (
        <a className="link-Container" target="_blank" href={configDocUrl} rel="noopener noreferrer">
          Configuration Guide
        </a>
      )}
    </div>
  );
};

export default CredentialsOauthComponent;
