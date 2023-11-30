import { sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { integrationParameters } from "configurations/constants";
import {
  DEFAULT_METRIC_IDENTIFIERS,
  DORA_SCM_DEFINITIONS
} from "configurations/pages/lead-time-profiles/helpers/constants";
import { sanitizeObject } from "utils/commonUtils";
import { DORAConfigDefinition } from "./DORAConfigDefinition";
import { v1 as uuid } from "uuid";
import { get, isEmpty } from "lodash";
import { Integration } from "model/entities/Integration";
import { findIntegrationType } from "helper/integration.helper";
import { StageEndOptions } from "./StageEndOptions";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { RestDevelopmentStageConfig, RestStageConfig } from "./workflowProfile/RestWorkflowProfileStageConfig";

export * from "./workflowProfile/RestWorkflowProfileStageConfig";

export enum WorkflowIntegrationType {
  IM = "IM",
  SCM = "SCM",
  CICD = "CICD"
}

export const getDefaultIntegrationFilters = (
  integration_type: WorkflowIntegrationType,
  calculationMode: "deployementFrequency" | "failedDeployment" | "totalDeployment",
  defaultJobs: string[] = [],
  application?: string
): IMFilter | SCMFilter | CICDFilter => {
  switch (integration_type) {
    case WorkflowIntegrationType.IM: {
      switch (application) {
        case IntegrationTypes.JIRA:
          return {
            integration_type,
            calculation_field: "issue_resolved_at",
            filter: [
              {
                key: "status_categories",
                param: integrationParameters.EQUALS,
                value: ["Done"]
              }
            ]
          };
        case IntegrationTypes.AZURE:
          return {
            integration_type,
            calculation_field: "workitem_resolved_at",
            filter: [
              {
                key: "workitem_status_categories",
                param: integrationParameters.EQUALS,
                value: ["Completed"]
              }
            ]
          };
        case IntegrationTypes.AZURE_NON_SPLITTED:
          return {
            integration_type,
            calculation_field: "workitem_resolved_at",
            filter: [
              {
                key: "workitem_status_categories",
                param: integrationParameters.EQUALS,
                value: ["Completed"]
              }
            ]
          };
      }
      return {
        integration_type,
        calculation_field: "issue_resolved_at",
        filter: [
          {
            key: "status_categories",
            param: integrationParameters.EQUALS,
            value: ["Done"]
          }
        ]
      };
    }
    case WorkflowIntegrationType.SCM: {
      let scm_filters = undefined;
      switch (calculationMode) {
        case "deployementFrequency":
          scm_filters = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.release, ["tags", "commit_branch"]);
          break;
        case "totalDeployment":
          scm_filters = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.release, ["tags", "commit_branch"]);
          break;
        case "failedDeployment":
          scm_filters = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.hotfix, ["tags", "commit_branch"]);
          break;
      }
      return {
        integration_type,
        calculation_field: "pr_merged_at",
        deployment_route: "pr",
        deployment_criteria: "pr_merged",
        scm_filters
      };
    }
    case WorkflowIntegrationType.CICD: {
      let event: CICDEvents = {
        type: "CICD_JOB_RUN"
      };
      switch (calculationMode) {
        case "deployementFrequency":
          event = {
            type: "CICD_JOB_RUN",
            values: defaultJobs
          };
          break;
      }
      switch (application) {
        case IntegrationTypes.HARNESSNG:
          let filterData: { key: string; param: integrationParameters; value: string[] }[] = [];
          if (calculationMode === "failedDeployment") {
            filterData = [
              {
                key: "job_statuses",
                param: integrationParameters.EQUALS,
                value: ["Failed", "Aborted", "Expired", "Approval Rejected"]
              }
            ];
          }
          return {
            integration_type,
            event,
            filter: filterData,
            is_ci_job: true,
            is_cd_job: true,
            calculation_field: "start_time"
          };
      }
      return {
        integration_type,
        event,
        filter: [],
        calculation_field: "end_time"
      };
    }
  }
};

export const getDefaultCalculationField = (
  integrationType: WorkflowIntegrationType | undefined,
  application: string | undefined
) => {
  switch (integrationType) {
    case WorkflowIntegrationType.IM:
      if (application === IntegrationTypes.JIRA) {
        return "issue_resolved_at";
      }
      return "workitem_resolved_at";
    case WorkflowIntegrationType.CICD:
      return "end_time";
    default:
      return undefined;
  }
};

