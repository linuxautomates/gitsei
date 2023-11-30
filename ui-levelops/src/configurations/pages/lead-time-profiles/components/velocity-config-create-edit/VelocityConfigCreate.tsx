import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";

import { notification } from "antd";
import { debounce, get } from "lodash";
import { useDispatch, useSelector } from "react-redux";

import { RestVelocityConfigs, RestVelocityConfigStage, TriggerEventType } from "classes/RestVelocityConfigs";
import { useHeader } from "custom-hooks/useHeader";
import Loader from "components/Loader/Loader";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { clearPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import {
  velocityConfigsBasicTemplateGet,
  velocityConfigsCreate,
  velocityConfigsList
} from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  VELOCITY_CONFIG_BASIC_TEMPLATE,
  VELOCITY_CONFIG_SEARCH_ID,
  VELOCITY_CONFIGS,
  velocityConfigsBaseTemplateSelector,
  velocityConfigsRestGetSelector
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { CANCEL_ACTION_KEY, getActionButtons, SAVE_ACTION_KEY, tarnsformFilterData } from "../../helpers/helpers";
import { useBreadcumsForVelocitySchemePage } from "../velocity-config-list/helper/useBreadcumsForVelocitySchemePage";
import VelocityConfig from "./VelocityConfig";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, WorkflowProfileAnalyticsActions } from "dataTracking/analytics.constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { trackingStage } from "../../helpers/helpers";
import { JIRA_RELEASE_SAVE_DISABLED_MESSAGE, STAGE_TYPE } from "../../helpers/constants";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import LeavePageBlocker from "configurations/pages/Organization/User/container/UserListPage/LeavePageBlocker";
import UnsavedChangesWarningModal from "../../containers/workflowDetails/components/warning-modal/UnsavedChangesWarning";
import { IssueManagementOptions } from "constants/issueManagementOptions";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

