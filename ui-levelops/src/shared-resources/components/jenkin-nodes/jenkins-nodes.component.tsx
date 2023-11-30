import * as React from "react";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { Alert } from "antd";

import "./jenkins-node.style.scss";
import InstanceCard from "./instance-card/Instance-card";
import { useEffect, useState } from "react";
import Loader from "components/Loader/Loader";
import { getInstanceStatus } from "./helper";
import { useDispatch, useSelector } from "react-redux";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { JENKINS_INTEGRATIONS_URI } from "reduxConfigs/actions/restapi/uri.constants";
import { jenkinsIntegrationsList } from "reduxConfigs/actions/restapi/jenkins-integrations.actions";
import { get } from "lodash";
import PluginApi from "./plugin-api/plugin-api.component";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import {
  jenkinsAvailableIntegrationListSelector,
  jenkinsIntegrationListSelector
} from "reduxConfigs/selectors/jenkinsIntegrationSelector";

interface JenkinsNodesProps {
  onIntegrationChange: (data: any) => void;
  integrationId: string;
  integrationName: string;
}

const AVAILABLE_INTEGRATIONS = "available_integrations";
const POLLING_INTERVAL = 30000; // 30 secs

const JenkinNodes: React.FC<JenkinsNodesProps> = ({ onIntegrationChange, integrationId, integrationName }) => {
  const dispatch = useDispatch();

  const [jenkinsIntegrationsLoading, setJenkinsIntegrationsLoading] = useState<boolean>(true);
  const [availableIntegrationsLoading, setAvailableIntegrationsLoading] = useState<boolean>(true);
  const [incidentHealthCheckCount, setIncidentHealthCheckCount] = useState<number>(0);
  const [filteredData, setFilteredData] = useState<any[]>([]);
  const [availableData, setAvailableData] = useState<any[]>([]);
  const [addInstance, setAddInstance] = useState<boolean>(false);
  const [hasNextJenkinsIntegration, setHasNextJenkinsIntegrations] = useState<boolean>(false);
  const [hasNextAvailableIntegration, setHasNextAvailableIntegrations] = useState<boolean>(false);
  const [polling, setPolling] = useState<boolean>(false);
  const [attachingInstanceId, setAttachingInstanceId] = useState<string>();
  const [detachingInstanceId, setDetachingInstanceId] = useState<string>();

  const jenkinsIntegrations = useParamSelector(getGenericUUIDSelector, {
    uri: JENKINS_INTEGRATIONS_URI,
    method: "list",
    uuid: "0"
  });

  const availableIntegrations = useParamSelector(getGenericUUIDSelector, {
    uri: JENKINS_INTEGRATIONS_URI,
    method: "list",
    uuid: AVAILABLE_INTEGRATIONS
  });

  const updateIntegrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "jenkins_integrations",
    method: "update",
    uuid: integrationId
  });

  const jenkinsIntegrationList = useSelector(jenkinsIntegrationListSelector);
  const availableIntegrationList = useSelector(jenkinsAvailableIntegrationListSelector);

  const getAvailableIntegrations = (_polling = true, fetchNextPage = false) => {
    const filter = {
      missing_fields: {
        integration_id: true
      }
    };
    dispatch(jenkinsIntegrationsList(_polling, fetchNextPage, { filter }, AVAILABLE_INTEGRATIONS));
    if (_polling) {
      setPolling(_polling);
    } else {
      setAvailableIntegrationsLoading(true);
    }
  };

  const getJenkinsIntegrations = (fetchNextPage = false) => {
    const filter = {
      integration_ids: [integrationId]
    };
    dispatch(jenkinsIntegrationsList(false, fetchNextPage, { filter }));
    setJenkinsIntegrationsLoading(true);
  };

  useEffect(() => {
    getJenkinsIntegrations();
    getAvailableIntegrations(false);
    const polling = setInterval(() => {
      getAvailableIntegrations();
    }, POLLING_INTERVAL);

    return () => {
      clearInterval(polling);
      dispatch(restapiClear(JENKINS_INTEGRATIONS_URI, "list", "-1"));
    };
  }, []);

  useEffect(() => {
    const _loading = get(updateIntegrationState, "loading", true);
    const error = get(updateIntegrationState, "error", true);
    if (!_loading && !error) {
      const filter = {
        integration_ids: [integrationId]
      };
      dispatch(jenkinsIntegrationsList(false, false, { filter }));
      const available_integration_filter = {
        missing_fields: {
          integration_id: true
        }
      };
      dispatch(
        jenkinsIntegrationsList(false, false, { filter: { ...available_integration_filter } }, AVAILABLE_INTEGRATIONS)
      );
    }
  }, [updateIntegrationState]);

  useEffect(() => {
    if (jenkinsIntegrationsLoading) {
      const loading = get(jenkinsIntegrations, "loading", true);
      const error = get(jenkinsIntegrations, "error", true);
      if (!loading && !error) {
        const hasNext = get(jenkinsIntegrations, ["data", "_metadata", "has_next"], false);
        setHasNextJenkinsIntegrations(hasNext);
        setJenkinsIntegrationsLoading(false);
      }
    }
  }, [jenkinsIntegrations]);

  useEffect(() => {
    if (availableIntegrationsLoading || polling) {
      const loading = get(availableIntegrations, "loading", true);
      const error = get(availableIntegrations, "error", true);
      if (!loading && !error) {
        const hasNext = get(availableIntegrations, ["data", "_metadata", "has_next"], false);
        setHasNextAvailableIntegrations(hasNext);
        if (polling) {
          setPolling(false);
        } else {
          setAvailableIntegrationsLoading(false);
        }
      }
    }
  }, [availableIntegrations]);

  useEffect(() => {
    if (jenkinsIntegrationsLoading || availableIntegrationsLoading) {
      return;
    }

    let count = 0;
    const _filteredData: string[] = jenkinsIntegrationList;
    const _availableData: string[] = availableIntegrationList;

    setDetachingInstanceId("");
    setAttachingInstanceId("");

    _filteredData.forEach((instance: any) => {
      if (getInstanceStatus(instance)?.toLocaleLowerCase() === "down") {
        count = count + 1;
      }
    });

    setAvailableData(_availableData);
    setFilteredData(_filteredData);
    setIncidentHealthCheckCount(count);
  }, [jenkinsIntegrationsLoading, availableIntegrationsLoading, availableIntegrationList, jenkinsIntegrationList]);

  const onAttach = (instance: any) => {
    setAttachingInstanceId(instance.id);
    onIntegrationChange({
      add: [instance.id],
      remove: []
    });
  };

  const onDetach = (instance: any) => {
    setDetachingInstanceId(instance.id);
    onIntegrationChange({
      add: [],
      remove: [instance.id]
    });
  };

  const handleJenkinsIntegrationScroll = (event: any) => {
    let maxScroll = event.target.scrollHeight - event.target.clientHeight;
    let currentScroll = event.target.scrollTop;

    if (currentScroll >= maxScroll - 100 && hasNextJenkinsIntegration && !jenkinsIntegrationsLoading) {
      setJenkinsIntegrationsLoading(true);
      getJenkinsIntegrations(true);
    }
  };

  const handleAvailableIntegrationsScroll = (event: any) => {
    let maxScroll = event.target.scrollHeight - event.target.clientHeight;
    let currentScroll = event.target.scrollTop;

    if (currentScroll >= maxScroll - 100 && hasNextAvailableIntegration && !availableIntegrationsLoading) {
      setAvailableIntegrationsLoading(true);
      getAvailableIntegrations(false, true);
    }
  };

  return (
    <div className="flex jenkins-node">
      <section onScroll={handleJenkinsIntegrationScroll} className="jenkins-node__jenkins-section">
        <div className="heading">
          <AntText className="heading__title">Jenkins Instances</AntText>
          <AntText className="heading__count">{filteredData.length}</AntText>
        </div>
        {jenkinsIntegrationsLoading && !hasNextJenkinsIntegration && <Loader />}
        {(!jenkinsIntegrationsLoading || hasNextJenkinsIntegration) && (
          <>
            {filteredData.length > 0 ? (
              incidentHealthCheckCount === 0 ? (
                <Alert
                  message={`All ${filteredData.length} Jenkins instances are operational.`}
                  type="success"
                  showIcon
                />
              ) : (
                <Alert
                  message={`${incidentHealthCheckCount} out of ${filteredData.length} instances are down.`}
                  type="error"
                  showIcon
                />
              )
            ) : (
              ""
            )}
            <div className="attached-integrations">
              {filteredData.map((instance: any) => (
                <InstanceCard
                  key={instance.id}
                  instance={instance}
                  attached={true}
                  onAttach={onAttach}
                  onDetach={onDetach}
                  detaching={instance.id === detachingInstanceId}
                  disabled={!!detachingInstanceId || !!attachingInstanceId}
                />
              ))}
            </div>
          </>
        )}
        {!jenkinsIntegrationsLoading && filteredData.length === 0 && (
          <div className="no-data-found">
            No Jenkins Instances found, please add Instance(s) from "Available Instances" section.
          </div>
        )}
        {jenkinsIntegrationsLoading && hasNextJenkinsIntegration && <Loader />}
      </section>
      <section
        onScroll={handleAvailableIntegrationsScroll}
        className="jenkins-node__available-section"
        style={{ paddingRight: addInstance ? "1rem" : "", borderRight: addInstance ? "1px solid #d9d9d9" : "" }}>
        <div className="flex heading">
          <AntText className="heading__title">Available Instances</AntText>
          <div>
            <AntText className="heading__count">{availableData.length}</AntText>
          </div>
          <div style={{ flex: 1 }} />
          <AntButton type="link" style={{ height: "22px" }} onClick={() => setAddInstance(true)}>
            + Add Instance
          </AntButton>
        </div>
        {availableIntegrationsLoading && !hasNextAvailableIntegration && <Loader />}
        {(!availableIntegrationsLoading || hasNextAvailableIntegration) &&
          availableData.map((instance: any) => (
            <InstanceCard
              key={instance.id}
              instance={instance}
              className="mb-10"
              attached={false}
              onAttach={onAttach}
              onDetach={onDetach}
              attaching={instance.id === attachingInstanceId}
              disabled={!!detachingInstanceId || !!attachingInstanceId}
            />
          ))}
        {!availableIntegrationsLoading && availableData.length === 0 && (
          <div className="no-data-found">
            No Available Instances found, please add Instance(s) using "+ Add Instance" button above.
          </div>
        )}
        {availableIntegrationsLoading && hasNextAvailableIntegration && <Loader />}
      </section>
      {addInstance && (
        <section className="jenkins-node__add-section">
          <PluginApi show={addInstance} setShow={setAddInstance} integrationName={integrationName} />
        </section>
      )}
    </div>
  );
};

export default JenkinNodes;