export interface IMFilter {
  integration_type: WorkflowIntegrationType.IM;
  calculation_field?: string;
  filter: sectionSelectedFilterType[];
}
export interface SCMFilter {
  integration_type: WorkflowIntegrationType.SCM;
  scm_filters: DORAConfigDefinition;
  calculation_field: "pr_merged_at" | "pr_closed_at" | "commit_pushed_at" | "committed_at" | "tag_added_at";
  deployment_criteria:
    | "pr_merged"
    | "pr_closed"
    | "pr_merged_closed"
    | "commit_merged_to_branch"
    | "commit_with_tag"
    | "commit_merged_to_branch_with_tag";
  deployment_route: "pr" | "commit";
}
export interface CICDEvents {
  params?: any;
  values?: string[];
  type: "CICD_JOB_RUN";
  selectedJob?: string;
}
export interface CICDFilter {
  integration_type: WorkflowIntegrationType.CICD;
  event: CICDEvents;
  filter?: sectionSelectedFilterType[];
  is_ci_job?: boolean;
  is_cd_job?: boolean;
  calculation_field: "end_time" | "start_time";
}

export const getSCMRemoveFilters = (filter: SCMFilter) => {
  if (filter.deployment_route === "pr") {
    return ["tags", "commit_branch"];
  }
  switch (filter.deployment_criteria) {
    case "commit_merged_to_branch_with_tag":
      return ["source_branch", "target_branch", "labels"];
    case "commit_merged_to_branch":
      return ["source_branch", "target_branch", "labels", "tags"];
    case "commit_with_tag":
      return ["source_branch", "commit_branch", "labels", "target_branch"];
  }
  return [];
};

export class RestDeploymentFrequency {
  _integration_id?: string;
  _integration_ids?: string[];
  _application_type: string;
  _filters: {
    deployment_frequency: IMFilter | SCMFilter | CICDFilter;
  };
  _calculation_field: string;
  _application: string;

  constructor(
    restData: any | undefined,
    integration_type?: WorkflowIntegrationType,
    defaultCalculationField: string = "end_time",
    defaultJobs?: string[],
    application?: string,
    integration_id?: string,
    integration_ids?: string[]
  ) {
    if (!restData) {
      if (integration_type) {
        this._integration_id = integration_id;
        this._integration_ids = integration_ids;
        this._calculation_field = defaultCalculationField;
        this._application = application || "";
        this._filters = {
          deployment_frequency: getDefaultIntegrationFilters(
            integration_type,
            "deployementFrequency",
            defaultJobs,
            application
          )
        };
      }
    } else {
      this._integration_id = restData.integration_id;
      this._calculation_field = restData.calculation_field || defaultCalculationField;
      this._integration_ids = restData.integration_ids;
      this._application = restData.application;
      if (restData.filters.deployment_frequency.integration_type === WorkflowIntegrationType.SCM) {
        if (restData.filters.deployment_frequency.scm_filters?._defaultValue === DEFAULT_METRIC_IDENTIFIERS.release) {
          this._filters = restData.filters;
        } else {
          const hiddenKeys = getSCMRemoveFilters(restData.filters.deployment_frequency);
          let requiredFields: string[] = [];
          if (restData.filters.deployment_frequency.deployment_route === "commit") {
            requiredFields = Object.keys(DORA_SCM_DEFINITIONS).filter(key => !(hiddenKeys || []).includes(key));
          }
          let scmFilterData = new DORAConfigDefinition(
            restData.filters.deployment_frequency.scm_filters,
            DEFAULT_METRIC_IDENTIFIERS.release,
            hiddenKeys,
            requiredFields
          );

          this._filters = {
            ...restData.filters,
            deployment_frequency: {
              ...restData.filters.deployment_frequency,
              scm_filters: scmFilterData
            }
          };
        }
      } else {
        this._filters = restData.filters;
      }
    }
  }

  get integration_id() {
    return this._integration_id;
  }

  get integration_ids() {
    return this._integration_ids;
  }

  get filter() {
    return this._filters;
  }

