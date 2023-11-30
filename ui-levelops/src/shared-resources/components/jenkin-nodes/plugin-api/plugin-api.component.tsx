import * as React from "react";
import { AntTextComponent as AntText } from "../../ant-text/ant-text.component";
import AntIconComponent from "../../ant-icon/ant-icon.component";

import "./plugin-api.style.scss";
import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { get } from "lodash";
import { apikeysCreate, apikeysDelete } from "reduxConfigs/actions/restapi/apikeyActions";
import Loader from "components/Loader/Loader";
import { RestApikey } from "classes/RestApikey";
import {
  getGenericMethodSelector,
  getGenericUUIDSelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";

const JENKINS_PLUGIN_URL = "https://github.com/levelops-tools-admin/levelops-plugins/releases/latest";

interface PluginApiProps {
  show: boolean;
  setShow: (value: boolean) => void;
  integrationName: string;
}

const PluginApi: React.FC<PluginApiProps> = ({ show, setShow, integrationName }) => {
  const dispatch = useDispatch();

  const [creatingApiKey, setCreatingApiKey] = useState(false);
  const [apiKeyName, setApiKeyName] = useState<string>(integrationName);
  const [apiKey, setApiKey] = useState<any>({});
  const [deletingApiKey, setDeletingApiKey] = useState<boolean>(false);

  const apiKeyCreateState = useParamSelector(getGenericUUIDSelector, {
    uri: "apikeys",
    method: "create",
    uuid: "0"
  });

  const apiKeyDeleteState = useParamSelector(getGenericMethodSelector, {
    uri: "apikeys",
    method: "delete"
  });

  const createApiKey = () => {
    const payload = new RestApikey({
      name: apiKeyName,
      description: "For new jenkins instance",
      role: "admin"
    });

    dispatch(apikeysCreate(payload));
    setCreatingApiKey(true);
  };

  const onNameChangeHandler = (newName: string) => {
    setApiKeyName(newName);

    // deleting existing key
    dispatch(apikeysDelete(apiKey.id));
    setDeletingApiKey(true);

    // createApiKey();
  };

  useEffect(() => {
    createApiKey();
  }, []);

  useEffect(() => {
    if (creatingApiKey) {
      const loading = get(apiKeyCreateState, "loading", true);
      const error = get(apiKeyCreateState, "error", true);
      if (!loading && !error) {
        const data = get(apiKeyCreateState, ["data"], {});
        setApiKey(data);
        setCreatingApiKey(false);
      }
    }
  }, [apiKeyCreateState]);

  useEffect(() => {
    if (deletingApiKey) {
      const loading = get(apiKeyDeleteState, [apiKey.id, "loading"], true);
      const error = get(apiKeyDeleteState, [apiKey.id, "error"], true);
      if (!loading && !error) {
        setCreatingApiKey(false);

        // creating new key
        createApiKey();
        setDeletingApiKey(false);
      }
    }
  }, [apiKeyDeleteState]);

  return (
    <div className="plugin-api">
      <div className="flex justify-space-between">
        <AntText className="plugin-api--installation-heading">Install Instructions</AntText>
        <AntIconComponent className="plugin-api--close-icon" type="close-circle" onClick={() => setShow(false)} />
      </div>
      <div className="plugin-install-steps">
        <ol className="plugin-install-steps-list">
          <li>Sign-in to Jenkins and select Manage Jenkins</li>
          <li>Select "Manage Plugins"</li>
          <li>Select the "Available plugins" tab</li>
          <li>In the "Search plugin", type in "propelo"</li>
          <li>Install the plugin "Propelo Job Reporter" and click on "Install without restart"</li>
          <li>
            Once the plugin installation is complete, the status will change to "Success". If it doesn't change to
            "Success", then a restart might be needed
          </li>
          <li>Set the instance name and use the API key below in "Manage Jenkins" {">>"} Propelo Job Reporter</li>
        </ol>
        <div className="additional-details-link">
          Please click{" "}
          <a target="_blank" href="https://github.com/jenkinsci/propelo-job-reporter-plugin">
            here
          </a>{" "}
          for more details about the installation
        </div>
      </div>
      <AntText className="heading">API KEY</AntText>
      {(creatingApiKey || deletingApiKey) && (
        <div>
          Creating api key, please wait <Loader />
        </div>
      )}
      {!creatingApiKey && !deletingApiKey && (
        <>
          <div className="plugin-api__api-key-container">
            <AntText className="plugin-api__api-key-container--name" editable={{ onChange: onNameChangeHandler }}>
              {apiKeyName}
            </AntText>
          </div>
          <div className="plugin-api__api-key-container">
            <AntText copyable className="plugin-api__api-key-container--key">
              {apiKey.key}
            </AntText>
          </div>
        </>
      )}
      <AntText className="description">
        Note: Please copy the API Key and place it in the Jenkins Configuration.
      </AntText>
      <br />
      <AntText className="description">Newly added instances will show up under "Available Instances"</AntText>
    </div>
  );
};

export default PluginApi;
