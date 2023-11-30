import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useHeader } from "../../../../../../custom-hooks/useHeader";
import { useHistory, useLocation } from "react-router-dom";
import { CALCULATION_RELEASED_IN_KEY, EVENT_JOB_SELECTION_ERROR_MESSAGE, WORKFLOW_PROFILE_MENU } from "./constant";
import "./workflowDetailsPage.scss";
import WorkflowProfileMenu from "./workflowMenu";
import {
  CANCEL_ACTION_KEY,
  checkValueExistsForFilter,
  getActionButtons,
  SAVE_ACTION_KEY
} from "../../../helpers/helpers";
import { useBreadcumsForVelocitySchemePage } from "../../../components/velocity-config-list/helper/useBreadcumsForVelocitySchemePage";
import ConfigurationInfo from "./configuration-tab/configurationInfoPage";
import {
  RestStageConfig,
  RestWorkflowProfile,
  TriggerEventType,
  WorkflowIntegrationType
} from "classes/RestWorkflowProfile";
import WorkflowAssociationsLandingPage from "./associations-tab/WorkflowAssociationLandingPage";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { velocityConfigsListSelector, VELOCITY_CONFIG_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { get } from "lodash";
import DeploymentFrequency from "./deploymentFrequency/DeploymentFrequency";
import ChangeFailureRatePage from "./change-failure-rate/ChangeFailureRatePage";
import { useDispatch, useSelector } from "react-redux";
import {
  createWorkflowProfileAction,
  updateWorkflowProfileAction
} from "reduxConfigs/actions/restapi/workFlowNewAction";
import { notification } from "antd";
import LeadTimeForChanges from "./leadTimeForChanges/LeadTimeForChanges";
import { WorkflowSavingState } from "reduxConfigs/reducers/workflowProfileReducer";
import { workflowProfileSavingStatusSelector } from "reduxConfigs/selectors/workflowProfileSelectors";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { orgUnitListDataState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { ORG_UNIT_LIST_ID } from "configurations/pages/Organization/Constants";
import { workflowProfileClearAction } from "reduxConfigs/actions/restapi/workflowProfileByOuAction";
import Loader from "components/Loader/Loader";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import UnsavedChangesWarningModal from "./warning-modal/UnsavedChangesWarning";
import LeavePageBlocker from "configurations/pages/Organization/User/container/UserListPage/LeavePageBlocker";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface WorkflowProfileDetailsPageProps {
  configId: string;
  defaultWorkflowProfile: RestWorkflowProfile;
  tabComponent: WORKFLOW_PROFILE_MENU;
}

const WorkflowProfileDetailsPage: React.FC<WorkflowProfileDetailsPageProps> = ({
  configId,
  defaultWorkflowProfile,
  tabComponent
}) => {
  const location = useLocation();
  const history = useHistory();
  const dispatch = useDispatch();

  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);

  const [selectedMenu, setSelectedMenu] = useState<WORKFLOW_PROFILE_MENU>(tabComponent);
  const [workflowProfile, setWorkflowProfile] = useState<RestWorkflowProfile>(defaultWorkflowProfile);
  const [profilesList, setProfilesList] = useState<Array<any>>([]);
  const [exclamationFlag, setExclamationFlag] = useState<boolean>(false);
  const [updating, setUpdating] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  useEffect(() => {
    refreshing && setRefreshing(false);
  }, [refreshing]);

  const workflowProfileListState = useParamSelector(velocityConfigsListSelector, {
    id: VELOCITY_CONFIG_LIST_ID
  });
  const changesSelector = useSelector(unSavedChangesSelector);
  const profileSavingState: WorkflowSavingState = useSelector(workflowProfileSavingStatusSelector);
  const orgUnitListState = useParamSelector(orgUnitListDataState, { id: ORG_UNIT_LIST_ID });

  const breadCrumbs = useBreadcumsForVelocitySchemePage(configId === "new" ? "Add Profile" : "Edit Profile");

  useEffect(() => {
    if (!workflowProfileListState.isLoading && !workflowProfileListState.error) {
      const records = get(workflowProfileListState, ["data", "records"], []);
      setProfilesList(records);
    }
  }, [workflowProfileListState]);

  const isInvalidPostStage = (stages: RestStageConfig[] | undefined) => {
    if (!stages) return false;
    let isInvalidPostStages = false;
    let otherCicdJobs: string[] | undefined = undefined;
    (stages || []).forEach(stage => {
      if (stage.event?.type === TriggerEventType.CICD_JOB_RUN) {
        if (!otherCicdJobs) {
          otherCicdJobs = stage.event.values;
        } else if (configId === "new" && stage.event.values.some((job: string) => otherCicdJobs?.includes(job))) {
          isInvalidPostStages = true;
        }
      }
      if (configId === "new" && !stage.event?.values?.length) {
        isInvalidPostStages = true;
      }
    });
    return isInvalidPostStages;
  };

  const isValidLeadTime = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      const allStagesForLeadTimeForFixedStages = workflowProfile?.lead_time_for_changes?.fixed_stages_enabled
        ? workflowProfile?.lead_time_for_changes?.fixed_stages
        : [];
      if (isInvalidPostStage(workflowProfile?.lead_time_for_changes?.post_development_custom_stages)) return true;
      const allStagesForLeadTime = [
        ...(allStagesForLeadTimeForFixedStages || []),
        ...(workflowProfile?.lead_time_for_changes?.pre_development_custom_stages || []),
        ...(workflowProfile?.lead_time_for_changes?.post_development_custom_stages || [])
      ];
      const resultForLeadTime = allStagesForLeadTime.some((stage: RestStageConfig) => stage.enabled);
      if (!resultForLeadTime) {
        return true;
      }
      const allCustomStagesForLeadTime = [
        ...(workflowProfile?.lead_time_for_changes?.pre_development_custom_stages || []),
        ...(workflowProfile?.lead_time_for_changes?.post_development_custom_stages || [])
      ];
      if (allCustomStagesForLeadTime) {
        const resultForLeadTimeTest = allCustomStagesForLeadTime.reduce((acc: any, defaultData: any) => {
          if (configId === "new" && (!defaultData.event.values || defaultData.event.values <= 0)) {
            acc.push(true);
          }
          return acc;
        }, []);
        if (resultForLeadTimeTest && resultForLeadTimeTest.length > 0) {
          return true;
        }
      }
      return false;
    },
    []
  );

  const isValidMTTR = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      const allStagesForMeanTimeForFixedStages = workflowProfile?.mean_time_to_restore?.fixed_stages_enabled
        ? workflowProfile?.mean_time_to_restore?.fixed_stages
        : [];
      const allStagesForMeanTime = [
        ...(allStagesForMeanTimeForFixedStages || []),
        ...(workflowProfile?.mean_time_to_restore?.pre_development_custom_stages || []),
        ...(workflowProfile?.mean_time_to_restore?.post_development_custom_stages || [])
      ];

      if (isInvalidPostStage(workflowProfile?.mean_time_to_restore?.post_development_custom_stages)) return true;
      const resultForMeanTime = allStagesForMeanTime.some((stage: RestStageConfig) => stage.enabled);
      if (!resultForMeanTime) {
        return true;
      }
      const allCustomStagesForLeadTime = [
        ...(workflowProfile?.lead_time_for_changes?.pre_development_custom_stages || []),
        ...(workflowProfile?.lead_time_for_changes?.post_development_custom_stages || [])
      ];
      if (allCustomStagesForLeadTime) {
        const resultForLeadTimeTest = allCustomStagesForLeadTime.reduce((acc: any, defaultData: any) => {
          if (configId === "new" && (!defaultData.event.values || defaultData.event.values <= 0)) {
            acc.push(true);
          }
          return acc;
        }, []);
        if (resultForLeadTimeTest && resultForLeadTimeTest.length > 0) {
          return true;
        }
      }
      return false;
    },
    []
  );

  const isValidDeploymentFrequency = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      let isDisabled = false;
      const filtersForDeploymentFrequency = workflowProfile?.deployment_frequency?._filters?.deployment_frequency;
      if (!workflowProfile?.deployment_frequency?.integration_id) return true;
      switch (filtersForDeploymentFrequency.integration_type) {
        case WorkflowIntegrationType.IM:
          if (
            workflowProfile?.deployment_frequency?.application === IntegrationTypes.JIRA &&
            filtersForDeploymentFrequency?.calculation_field === CALCULATION_RELEASED_IN_KEY
          ) {
            return isDisabled;
          }
          if (!filtersForDeploymentFrequency.filter.some(filterItem => checkValueExistsForFilter(filterItem))) {
            return true;
          }
          break;
        case WorkflowIntegrationType.SCM:
          if (filtersForDeploymentFrequency?.scm_filters.hasError) {
            return true;
          }
          break;
        case WorkflowIntegrationType.CICD:
          if (
            configId === "new" &&
            (!filtersForDeploymentFrequency?.event?.values || filtersForDeploymentFrequency?.event?.values?.length < 1)
          ) {
            return true;
          }
          if (
            filtersForDeploymentFrequency.hasOwnProperty("is_ci_job") &&
            typeof filtersForDeploymentFrequency.is_ci_job !== "undefined" &&
            !filtersForDeploymentFrequency.is_ci_job &&
            filtersForDeploymentFrequency.hasOwnProperty("is_cd_job") &&
            typeof filtersForDeploymentFrequency.is_cd_job !== "undefined" &&
            !filtersForDeploymentFrequency.is_cd_job
          ) {
            return true;
          }
          break;
      }
      return isDisabled;
    },
    []
  );

  const isValidChangeFailureRate = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      let isDisabled = false;
      const filtersForChangeFailureRate = workflowProfile?.change_failure_rate?._filters;
      if (!workflowProfile?.change_failure_rate?.integration_id) return true;

      switch (filtersForChangeFailureRate?.failed_deployment?.integration_type) {
        case WorkflowIntegrationType.IM:
          if (
            !filtersForChangeFailureRate.failed_deployment.filter?.some(filterItem =>
              checkValueExistsForFilter(filterItem)
            )
          ) {
            return true;
          }
          break;
        case WorkflowIntegrationType.SCM:
          if (filtersForChangeFailureRate.failed_deployment.scm_filters?.hasError) {
            return true;
          }
          break;
        case WorkflowIntegrationType.CICD:
          if (
            filtersForChangeFailureRate.failed_deployment.integration_type === WorkflowIntegrationType.CICD &&
            filtersForChangeFailureRate.total_deployment.integration_type === WorkflowIntegrationType.CICD &&
            filtersForChangeFailureRate.failed_deployment?.event?.selectedJob !==
              filtersForChangeFailureRate.total_deployment?.event?.selectedJob
          ) {
            return true;
          }

          if (
            configId === "new" &&
            (!filtersForChangeFailureRate.failed_deployment.event?.values ||
              filtersForChangeFailureRate.failed_deployment.event?.values?.length < 1)
          ) {
            return true;
          }
          let { is_ci_job, is_cd_job } = filtersForChangeFailureRate.failed_deployment;
          if (
            filtersForChangeFailureRate.failed_deployment.hasOwnProperty("is_ci_job") &&
            typeof is_ci_job !== "undefined" &&
            !is_ci_job &&
            filtersForChangeFailureRate.failed_deployment.hasOwnProperty("is_cd_job") &&
            typeof is_cd_job !== "undefined" &&
            !is_cd_job
          ) {
            return true;
          }
          break;
      }

      if (!workflowProfile?.change_failure_rate?.is_absolute) {
        switch (filtersForChangeFailureRate?.total_deployment?.integration_type) {
          case WorkflowIntegrationType.IM:
            if (
              !filtersForChangeFailureRate.total_deployment.filter?.some(filterItem =>
                checkValueExistsForFilter(filterItem)
              )
            ) {
              return true;
            }
            break;
          case WorkflowIntegrationType.SCM:
            if (filtersForChangeFailureRate.total_deployment.scm_filters?.hasError) {
              return true;
            }
            break;
          case WorkflowIntegrationType.CICD:
            if (
              filtersForChangeFailureRate.failed_deployment.integration_type === WorkflowIntegrationType.CICD &&
              filtersForChangeFailureRate.total_deployment.integration_type === WorkflowIntegrationType.CICD &&
              filtersForChangeFailureRate.failed_deployment?.event?.selectedJob !==
                filtersForChangeFailureRate.total_deployment?.event?.selectedJob
            ) {
              return true;
            }
            if (
              configId === "new" &&
              (!filtersForChangeFailureRate.total_deployment.event?.values ||
                filtersForChangeFailureRate.total_deployment.event?.values?.length < 1)
            ) {
              return true;
            }
            let { is_ci_job, is_cd_job } = filtersForChangeFailureRate.total_deployment;

            if (
              filtersForChangeFailureRate.total_deployment.hasOwnProperty("is_ci_job") &&
              typeof is_ci_job !== "undefined" &&
              !is_ci_job &&
              filtersForChangeFailureRate.total_deployment.hasOwnProperty("is_cd_job") &&
              typeof is_cd_job !== "undefined" &&
              !is_cd_job
            ) {
              return true;
            }
            break;
        }
      }
      return isDisabled;
    },
    []
  );

  const isValidWorkflowProfile = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      if (
        workflowProfile?.name === undefined ||
        workflowProfile?.name?.trim() === "" ||
        !workflowProfile?.isValidName ||
        workflowProfile?.name?.length > 75
      ) {
        return true;
      }
      let result =
        isValidDeploymentFrequency(workflowProfile) ||
        isValidChangeFailureRate(workflowProfile) ||
        isValidLeadTime(workflowProfile) ||
        isValidMTTR(workflowProfile);

      return result;
    },
    [isValidChangeFailureRate, isValidDeploymentFrequency, isValidLeadTime, isValidMTTR]
  );

  const getErrorMessage = useMemo(
    () => (workflowProfile: RestWorkflowProfile) => {
      const filtersForChangeFailureRate: any = workflowProfile?.change_failure_rate?._filters;
      if (
        filtersForChangeFailureRate.failed_deployment?.event?.selectedJob !==
        filtersForChangeFailureRate.total_deployment?.event?.selectedJob
      ) {
        return EVENT_JOB_SELECTION_ERROR_MESSAGE;
      }
      return "";
    },
    []
  );

  const [createAccess, editAccess] = useConfigScreenPermissions();

  useEffect(() => {
    const oldReadonly = getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY);
    const hasSaveAccess = window.isStandaloneApp ? !oldReadonly : configId === "new" ? createAccess : editAccess;
    setupHeader({
      action_buttons: workflowProfile.hide_cancel_save_button
        ? {}
        : {
            action_cancel: {
              type: "secondary",
              label: "Cancel",
              hasClicked: false,
              disabled: false,
              showProgress: false
            },
            ...getActionButtons(
              !hasSaveAccess || isValidWorkflowProfile(workflowProfile),
              getErrorMessage(workflowProfile)
            )
          },
      showFullScreenBottomSeparator: true,
      title: workflowProfile.name || "New Profile",
      description: workflowProfile.description || "",
      bread_crumbs: breadCrumbs,
      bread_crumbs_position: "before",
      withBackButton: true
    });
  }, [
    configId,
    workflowProfile.name,
    workflowProfile.description,
    workflowProfile.hide_cancel_save_button,
    workflowProfile
  ]);

  const setWarningModalSheet = useCallback((showModal: boolean) => {
    dispatch(
      setUnSavedChanges({
        ...changesSelector,
        show_modal: showModal,
        onCancel: () => {
          dispatch(setUnSavedChanges({ show_modal: false, dirty: false, onCancel: "" }));
          history.goBack();
        }
      })
    );
  }, []);

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
          if (workflowProfile.validate) {
            dispatch(
              setUnSavedChanges({
                show_modal: false,
                dirty: false
              })
            );
            if (workflowProfile.id) {
              dispatch(updateWorkflowProfileAction(workflowProfile.id!, workflowProfile.postData));
            } else {
              dispatch(createWorkflowProfileAction(workflowProfile.postData));
            }
            dispatch(workflowProfileClearAction());
            setUpdating(true);
            return {
              hasClicked: false,
              disabled: true,
              showProgress: true
            };
          }
          notification.error({ message: "Please check the workflow profile name in configuration tab" });
          return {
            hasClicked: false
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick, workflowProfile, setUpdating]);

  useEffect(() => {
    if (updating) {
      const { saveClicked, isSaving, error } = profileSavingState;
      if (saveClicked && !isSaving) {
        if (!error) {
          notification.success({
            message: `${workflowProfile.name} ${workflowProfile.id ? "Updated" : "created"} Successfully`
          });
          history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
        } else {
          notification.error({
            message: error?.response?.data?.message
              ? error?.response?.data?.message
              : `Failed to ${workflowProfile.id ? "update" : "save"} the profile.`
          });
        }
        setUpdating(false);
      }
    }
  }, [profileSavingState]);

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

  const handleConfigurationInfoChange = useCallback(
    (newValue: any, setDirty: boolean = true) => {
      markDirty(setDirty);
      setWorkflowProfile(
        new RestWorkflowProfile({
          ...workflowProfile.json,
          ...newValue
        })
      );
    },
    [workflowProfile]
  );

  useEffect(() => {
    const loading = get(orgUnitListState, ["loading"], true);
    const error = get(orgUnitListState, ["error"], false);
    if (!loading && !error) {
      const orgRecords = get(orgUnitListState, ["data", "records"], {});
      const workspaceToorg = orgRecords?.reduce((acc: any, org: any) => {
        const workspaceId = `w_${org.workspace_id}`;
        if (acc.hasOwnProperty(workspaceId)) {
          acc[workspaceId] = [...acc[workspaceId], org.id];
        } else {
          acc[workspaceId] = [org.id];
        }
        return acc;
      }, {});

      handleConfigurationInfoChange(
        {
          workspace_to_org: workspaceToorg
        },
        false
      );
    }
  }, [orgUnitListState]);

  // @ts-ignore
  useEffect(() => () => dispatch(restapiClear("organization_unit_management", "list", ORG_UNIT_LIST_ID)), []);

  const menuChangeHandler = useCallback((value: any) => {
    setSelectedMenu(value.key);
    setRefreshing(true);
  }, []);

  const handleExclamationFlag = useCallback(
    (newValue: any) => {
      let ouRefId: Array<string> = [];
      Object.values(workflowProfile.workspace_to_org).forEach((orgList: Array<string>) => {
        ouRefId = [...ouRefId, ...orgList];
      });

      if (ouRefId && ouRefId.length > 0) setExclamationFlag(newValue);
    },
    [setExclamationFlag, workflowProfile]
  );

  const tabContent = useMemo(() => {
    if (refreshing) return <Loader />;
    switch (selectedMenu) {
      case WORKFLOW_PROFILE_MENU.CONFIGURATION:
        return (
          <ConfigurationInfo
            profile={workflowProfile}
            handleChanges={handleConfigurationInfoChange}
            profilesList={profilesList}
          />
        );
      case WORKFLOW_PROFILE_MENU.ASSOCIATIONS:
        return (
          <WorkflowAssociationsLandingPage
            profile={workflowProfile}
            onChange={handleConfigurationInfoChange}
            profilesList={profilesList}
            setExclamationFlag={handleExclamationFlag}
          />
        );
      case WORKFLOW_PROFILE_MENU.LEAD_TIME_FOR_CHANGES:
        return (
          <LeadTimeForChanges
            leadTimeConfig={workflowProfile.lead_time_for_changes}
            onChange={handleConfigurationInfoChange}
            title={WORKFLOW_PROFILE_MENU.LEAD_TIME_FOR_CHANGES}
            description="Lead Time for Changes as per DORA metrics is defined as the amount of time it takes a task to get into production."
            setExclamationFlag={handleExclamationFlag}
          />
        );
      case WORKFLOW_PROFILE_MENU.DEPLOYMENT_FREQUENCY:
        return (
          <DeploymentFrequency
            deploymentFrequencyConfig={workflowProfile.deployment_frequency}
            onChange={handleConfigurationInfoChange}
            setExclamationFlag={handleExclamationFlag}
          />
        );
      case WORKFLOW_PROFILE_MENU.CHANGE_FAILURE_RATE:
        return (
          <ChangeFailureRatePage
            changeFailureRateConfig={workflowProfile.change_failure_rate}
            onChange={handleConfigurationInfoChange}
            setExclamationFlag={handleExclamationFlag}
          />
        );
      case WORKFLOW_PROFILE_MENU.MEAN_TIME_TO_RESTORE:
        return (
          <LeadTimeForChanges
            leadTimeConfig={workflowProfile.mean_time_to_restore}
            onChange={handleConfigurationInfoChange}
            title={WORKFLOW_PROFILE_MENU.MEAN_TIME_TO_RESTORE}
            description="Mean time to restore is a measure of how long it takes a team to recover from a failure in production."
            setExclamationFlag={handleExclamationFlag}
          />
        );
      default:
        return <></>;
    }
  }, [refreshing, selectedMenu, workflowProfile, handleConfigurationInfoChange]);

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

  return (
    <div className="workflow-profile-container">
      <WorkflowProfileMenu onChange={menuChangeHandler} exclamationFlag={exclamationFlag} tabComponent={tabComponent} />
      {tabContent}
      {renderLeavePagePopup}
    </div>
  );
};

export default WorkflowProfileDetailsPage;
