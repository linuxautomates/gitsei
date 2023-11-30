import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { notification } from "antd";
import { debounce, get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { RestVelocityConfigs, RestVelocityConfigStage, TriggerEventType } from "classes/RestVelocityConfigs";
import Loader from "components/Loader/Loader";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { useHeader } from "custom-hooks/useHeader";
import {
  velocityConfigsBasicTemplateGet,
  velocityConfigsGet,
  velocityConfigsList,
  velocityConfigsUpdate
} from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { clearPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import {
  getGenericRestAPISelector,
  getGenericRestAPIStatusSelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import {
  VELOCITY_CONFIGS,
  velocityConfigsRestGetSelector,
  VELOCITY_CONFIG_SEARCH_ID,
  VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE
} from "reduxConfigs/selectors/velocityConfigs.selector";
import {
  getActionButtons,
  SAVE_ACTION_KEY,
  CANCEL_ACTION_KEY,
  tarnsformFilterData,
  tarnsformFilterGetData
} from "../../helpers/helpers";
import { useBreadcumsForVelocitySchemePage } from "../velocity-config-list/helper/useBreadcumsForVelocitySchemePage";
import VelocityConfig from "./VelocityConfig";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { trackingStage } from "../../helpers/helpers";
import { JIRA_RELEASE_SAVE_DISABLED_MESSAGE, STAGE_TYPE } from "../../helpers/constants";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import LeavePageBlocker from "configurations/pages/Organization/User/container/UserListPage/LeavePageBlocker";
import UnsavedChangesWarningModal from "../../containers/workflowDetails/components/warning-modal/UnsavedChangesWarning";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { IssueManagementOptions } from "constants/issueManagementOptions";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

interface VelocityConfigEditComponentProps {
  configId: string;
  stageName?: string;
}

const VelocityConfigEditComponent: React.FC<VelocityConfigEditComponentProps> = props => {
  const { configId, stageName } = props;

  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();

  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [searching, setSearching] = useState(false);
  const [name, setName] = useState<string | undefined>("");
  const [nameExist, setNameExist] = useState(false);
  // for tracking
  const [originalStages, setOriginalStages] = useState<object>({});
  const changesSelector = useSelector(unSavedChangesSelector);

  const restConfig: RestVelocityConfigs = useParamSelector(velocityConfigsRestGetSelector, {
    config_id: configId
  });

  const breadCrumbs = useBreadcumsForVelocitySchemePage(restConfig.id ? "Edit Profile" : "Add Profile");

  const configStatus = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: VELOCITY_CONFIGS,
    method: "get",
    uuid: configId
  });

  const restUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: VELOCITY_CONFIGS,
    method: "update",
    uuid: configId
  });

  const configsCheckListState = useParamSelector(getGenericRestAPISelector, {
    uri: VELOCITY_CONFIGS,
    method: "list",
    uuid: VELOCITY_CONFIG_SEARCH_ID
  });

  let fieldsChanged = "";

  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);
  const entWorkflowProfile = useHasEntitlements(Entitlement.SETTING_WORKFLOW);
  const entWorkflowProfileCountExceed = useHasEntitlements(
    Entitlement.SETTING_WORKFLOW_PROFILE_COUNT_3,
    EntitlementCheckType.AND
  );

  const isHarnessReadonly = useHasConfigReadOnlyPermission();

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
    return () => {
      dispatch(restapiClear(VELOCITY_CONFIGS, "get", -1));
      dispatch(restapiClear(VELOCITY_CONFIGS, "update", -1));
      dispatch(restapiClear(VELOCITY_CONFIGS, "list", -1));
      dispatch(genericRestAPISet({}, VELOCITY_CONFIGS, "get", "-1"));
      dispatch(clearPageSettings(location.pathname));
    };
  }, []);

  useEffect(() => {
    // for tracking
    if (restConfig.id) {
      setOriginalStages({
        [STAGE_TYPE.preDevCustomStages]: get(restConfig, [STAGE_TYPE.preDevCustomStages], []),
        [STAGE_TYPE.postDevCustomStages]: get(restConfig, [STAGE_TYPE.postDevCustomStages], [])
      });
    }
  }, [restConfig.id]);

  useEffect(() => {
    dispatch(velocityConfigsGet(configId));
    setLoading(true);
  }, [configId]);

  useEffect(() => {
    if (searching) {
      const loading = get(configsCheckListState, ["loading"], true);
      const error = get(configsCheckListState, ["error"], true);
      if (!loading && !error) {
        const records = get(configsCheckListState, ["data", "records"], []);
        const resNameExists =
          (name !== restConfig?.name?.trim() &&
            !!records?.filter((item: any) => item?.name?.toLowerCase() === restConfig?.name?.trim().toLowerCase())
              ?.length) ||
          false;
        setNameExist(resNameExists);
        setSearching(false);
      }
    }
  }, [configsCheckListState]);

  useEffect(() => {
    if (loading) {
      if (!configStatus.loading) {
        if (!configStatus.error) {
          setName(restConfig.name);
        }

        if (!restConfig.json.hasOwnProperty("fixed_stages")) {
          dispatch(velocityConfigsBasicTemplateGet(VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE));
        }
        if (
          (restConfig.json.post_development_custom_stages &&
            restConfig.json.post_development_custom_stages.length > 0) ||
          (restConfig.json.pre_development_custom_stages && restConfig.json.pre_development_custom_stages.length > 0) ||
          (!restConfig.json.pre_development_custom_stages && restConfig.jira_only)
        ) {
          convertPostData(restConfig, "get");
        }
        setLoading(false);
      }
    }
  }, [restConfig]);

  useEffect(() => {
    if (restConfig?.id === undefined && configStatus.loading === false) {
      notification.error({ message: "Workflow Profile not found!" });
      history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
    }
    const oldReadOnly = getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY);
    const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;
    const disabled =
      !restConfig?.validate ||
      loading ||
      updating ||
      nameExist ||
      !entWorkflowProfile ||
      entWorkflowProfileCountExceed ||
      isReadOnly;
    setupHeader({
      title: restConfig?.name || "Edit Workflow Profile",
      action_buttons: getActionButtons(
        disabled,
        disabled && restConfig.jira_only
          ? JIRA_RELEASE_SAVE_DISABLED_MESSAGE
          : !entWorkflowProfile || entWorkflowProfileCountExceed
          ? TOOLTIP_ACTION_NOT_ALLOWED
          : ""
      ),
      bread_crumbs: breadCrumbs,
      bread_crumbs_position: "before",
      withBackButton: true
    });
  }, [restConfig, loading, nameExist]);

  useEffect(() => {
    if (updating) {
      const loading = get(restUpdateState, ["loading"], true);
      const error = get(restUpdateState, ["error"], true);
      if (!loading) {
        if (!error) {
          notification.success({ message: "Workflow Profile Updated Successfully" });
          history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
        }
        setUpdating(false);
      }
    }
  }, [restUpdateState]);

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
          setUpdating(true);
          convertPostData(restConfig, "edit");
          dispatch(velocityConfigsUpdate(configId, restConfig));
          const currentPreStages = get(restConfig, [STAGE_TYPE.preDevCustomStages], []);
          const currentPostStages = get(restConfig, [STAGE_TYPE.postDevCustomStages], []);
          trackingStage({
            originalPreStages: (originalStages as any)[STAGE_TYPE.preDevCustomStages],
            originalPostStages: (originalStages as any)[STAGE_TYPE.postDevCustomStages],
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

  const debouncedSearch = useCallback(
    debounce((name: string) => {
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
      fieldsChanged = `${fieldsChanged}, ${key}`;

      if (key === "starting_event_is_commit_created" && !!value) {
        restConfig["starting_event_is_commit_created"] = true;
        restConfig["starting_event_is_generic_event"] = false;
      } else if (key === "starting_event_is_generic_event" && !!value) {
        restConfig["starting_event_is_commit_created"] = false;
        restConfig["starting_event_is_generic_event"] = true;
      } else if (key === "fixed_stages" && value === null) {
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
      dispatch(genericRestAPISet(restConfig?.json, VELOCITY_CONFIGS, "get", configId));
      if (key === "name") {
        debouncedSearch(value as string);
      }
    },
    [restConfig, configId]
  );

  const convertPostData = (velocityProfile: RestVelocityConfigs, type: string) => {
    let finalData = velocityProfile.postData;

    let preDevelopmentCustomStage = get(finalData, ["pre_development_custom_stages"], []);
    let postDevelopmentCustomStage = finalData?.jira_only ? [] : get(finalData, ["post_development_custom_stages"], []);

    let preDevelopmentCustomStageNew =
      type === "edit"
        ? tarnsformFilterData(preDevelopmentCustomStage)
        : tarnsformFilterGetData(preDevelopmentCustomStage, { jiraOnlyFlag: finalData.jira_only });
    let postDevelopmentCustomStageNew =
      type === "edit"
        ? tarnsformFilterData(postDevelopmentCustomStage)
        : tarnsformFilterGetData(postDevelopmentCustomStage, {});

    handleFieldChange("pre_development_custom_stages", preDevelopmentCustomStageNew, false);
    handleFieldChange("post_development_custom_stages", postDevelopmentCustomStageNew, false);
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

  if (loading || updating) return <Loader />;

  return (
    <>
      <VelocityConfig config={restConfig} onChange={handleFieldChange} nameExist={nameExist} stageName={stageName} />
      {renderLeavePagePopup}
    </>
  );
};

export default VelocityConfigEditComponent;
