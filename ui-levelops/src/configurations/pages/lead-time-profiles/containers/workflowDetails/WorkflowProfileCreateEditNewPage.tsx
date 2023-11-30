import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import WorkflowProfileDetailsPage from "./components/workflowDetailsPage";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { WorkflowIntegrationType, RestWorkflowProfile } from "classes/RestWorkflowProfile";
import Loader from "components/Loader/Loader";
import { integrationFinder } from "helper/integration.helper";
import "./workflowProfileCreateEditNewPage.scss";
import {
  velocityConfigsBasicTemplateGet,
  velocityConfigsList
} from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import {
  FILTER_VALUES_LIST_ID,
  velocityConfigsBaseTemplateSelector,
  VELOCITY_CONFIG_BASIC_TEMPLATE,
  VELOCITY_CONFIG_LIST_ID
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { useDispatch, useSelector } from "react-redux";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { get, isArray } from "lodash";
import { clearSavedWorkflowProfile, getWorkflowProfileAction } from "reduxConfigs/actions/restapi/workFlowNewAction";
import { WorkflowDetailsState } from "reduxConfigs/reducers/workflowProfileReducer";
import { workflowProfileDetailsSelector } from "reduxConfigs/selectors/workflowProfileSelectors";
import { OrganizationUnitList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { ORG_UNIT_LIST_ID } from "configurations/pages/Organization/Constants";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { EntityIdentifier } from "model/entities/entity";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { Integration } from "model/entities/Integration";
import { WORKFLOW_PROFILE_MENU } from "./components/constant";
import WorkflowProfileIntegrationMissingModal from "./WorkflowProfileIntegrationMissingModal";

const WorkflowProfileCreateEditNewPage: React.FC = props => {
  const [defaultWorkflowProfile, setdefaultWorkflowProfile] = useState<RestWorkflowProfile>();
  const [loadingBasicStages, setLoadingBasicStages] = useState<boolean>(false);
  const [basicStages, setBasicStages] = useState<any>();
  const [defaultIntegrations, setDefaultIntegrations] = useState<any>(undefined);
  const [cicdIntId, setCicdIntId] = useState<EntityIdentifier>();
  const [cicdJobsLoading, setCicdJobsLoading] = useState<boolean>(false);
  const [cicdJobs, setCicdJobs] = useState<string[] | undefined>(undefined);

  const location = useLocation();
  const configId: string = (queryString.parse(location.search).configId as string) || "new";
  const tabComponent: WORKFLOW_PROFILE_MENU =
    (queryString.parse(location.search).tabComponent as WORKFLOW_PROFILE_MENU) || WORKFLOW_PROFILE_MENU.CONFIGURATION;
  const defaultOU: string | undefined = (queryString.parse(location.search).defaultOU as string) || "";
  const queryIntegrations = queryString.parse(location.search).ouIntegrations as string;
  const ouIntegrations = queryIntegrations ? queryIntegrations.split(",") : undefined;

  const { isLoading, filteredIntegrations } = useAllIntegrationState(
    [WorkflowIntegrationType.IM, WorkflowIntegrationType.CICD, WorkflowIntegrationType.SCM],
    "",
    ouIntegrations
  );
  const dispatch = useDispatch();

  const uri = "jenkins_jobs_filter_values";
  const filterKey = "job_normalized_full_name";

  const restBasicTemplateState = useParamSelector(velocityConfigsBaseTemplateSelector, {
    id: VELOCITY_CONFIG_BASIC_TEMPLATE
  });

  const workflowProfileDetailsState: WorkflowDetailsState = useSelector(workflowProfileDetailsSelector);
  const filterValuesState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: FILTER_VALUES_LIST_ID
  });

  useEffect(() => {
    if (configId) {
      dispatch(velocityConfigsBasicTemplateGet(VELOCITY_CONFIG_BASIC_TEMPLATE));
      dispatch(velocityConfigsList({}, VELOCITY_CONFIG_LIST_ID));
      setLoadingBasicStages(true);
    }
  }, [configId, dispatch]);

  useEffect(() => {
    if (loadingBasicStages) {
      const loading = get(restBasicTemplateState, ["loading"], true);
      const error = get(restBasicTemplateState, ["error"], true);
      if (!loading) {
        setLoadingBasicStages(false);
        if (!error) {
          const data = get(restBasicTemplateState, ["data"], {});
          setBasicStages(data);
        }
      }
    }
  }, [restBasicTemplateState, loadingBasicStages]);

  useEffect(() => {
    if (!defaultWorkflowProfile && cicdIntId && !cicdJobsLoading) {
      const filters = {
        fields: [filterKey],
        filter: {
          integration_ids: [cicdIntId]
        },
        integration_ids: [cicdIntId]
      };
      setCicdJobsLoading(true);
      dispatch(genericList(uri, "list", filters, null, FILTER_VALUES_LIST_ID));
      setCicdJobs(undefined);
    }
  }, [cicdIntId, dispatch, cicdJobsLoading, defaultWorkflowProfile]);

  useEffect(() => {
    if (cicdJobsLoading) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], false);
      if (!loading && !error) {
        const path = ["data", "records", "0", filterKey];
        const data = get(filterValuesState, path, []);
        let tempVal = data.map((opt: any) => opt.cicd_job_id) || [];
        setCicdJobs(tempVal);
        setCicdJobsLoading(false);
      }
    }
  }, [filterValuesState, cicdJobsLoading]);

  useEffect(() => {
    if (!defaultWorkflowProfile && configId === "new" && basicStages && defaultIntegrations) {
      if (defaultOU) {
        dispatch(
          OrganizationUnitList(
            {
              filter: {
                ref_id: [defaultOU]
              }
            },
            ORG_UNIT_LIST_ID
          )
        );
      }
      if (cicdIntId) {
        if (!cicdJobsLoading && cicdJobs) {
          const newProfile = new RestWorkflowProfile(
            null,
            defaultIntegrations.CICD || defaultIntegrations.SCM || defaultIntegrations.IM,
            defaultIntegrations.IM || defaultIntegrations.SCM,
            basicStages,
            cicdJobs
          );
          setdefaultWorkflowProfile(newProfile);
        }
      } else {
        const newProfile = new RestWorkflowProfile(
          null,
          defaultIntegrations.CICD || defaultIntegrations.SCM || defaultIntegrations.IM,
          defaultIntegrations.IM || defaultIntegrations.SCM,
          basicStages
        );
        setdefaultWorkflowProfile(newProfile);
      }
    }
  }, [
    defaultIntegrations,
    basicStages,
    isLoading,
    configId,
    cicdJobsLoading,
    cicdJobs,
    cicdIntId,
    defaultWorkflowProfile
  ]);

  useEffect(() => {
    if (
      !defaultWorkflowProfile &&
      configId === "new" &&
      !isLoading &&
      basicStages &&
      filteredIntegrations &&
      filteredIntegrations.length > 0
    ) {
      const intIM = filteredIntegrations.find(integrationFinder(WorkflowIntegrationType.IM));
      const intSCM = filteredIntegrations.find(integrationFinder(WorkflowIntegrationType.SCM));
      const intCICD = filteredIntegrations.find(integrationFinder(WorkflowIntegrationType.CICD));

      setDefaultIntegrations({
        IM: intIM,
        SCM: intSCM,
        CICD: intCICD
      });

      if (intCICD) {
        setCicdIntId(intCICD.id);
      }

      const filteredBasicStages = intIM
        ? basicStages
        : {
            ...basicStages,
            fixed_stages: basicStages.fixed_stages.filter(
              (stage: any) => get(stage, ["event", "type"], "") !== "SCM_COMMIT_CREATED"
            )
          };

      setBasicStages(filteredBasicStages);
    }
  }, [configId, isLoading, filteredIntegrations, basicStages, defaultWorkflowProfile]);

  useEffect(() => {
    if (configId !== "new" && basicStages) {
      dispatch(getWorkflowProfileAction(configId, basicStages));
    }
    return () => {
      dispatch(clearSavedWorkflowProfile());
    };
  }, [configId, basicStages, dispatch]);

  useEffect(() => {
    if (
      configId !== "new" &&
      workflowProfileDetailsState.data &&
      workflowProfileDetailsState.data !== undefined &&
      !isLoading
    ) {
      const data: any = get(workflowProfileDetailsState, ["data"], {});

      if (data.associated_ou_ref_ids?.length > 0) {
        dispatch(
          OrganizationUnitList(
            {
              filter: {
                ref_id: data.associated_ou_ref_ids
              }
            },
            ORG_UNIT_LIST_ID
          )
        );
      } else {
        dispatch(restapiClear("organization_unit_management", "list", ORG_UNIT_LIST_ID));
      }
      const failureRateProfileIntegrationId = get(data, ["change_failure_rate", "integration_id"], undefined);
      let failureRateIntegration = undefined;
      if (failureRateProfileIntegrationId) {
        failureRateIntegration = filteredIntegrations.find(
          (integration: Integration) => integration.id === failureRateProfileIntegrationId
        );
      }
      const workflowProfileData = new RestWorkflowProfile(data, failureRateIntegration);
      setdefaultWorkflowProfile(workflowProfileData);
    }
  }, [workflowProfileDetailsState, configId, dispatch, isLoading]);

  if (!isLoading && (!filteredIntegrations || filteredIntegrations.length <= 0)) {
    return <WorkflowProfileIntegrationMissingModal />;
  }

  if (!defaultWorkflowProfile) {
    return <Loader />;
  }

  return (
    <WorkflowProfileDetailsPage
      configId={configId}
      tabComponent={tabComponent}
      defaultWorkflowProfile={defaultWorkflowProfile}
    />
  );
};

export default WorkflowProfileCreateEditNewPage;