  get calculation_field() {
    return get(this._filters, ["deployment_frequency", "calculation_field"], this._calculation_field);
  }

  set calculation_field(field: any) {
    this._calculation_field = field;
    this._filters = {
      ...this._filters,
      deployment_frequency: {
        ...(this._filters?.deployment_frequency || {}),
        calculation_field: field
      }
    };
  }

  get application() {
    return this._application;
  }
  get integrationType() {
    return get(this._filters, ["deployment_frequency", "integration_type"]);
  }
  set application(app) {
    this._application = app;
  }
  set integration_id(integrationId) {
    this._integration_id = integrationId;
  }
  set integration_ids(integrationIds) {
    this._integration_ids = integrationIds;
  }
  set filter(filters) {
    this._filters = filters;
  }

  get json() {
    return {
      integration_id: this._integration_id,
      integration_ids: this._integration_ids,
      filters: this._filters,
      application: this._application,
      calculation_field: get(this._filters, ["deployment_frequency", "calculation_field"], this._calculation_field)
    };
  }
}

export class RestChangeFailureRate {
  _integration_id?: string;
  _integration_ids?: string[];
  _is_absolute: boolean;
  _filters: {
    failed_deployment: IMFilter | SCMFilter | CICDFilter;
    total_deployment: IMFilter | SCMFilter | CICDFilter;
  };
  _calculation_field: string;
  _application: string;

  constructor(
    restData: any | undefined,
    integration_type?: WorkflowIntegrationType,
    defaultCalculationField: string = "end_time",
    defaultJobs?: string[],
    application?: string,
    integration_id?: string,
    integration_ids?: string[]
  ) {
    if (!restData && integration_type) {
      this._integration_id = integration_id;
      this._integration_ids = integration_ids;
      this._filters = {
        failed_deployment: getDefaultIntegrationFilters(integration_type, "failedDeployment", defaultJobs, application),
        total_deployment: getDefaultIntegrationFilters(integration_type, "totalDeployment", defaultJobs, application)
      };
      this._calculation_field = defaultCalculationField;
      this._application = application || "";
    } else {
      this._integration_id = restData.integration_id;
      this._calculation_field = restData.calculation_field || defaultCalculationField;
      this._integration_ids = restData?.integration_ids;
      this._application = restData.application;
      if (restData.filters.failed_deployment.integration_type === WorkflowIntegrationType.SCM) {
        if (restData.filters.failed_deployment.scm_filters?._defaultValue === DEFAULT_METRIC_IDENTIFIERS.hotfix) {
          this._filters = restData.filters;
        } else {
          const failedDepHiddenKeys = getSCMRemoveFilters(restData.filters.failed_deployment);
          let failedDepRequiredFields: string[] = [];
          if (restData.filters.failed_deployment.deployment_route === "commit") {
            failedDepRequiredFields = Object.keys(DORA_SCM_DEFINITIONS).filter(
              key => !(failedDepHiddenKeys || []).includes(key)
            );
          }
          let failedDeploymentData = new DORAConfigDefinition(
            restData.filters.failed_deployment.scm_filters,
            DEFAULT_METRIC_IDENTIFIERS.hotfix,
            failedDepHiddenKeys,
            failedDepRequiredFields
          );

          let totalDeploymentData;
          let totalDeployment = restData.filters.total_deployment;
          if (restData.is_absolute && isEmpty(restData.filters.total_deployment)) {
            totalDeployment = getDefaultIntegrationFilters(
              restData.filters.failed_deployment.integration_type,
              "totalDeployment",
              defaultJobs,
              application
            );
            totalDeploymentData = totalDeployment.scm_filters;
          } else {
            const totalDepHiddenKeys = getSCMRemoveFilters(restData.filters.total_deployment);
            let totalDepRequiredFields: string[] = [];
            if (restData.filters.total_deployment.deployment_route === "commit") {
              totalDepRequiredFields = Object.keys(DORA_SCM_DEFINITIONS).filter(
                key => !(totalDepHiddenKeys || []).includes(key)
              );
            }
            totalDeploymentData = new DORAConfigDefinition(
              restData.filters.total_deployment.scm_filters,
              DEFAULT_METRIC_IDENTIFIERS.release,
              totalDepHiddenKeys,
              totalDepRequiredFields
            );
          }

          this._filters = {
            ...restData.filters,
            failed_deployment: {
              ...restData.filters.failed_deployment,
              integration_type: restData.filters.failed_deployment.integration_type,
              scm_filters: failedDeploymentData
            },
            total_deployment: {
              ...totalDeployment,
              integration_type: restData.filters.failed_deployment.integration_type,
              scm_filters: totalDeploymentData
            }
          };
        }
      } else {
        if (restData.is_absolute && isEmpty(restData.filters.total_deployment)) {
          let getDefaultTotalData = getDefaultIntegrationFilters(
            restData.filters.failed_deployment.integration_type,
            "totalDeployment",
            [],
            application
          );

          restData = {
            ...restData,
            filters: {
              ...restData.filters,
              total_deployment: getDefaultTotalData
            }
          };
        }

        this._filters = restData.filters;
      }
    }
    this._is_absolute = (restData || ({} as any))?.is_absolute;
  }