const VelocityConfigCreateComponent: React.FC = props => {
  const history = useHistory();
  const location = useLocation();

  const dispatch = useDispatch();

  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [searching, setSearching] = useState(false);
  const [nameExist, setNameExist] = useState(false);

  const { setupHeader, changeButtonState, onActionButtonClick } = useHeader(location.pathname);

  const restConfig: RestVelocityConfigs = useParamSelector(velocityConfigsRestGetSelector, {
    config_id: "new"
  });
  const changesSelector = useSelector(unSavedChangesSelector);
  const breadCrumbs = useBreadcumsForVelocitySchemePage(restConfig.id ? "Edit Profile" : "Add Profile");

  const restCreateState = useParamSelector(getGenericRestAPISelector, {
    uri: VELOCITY_CONFIGS,
    method: "create",
    uuid: "new"
  });

  const restBasicTemplateState = useParamSelector(velocityConfigsBaseTemplateSelector, {
    id: VELOCITY_CONFIG_BASIC_TEMPLATE
  });

  const configsCheckListState = useParamSelector(getGenericRestAPISelector, {
    uri: VELOCITY_CONFIGS,
    method: "list",
    uuid: VELOCITY_CONFIG_SEARCH_ID
  });
  const entWorkflowProfile = useHasEntitlements(Entitlement.SETTING_WORKFLOW);
  const entWorkflowProfileCountExceed = useHasEntitlements(
    Entitlement.SETTING_WORKFLOW_PROFILE_COUNT_3,
    EntitlementCheckType.AND
  );

  const setWarningModalSheet = useCallback((showModal: boolean) => {
    dispatch(
      setUnSavedChanges({
        ...changesSelector,
        show_modal: showModal
      })
    );
  }, []);
  const markDirty = useCallback((setDirty: boolean) => {
    if (setDirty) {
      dispatch(
        setUnSavedChanges({
          show_modal: false,
          dirty: true
        })
      );
    }
  }, []);

  useEffect(() => {
    dispatch(velocityConfigsBasicTemplateGet(VELOCITY_CONFIG_BASIC_TEMPLATE));
    setLoading(true);
    return () => {
      dispatch(restapiClear(VELOCITY_CONFIGS, "list", -1));
      dispatch(restapiClear(VELOCITY_CONFIGS, "create", -1));
      dispatch(restapiClear(VELOCITY_CONFIGS, "get", -1));
      dispatch(clearPageSettings(location.pathname));
    };
  }, []);

  useEffect(() => {
    if (loading) {
      const loading = get(restBasicTemplateState, ["loading"], true);
      const error = get(restBasicTemplateState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(restBasicTemplateState, ["data"], {});
          dispatch(genericRestAPISet(data, VELOCITY_CONFIGS, "get", "new"));
        }
        setLoading(false);
      }
    }
  }, [restBasicTemplateState]);

  useEffect(() => {
    if (searching) {
      const loading = get(configsCheckListState, ["loading"], true);
      const error = get(configsCheckListState, ["error"], true);
      if (!loading && !error) {
        const records = get(configsCheckListState, ["data", "records"], []);
        const resNameExists =
          !!records?.filter((item: any) => item?.name?.toLowerCase() === restConfig?.name?.trim().toLowerCase())
            ?.length || false;
        setNameExist(resNameExists);
        setSearching(false);
      }
    }
  }, [configsCheckListState]);

  const [hasCreateAccess] = useConfigScreenPermissions();

  useEffect(() => {
    const hasAccess = window.isStandaloneApp ? true : hasCreateAccess;
    setupHeader({
      title: "Create Workflow Profile",
      action_buttons: getActionButtons(
        !hasAccess || !restConfig?.validate || creating || nameExist || !entWorkflowProfile,
        !restConfig?.validate && restConfig.jira_only
          ? JIRA_RELEASE_SAVE_DISABLED_MESSAGE
          : !entWorkflowProfile || entWorkflowProfileCountExceed
          ? TOOLTIP_ACTION_NOT_ALLOWED
          : ""
      ),
      bread_crumbs: breadCrumbs,
      bread_crumbs_position: "before",
      withBackButton: true
    });
  }, [restConfig, nameExist]);

  const anyErrorInDef = useMemo(() => {
    let error = false;
    const { scm_config } = restConfig.postData;
    Object.keys(scm_config).forEach(key => {
      if (new DORAConfigDefinition(scm_config[key]).hasError) {
        error = true;
      }
    });
    return error;
  }, [restConfig]);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          if (changesSelector.dirty) {
            setWarningModalSheet(true);
          } else {
            history.goBack();
          }
          return {
            hasClicked: false
          };
        case SAVE_ACTION_KEY:
          if (anyErrorInDef) {
            notification.error({ message: "Please resolve any errors before continuing" });
            return {
              hasClicked: false
            };
          }
          setCreating(true);
          convertPostData(restConfig);
          dispatch(velocityConfigsCreate(restConfig, "new"));
          // GA EVENT
          emitEvent(AnalyticsCategoryType.WORKFLOW_PROFILES, WorkflowProfileAnalyticsActions.ADD_PROFILE);
          const currentPreStages = get(restConfig, [STAGE_TYPE.preDevCustomStages], []);
          const currentPostStages = get(restConfig, [STAGE_TYPE.postDevCustomStages], []);
          trackingStage({
            originalPreStages: [],
            originalPostStages: [],
            currentPreStages,
            currentPostStages
          });
          return {
            hasClicked: false,
            disabled: true,
            showProgress: true
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  useEffect(() => {
    if (creating) {
      const loading = get(restCreateState, ["loading"], true);
      const error = get(restCreateState, ["error"], true);

      if (!loading) {
        if (!error) {
          notification.success({ message: "Workflow Profile Created Successfully" });
          history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
        }
        setCreating(false);
      }
    }
  }, [restCreateState]);

  const debouncedSearch = useCallback(
    debounce((name: string, restConfig: RestVelocityConfigs) => {
      const filters = {
        filter: {
          partial: {
            name: {
              $begins: name
            }
          }
        }
      };
      dispatch(velocityConfigsList(filters, VELOCITY_CONFIG_SEARCH_ID));
      dispatch(genericRestAPISet(restConfig?.json, VELOCITY_CONFIGS, "get", "new"));
      setSearching(true);
    }, 300),
    []
  );

  const removeStageDefinitions = useCallback(
    (value: string) => {
      if (restConfig.post_development_custom_stages?.length) {
        restConfig.post_development_custom_stages = restConfig.post_development_custom_stages.map(stage => {
          return {
            ...stage.json,
            event: {
              type:
                value[0] === IssueManagementOptions.JIRA
                  ? TriggerEventType.JIRA_STATUS
                  : TriggerEventType.WORKITEM_STATUS,
              values: [],
              params: undefined
            } as any
          } as RestVelocityConfigStage;
        });
      }
      if (restConfig.pre_development_custom_stages?.length) {
        restConfig.pre_development_custom_stages = restConfig.pre_development_custom_stages.map(stage => {
          return {
            ...stage.json,
            event: {
              type:
                value[0] === IssueManagementOptions.JIRA
                  ? TriggerEventType.JIRA_STATUS
                  : TriggerEventType.WORKITEM_STATUS,
              values: [],
              params: undefined
            } as any
          } as RestVelocityConfigStage;
        });
      }
    },
    [restConfig]
  );

  const handleFieldChange = useCallback(
    (key: string, value: any, isDirty: boolean = true) => {
      markDirty(isDirty);
      if (key === "fixed_stages" && value === null) {
        // @ts-ignore
        restConfig[key] = value;
        restConfig["starting_event_is_commit_created"] = false;
      } else if (key === "issue_management_integrations") {
        removeStageDefinitions(value);
        restConfig[key] = value;
      } else if (key === "jira_only") {
        restConfig[key] = value;
        if (value) {
          restConfig["fixed_stages_enabled"] = false;
          let newStages = new RestVelocityConfigStage(null, TriggerEventType.JIRA_RELEASE);
          handleFieldChange("pre_development_custom_stages", [newStages.json], false);
        } else {
          restConfig["fixed_stages_enabled"] = true;
          restConfig["pre_development_custom_stages"] = [];
        }
      } else {
        // @ts-ignore
        restConfig[key] = value;
      }

      if (key === "name") {
        debouncedSearch(value as string, restConfig);
      } else {
        dispatch(genericRestAPISet(restConfig?.json, VELOCITY_CONFIGS, "get", "new"));
      }
    },
    [restConfig, removeStageDefinitions]
  );

  const convertPostData = (velocityProfile: RestVelocityConfigs) => {
    let finalData = velocityProfile.postData;

    let preDevelopmentCustomStage = tarnsformFilterData(get(finalData, ["pre_development_custom_stages"], []));
    let postDevelopmentCustomStage = finalData?.jira_only
      ? []
      : tarnsformFilterData(get(finalData, ["post_development_custom_stages"], []));

    handleFieldChange("pre_development_custom_stages", preDevelopmentCustomStage, false);
    handleFieldChange("post_development_custom_stages", postDevelopmentCustomStage, false);
    if (finalData.jira_only) {
      handleFieldChange("release", {}, false);
      handleFieldChange("deployment", {}, false);
      handleFieldChange("hotfix", {}, false);
      handleFieldChange("defect", {}, false);
    }
  };

  const renderLeavePagePopup = useMemo(
    () => (
      <>
        <LeavePageBlocker
          when={changesSelector?.dirty || false}
          path={`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}`}
        />
        <UnsavedChangesWarningModal
          visibility={changesSelector.show_modal}
          setVisibility={setWarningModalSheet}
          handleClickProceedButton={changesSelector.onCancel}
        />
      </>
    ),
    [changesSelector, setWarningModalSheet]
  );
  if (creating || loading) return <Loader />;

  return (
    <>
      <VelocityConfig config={restConfig} onChange={handleFieldChange} nameExist={nameExist} />
      {renderLeavePagePopup}
    </>
  );
};

export default VelocityConfigCreateComponent;
