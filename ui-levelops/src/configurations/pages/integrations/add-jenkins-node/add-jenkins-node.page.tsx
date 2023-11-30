import React, { useEffect, useMemo, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import JenkinNodes from "../../../../shared-resources/components/jenkin-nodes/jenkins-nodes.component";
import { useDispatch, useSelector } from "react-redux";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { get } from "lodash";
import { jenkinsIntegrationsAttach } from "reduxConfigs/actions/restapi";
import { Button, notification } from "antd";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { ADD_TO_WORKSPACE_NOTIFICATION_KEY } from "../constants";
import { WebRoutes } from "routes/WebRoutes";
import { getSettingsPage } from "constants/routePaths";

interface AddJenkinsNodeProps extends RouteComponentProps {}

const AddJenkinsNodes: React.FC<AddJenkinsNodeProps> = props => {
  const dispatch = useDispatch();
  const integrationId = queryString.parse(props.location.search)?.id;
  const pageSettingsState = useSelector(getPageSettingsSelector);
  const [addingNodes, setAddingNodes] = useState(false);
  const [integrations, setIntegrations] = useState<any>({});

  const updateIntegrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "jenkins_integrations",
    method: "update",
    uuid: integrationId
  });

  const integrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "create",
    uuid: "0"
  });
  const newlyCreatedIntegration = get(integrationState, "data", null);

  useEffect(() => {
    if (
      integrationId === undefined ||
      integrationId === null ||
      !newlyCreatedIntegration ||
      (newlyCreatedIntegration && newlyCreatedIntegration.id !== integrationId)
    ) {
      props.history.push(`${getSettingsPage()}/integrations`);
    }

    dispatch(
      setPageSettings(props.location.pathname, {
        title: "Add Nodes",
        action_buttons: {
          done: {
            type: "primary",
            label: "Done",
            hasClicked: false
          }
        }
      })
    );

    return () => {
      dispatch(restapiClear("integrations", "create", "0"));
    };
  }, []);

  const handleRedirectToWorkspace = () => {
    props.history.push(WebRoutes.workspace.root());
    notification.close(ADD_TO_WORKSPACE_NOTIFICATION_KEY);
  };

  const addToWorkspaceButton = useMemo(
    () => (
      <Button type="primary" onClick={handleRedirectToWorkspace}>
        Add this integration to a project
      </Button>
    ),
    []
  );

  useEffect(() => {
    const page = pageSettingsState[props.location.pathname];
    if (page && page.action_buttons && page.action_buttons.done && page.action_buttons.done.hasClicked) {
      if (addingNodes) {
        const _loading = get(updateIntegrationState, "loading", true);
        const error = get(updateIntegrationState, "error", true);
        if (!_loading && !error) {
          let description = "Created Integration Successfully";
          if (integrations?.add.length) {
            description = description + "\n" + "And Added Instance(s)";
          }
          props.history.push(`${getSettingsPage()}/integrations`);
          notification.success({
            message: `Success`,
            description,
            duration: 0,
            key: ADD_TO_WORKSPACE_NOTIFICATION_KEY,
            btn: addToWorkspaceButton
          });
        }
      }
    }
  }, [pageSettingsState]);

  useEffect(() => {
    if (Object.keys(integrations).length) {
      setAddingNodes(true);
      dispatch(
        jenkinsIntegrationsAttach(
          {
            integration_id: integrationId,
            ...integrations
          },
          integrationId
        )
      );
    }
  }, [integrations]);

  return (
    <JenkinNodes
      integrationId={integrationId as string}
      onIntegrationChange={setIntegrations}
      integrationName={newlyCreatedIntegration?.name}
    />
  );
};

export default AddJenkinsNodes;