  get integration_id() {
    return this._integration_id;
  }
  get integration_ids() {
    return this._integration_ids;
  }
  get application() {
    return this._application;
  }
  set application(app) {
    this._application = app;
  }
  get filter() {
    return this._filters;
  }
  set integration_id(integrationId) {
    this._integration_id = integrationId;
  }
  set integration_ids(integrationIds) {
    this._integration_ids = integrationIds;
  }
  set filter(filters) {
    this._filters = filters;
  }
  get calculation_field() {
    return get(this._filters, ["failed_deployment", "calculation_field"], this._calculation_field);
  }

  get is_absolute() {
    return this._is_absolute;
  }
  set is_absolute(value) {
    this._is_absolute = value;
  }

  get integrationType() {
    return get(this._filters, ["failed_deployment", "integration_type"]);
  }

  get json() {
    return {
      integration_id: this.integration_id,
      integration_ids: this.integration_ids,
      filters: this._filters,
      is_absolute: this._is_absolute,
      calculation_field: this._calculation_field,
      application: this._application
    };
  }
}

export enum AcceptanceTimeUnit {
  SECONDS = "SECONDS",
  MINUTES = "MINUTES",
  DAYS = "DAYS"
}

export enum VelocityConfigStage {
  PRE_DEVELOPMENT_STAGE = "pre_development_custom_stages",
  FIXED_STAGE = "fixed_stages",
  POST_DEVELOPMENT_STAGE = "post_development_custom_stages"
}

export enum TriggerEventType {
  JIRA_STATUS = "JIRA_STATUS",
  CICD_JOB_RUN = "CICD_JOB_RUN",
  WORKITEM_STATUS = "WORKITEM_STATUS"
}

//THIS OLD CLASS IS CODE FOR NEW LEAD TIME & MTTR LIKE BEFORE WE MERGE WITH RETRO FIT PART , SO WHEN WE NEED THIS NEW JUST OPEN THE CODE
export class RestDevelopmentStageConfigOld {
  _integration_id: string;
  _pre_development_custom_stages?: RestStageConfig[];
  _fixed_stages?: RestStageConfig[];
  _post_development_custom_stages?: RestStageConfig[];
  _event: "ticket_created" | "commit_created" | "api_event";
  _fixed_stages_enabled?: boolean;
  _filters: {
    lead_time_for_changes?:
      | IMFilter
      | {
          integration_type: WorkflowIntegrationType.SCM;
        };
    mean_time_to_restore?:
      | IMFilter
      | {
          integration_type: WorkflowIntegrationType.SCM;
        };
  };

