import React, { useCallback, useMemo } from "react";
import { useHistory } from "react-router-dom";
import { useDispatch } from "react-redux";
import { Empty } from "antd";
import {
  velocityConfigsDelete,
  velocityConfigsSetDefault,
  velocityConfigsClone,
  workflowProfileClone
} from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { getGenericRestAPIStatusSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  velocityConfigsRestListSelector,
  VELOCITY_CONFIG_LIST_ID,
  VELOCITY_CONFIGS
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, WorkflowProfileAnalyticsActions } from "dataTracking/analytics.constants";
import { ProfilesPaginatedTable } from "shared-resources/containers";
import {
  actionColumn,
  categoriesColumn,
  defaultColumn,
  descriptionColumn,
  nameColumn,
  PROFILE_ACTIONS,
  workflowProfileTypeColumn
} from "configurations/pages/ticket-categorization/helper/profiles.helper";
import { WebRoutes } from "routes/WebRoutes";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

const VelocityConfigsPageContent: React.FC = props => {
  const history = useHistory();
  const dispatch = useDispatch();

  const velocityConfigsListState = useParamSelector(velocityConfigsRestListSelector, {
    id: VELOCITY_CONFIG_LIST_ID
  });

  const configStatus = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: VELOCITY_CONFIGS,
    method: "list",
    uuid: VELOCITY_CONFIG_LIST_ID
  });

  const editUrl = WebRoutes.velocity_profile.scheme.edit;

  const findProfileWithID = useCallback(
    (id: string) => {
      if (velocityConfigsListState && Array.isArray(velocityConfigsListState)) {
        return velocityConfigsListState.find((config: any) => config.id === id);
      }
      return null;
    },
    [velocityConfigsListState]
  );

  const handleMenuClick = useCallback(
    (key: string, configId: string) => {
      switch (key) {
        case PROFILE_ACTIONS.EDIT:
          history.push(editUrl(configId));
          break;
        case PROFILE_ACTIONS.DELETE:
          dispatch(velocityConfigsDelete(configId));
          // GA EVENT
          emitEvent(AnalyticsCategoryType.WORKFLOW_PROFILES, WorkflowProfileAnalyticsActions.DELETE_PROFILE);
          break;
        case PROFILE_ACTIONS.CLONE:
          const config = findProfileWithID(configId);
          if (config.is_new) {
            dispatch(workflowProfileClone(configId));
          } else {
            dispatch(velocityConfigsClone(configId));
          }
          // GA EVENT
          emitEvent(AnalyticsCategoryType.WORKFLOW_PROFILES, WorkflowProfileAnalyticsActions.CLONE_PROFILE);
          break;
        case "set-default":
          dispatch(velocityConfigsSetDefault(configId));
          break;
        default:
          return;
      }
    },
    [findProfileWithID]
  );

  const access = useConfigScreenPermissions();

  const oldReadOnly = getRBACPermission(PermeableMetrics.EFFORT_INVESTMENT_READ_ONLY);
  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const actionColumns = useMemo(() => {
    let columns = [PROFILE_ACTIONS.EDIT];
    if (window.isStandaloneApp) {
      if (!getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY)) {
        columns.push(PROFILE_ACTIONS.CLONE, PROFILE_ACTIONS.DELETE);
      }
    } else if (access) {
      access[0] && columns.push(PROFILE_ACTIONS.CLONE);
      access[2] && columns.push(PROFILE_ACTIONS.DELETE);
    }
    return columns;
  }, [access]);

  const columns = useMemo(
    () => [
      nameColumn(velocityConfigsListState, editUrl),
      workflowProfileTypeColumn(),
      descriptionColumn(),
      categoriesColumn("pre_development_custom_stages"),
      defaultColumn("defaultConfig", handleMenuClick, isReadOnly),
      actionColumn(actionColumns, handleMenuClick)
    ],
    [handleMenuClick, velocityConfigsListState]
  );

  if (configStatus.loading === false && !velocityConfigsListState.length) return <Empty />;

  return <ProfilesPaginatedTable dataSource={velocityConfigsListState} columns={columns} />;
};

export default VelocityConfigsPageContent;