  constructor(
    restData: any | undefined,
    defaultIntegrationId?: string,
    integration_type?: WorkflowIntegrationType,
    basicStages?: any,
    defaultScmValue: string = "",
    profileStage: string = "",
    application?: string
  ) {
    if (!restData) {
      this._pre_development_custom_stages = [];
      this._fixed_stages = [];
      this._post_development_custom_stages = [];
      this._fixed_stages_enabled = true;
      this._filters = { [profileStage]: { integration_type: undefined } };
      if (defaultIntegrationId && integration_type) {
        this._integration_id = defaultIntegrationId;
        if (integration_type === WorkflowIntegrationType.IM) {
          this._filters = {
            [profileStage]: {
              integration_type: WorkflowIntegrationType.IM,
              filter: [
                {
                  key: application === IntegrationTypes.AZURE ? "workitem_types" : "issue_types",
                  param: integrationParameters.CONTAINS,
                  value: ["New Feature"]
                }
              ]
            }
          };
          this._event = "ticket_created";
        } else {
          this._event = "commit_created";
          this._filters = {
            [profileStage]: {
              integration_type: WorkflowIntegrationType.SCM
            }
          };
        }
        if (basicStages) {
          this._fixed_stages = (((basicStages || {}) as any)?.fixed_stages || []).map((stage: any) => {
            const hiddenKeys =
              stage.event.type === "SCM_PR_MERGED"
                ? ["source_branch", "tags"]
                : stage.event.type === "SCM_PR_SOURCE_BRANCH"
                ? ["target_branch", "commit_branch", "tags", "labels"]
                : ["target_branch", "commit_branch", "tags", "labels", "source_branch"];
            return new RestStageConfig(
              {
                ...(stage || {}),
                id: stage.id || uuid(),
                type: VelocityConfigStage.FIXED_STAGE,
                cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {}
              },
              defaultScmValue,
              hiddenKeys
            );
          });
        }
      }
    } else {
      this._integration_id = restData.integration_id;
      this._pre_development_custom_stages = (((restData || {}) as any)?.pre_development_custom_stages || []).map(
        (stage: any) =>
          new RestStageConfig({
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.PRE_DEVELOPMENT_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {},
            profileStage: restData && restData.hasOwnProperty("filters") ? Object.keys(restData?.filters) : ""
          })
      );

      this._fixed_stages = (((restData || {}) as any)?.fixed_stages || []).map((stage: any) => {
        const hiddenKeys =
          stage.event.type === "SCM_PR_MERGED"
            ? ["source_branch", "tags"]
            : stage.event.type === "SCM_PR_SOURCE_BRANCH"
            ? ["target_branch", "commit_branch", "tags", "labels"]
            : ["target_branch", "commit_branch", "tags", "labels", "source_branch"];
        return new RestStageConfig(
          {
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.FIXED_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {},
            profileStage: restData && restData.hasOwnProperty("filters") ? Object.keys(restData?.filters) : ""
          },
          defaultScmValue,
          hiddenKeys
        );
      });

      this._post_development_custom_stages = (((restData || {}) as any)?.post_development_custom_stages || []).map(
        (stage: any) =>
          new RestStageConfig({
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.POST_DEVELOPMENT_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {},
            profileStage: restData && restData.hasOwnProperty("filters") ? Object.keys(restData?.filters) : ""
          })
      );
      this._event = restData.event ?? this.deriveEvent(restData);
      this._filters = ((restData || {}) as any)?.filters;
      const keyPresent = ((restData || {}) as any).hasOwnProperty("fixed_stages_enabled");
      this._fixed_stages_enabled = keyPresent
        ? ((restData || {}) as any).fixed_stages_enabled
        : ((restData || {}) as any).hasOwnProperty("fixed_stages");
    }
  }
  deriveEvent(restData: any): "ticket_created" | "commit_created" | "api_event" {
    if (restData?.starting_event_is_commit_created) {
      return "commit_created";
    } else if (restData?.starting_event_is_generic_event) {
      return "api_event";
    }
    return "ticket_created";
  }

  get integration_id() {
    return this._integration_id;
  }
  set integration_id(integrationId) {
    this._integration_id = integrationId;
  }

  get pre_development_custom_stages() {
    return this._pre_development_custom_stages?.sort((a, b) => a?.order - b?.order);
  }

  set pre_development_custom_stages(pre_development_custom_stages) {
    this._pre_development_custom_stages = pre_development_custom_stages?.map((stage, index) => {
      return new RestStageConfig(stage as any);
    });
  }

  get fixed_stages() {
    return this._fixed_stages;
  }

  set fixed_stages(fixed_stages) {
    this._fixed_stages = fixed_stages?.map((stage, index) => {
      return new RestStageConfig(stage as any);
    });
  }

  get post_development_custom_stages() {
    return this._post_development_custom_stages;
  }

  set post_development_custom_stages(post_development_custom_stages) {
    this._post_development_custom_stages = post_development_custom_stages?.map((stage, index) => {
      return new RestStageConfig(stage as any);
    });
  }

  get event() {
    return this._event;
  }

  set event(value) {
    this._event = value;
  }

  get fixed_stages_enabled() {
    return this._fixed_stages_enabled;
  }

  set fixed_stages_enabled(value) {
    this._fixed_stages_enabled = value;
  }

  get filters() {
    return this._filters;
  }
  set filters(newFilter) {
    this._filters = newFilter;
  }

  get all_stages() {
    return [
      ...(this.pre_development_custom_stages || []),
      ...(this.fixed_stages || []),
      ...(this.post_development_custom_stages || [])
    ];
  }

  stages(type: VelocityConfigStage) {
    switch (type) {
      case VelocityConfigStage.PRE_DEVELOPMENT_STAGE:
        return this._pre_development_custom_stages;
      case VelocityConfigStage.FIXED_STAGE:
        return this._fixed_stages;
      case VelocityConfigStage.POST_DEVELOPMENT_STAGE:
        return this._post_development_custom_stages;
      default:
        return [];
    }
  }

  convertEvent(event: "ticket_created" | "commit_created" | "api_event") {
    let starting_event_is_commit_created = false;
    let starting_event_is_generic_event = false;
    switch (event) {
      case "commit_created":
        starting_event_is_commit_created = true;
        break;
      case "api_event":
        starting_event_is_generic_event = true;
        break;
    }
    return {
      starting_event_is_commit_created,
      starting_event_is_generic_event
    };
  }

  get json() {
    return sanitizeObject({
      integration_id: this._integration_id,
      integration_ids: [this._integration_id],
      pre_development_custom_stages: (this._pre_development_custom_stages || []).map(stage => stage.json),
      fixed_stages: (this._fixed_stages || []).map(stage => stage.json),
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.json),
      fixed_stages_enabled: this._fixed_stages_enabled,
      filters: this._filters,
      event: this._event,
      ...this.convertEvent(this._event)
    });
  }

  get postData() {
    return sanitizeObject({
      integration_id: this._integration_id,
      integration_ids: [this._integration_id],
      pre_development_custom_stages:
        this._event === "commit_created"
          ? []
          : (this._pre_development_custom_stages || []).map(stage => stage.postData),
      fixed_stages: this._fixed_stages_enabled
        ? (this._fixed_stages || []).filter(stage => stage.enabled).map(stage => stage.postData)
        : [],
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.postData),
      fixed_stages_enabled: this._fixed_stages_enabled,
      filters: this._filters,
      event: this._event,
      ...this.convertEvent(this._event)
    });
  }
}
export class RestWorkflowProfile {
  _id?: string;
  _name?: string;
  _isValidName: boolean;
  _description?: string;
  _workspace_to_org: Record<string, Array<string>>;
  _deployment_frequency: RestDeploymentFrequency;
  _change_failure_rate: RestChangeFailureRate;
  _lead_time_for_changes: RestDevelopmentStageConfig;
  _mean_time_to_restore: RestDevelopmentStageConfig;
  _hide_cancel_save_button: boolean;
  is_new: true;

  constructor(
    restData: any = null,
    failureRateIntegration?: Integration,
    leadIntegration?: Integration,
    basicStages?: any,
    defaultJobs?: string[]
  ) {
    this._id = undefined;
    this._name = undefined;
    this._isValidName = true;
    this._description = undefined;
    this._workspace_to_org = {
      w_: []
    };
    this._hide_cancel_save_button = false;
    this.is_new = true;

    if (restData) {
      this._id = (restData || ({} as any))?.id;
      this._name = (restData || ({} as any))?.name;
      this._isValidName = (restData || ({} as any))?.hasOwnProperty("isValidName") ? restData.isValidName : true;
      this._description = (restData || ({} as any))?.description;
      this._workspace_to_org = (restData || ({} as any))?.workspace_to_org || { w_: [] };
      this._deployment_frequency = new RestDeploymentFrequency((restData || ({} as any))?.deployment_frequency);
      this._change_failure_rate = new RestChangeFailureRate(
        (restData || ({} as any))?.change_failure_rate,
        undefined,
        "pr_merged_at",
        undefined,
        failureRateIntegration?.application
      );
      this._lead_time_for_changes = new RestDevelopmentStageConfig((restData || ({} as any))?.lead_time_for_changes);
      this._mean_time_to_restore = new RestDevelopmentStageConfig((restData || ({} as any))?.mean_time_to_restore);
      this._hide_cancel_save_button = (restData || ({} as any))?.hide_cancel_save_button;
    } else {
      const failureRateIntegrationId = (failureRateIntegration?.id as string) || undefined;
      const failureRateIntegration_type = findIntegrationType(failureRateIntegration || null);
      const failureRateApplication = failureRateIntegration?.application;
      const defaultCalculationField = getDefaultCalculationField(failureRateIntegration_type, failureRateApplication);
      this._deployment_frequency = new RestDeploymentFrequency(
        null,
        failureRateIntegration_type,
        defaultCalculationField,
        defaultJobs,
        failureRateApplication,
        failureRateIntegrationId,
        [failureRateIntegrationId as string]
      );
      this._change_failure_rate = new RestChangeFailureRate(
        null,
        failureRateIntegration_type,
        defaultCalculationField,
        defaultJobs,
        failureRateApplication,
        failureRateIntegrationId,
        [failureRateIntegrationId as string]
      );

      this._lead_time_for_changes = new RestDevelopmentStageConfig(
        null,
        [IntegrationTypes.JIRA],
        basicStages,
        DEFAULT_METRIC_IDENTIFIERS.release,
        "lead_time_for_changes"
      );

      this._mean_time_to_restore = new RestDevelopmentStageConfig(
        null,
        [IntegrationTypes.JIRA],
        basicStages,
        DEFAULT_METRIC_IDENTIFIERS.defect,
        "mean_time_to_restore"
      );
    }
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get isValidName() {
    return this._isValidName;
  }

  set isValidName(isValid) {
    this._isValidName = isValid;
  }

  get description() {
    return this._description;
  }

  set description(desc) {
    this._description = desc;
  }

  get validate() {
    let isValid = !!(this._isValidName && this._name?.length);
    return isValid;
  }

  get workspace_to_org() {
    return this._workspace_to_org;
  }

  set workspace_to_org(workspace_to_orgMap) {
    this._workspace_to_org = workspace_to_orgMap;
  }
  get deployment_frequency() {
    return this._deployment_frequency;
  }

  get change_failure_rate() {
    return this._change_failure_rate;
  }
  get lead_time_for_changes() {
    return this._lead_time_for_changes;
  }
  get mean_time_to_restore() {
    return this._mean_time_to_restore;
  }

  get hide_cancel_save_button() {
    return this._hide_cancel_save_button;
  }

  set hide_cancel_save_button(hide_cancel_save_button) {
    this._hide_cancel_save_button = hide_cancel_save_button;
  }

  get json() {
    let associated_ou_ref_ids: Array<string> = [];
    Object.values(this._workspace_to_org).forEach((orgList: Array<string>) => {
      associated_ou_ref_ids = [...associated_ou_ref_ids, ...orgList];
    });
    return sanitizeObject({
      id: this._id,
      name: this._name,
      isValidName: this._isValidName,
      description: this._description,
      workspace_to_org: this._workspace_to_org,
      deployment_frequency: this._deployment_frequency.json,
      change_failure_rate: this._change_failure_rate.json,
      lead_time_for_changes: this._lead_time_for_changes.json,
      mean_time_to_restore: this._mean_time_to_restore.json,
      associated_ou_ref_ids,
      is_new: true,
      hide_cancel_save_button: this._hide_cancel_save_button
    });
  }

  get postData() {
    let associated_ou_ref_ids: Array<string> = [];
    Object.values(this._workspace_to_org).forEach((orgList: Array<string>) => {
      associated_ou_ref_ids = [...associated_ou_ref_ids, ...orgList];
    });
    return sanitizeObject({
      id: this._id,
      name: this._name,
      isValidName: this._isValidName,
      description: this._description,
      workspace_to_org: this._workspace_to_org,
      deployment_frequency: this._deployment_frequency.json,
      change_failure_rate: this._change_failure_rate.json,
      lead_time_for_changes: this._lead_time_for_changes.postData,
      mean_time_to_restore: this._mean_time_to_restore.postData,
      associated_ou_ref_ids,
      is_new: true,
      hide_cancel_save_button: this._hide_cancel_save_button
    });
  }
}
