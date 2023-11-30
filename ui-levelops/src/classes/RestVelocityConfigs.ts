import { DEFAULT_METRIC_IDENTIFIERS } from "configurations/pages/lead-time-profiles/helpers/constants";
import { sanitizeObject } from "utils/commonUtils";
import { v1 as uuid } from "uuid";
import { DORAConfigDefinition } from "./DORAConfigDefinition";
import { StageEndOptions } from "./StageEndOptions";
import { IssueManagementOptions } from "constants/issueManagementOptions";

export enum VelocityConfigStage {
  PRE_DEVELOPMENT_STAGE = "pre_development_custom_stages",
  FIXED_STAGE = "fixed_stages",
  POST_DEVELOPMENT_STAGE = "post_development_custom_stages"
}

export enum AcceptanceTimeUnit {
  SECONDS = "SECONDS",
  MINUTES = "MINUTES",
  DAYS = "DAYS"
}

export enum TriggerEventType {
  JIRA_STATUS = "JIRA_STATUS",
  CICD_JOB_RUN = "CICD_JOB_RUN",
  WORKITEM_STATUS = "WORKITEM_STATUS",
  HARNESSCD_JOB_RUN = "HARNESSCD_JOB_RUN",
  HARNESSCI_JOB_RUN = "HARNESSCI_JOB_RUN",
  JIRA_RELEASE = "JIRA_RELEASE",
  GITHUB_ACTIONS_JOB_RUN = "GITHUB_ACTIONS_JOB_RUN"
}

export const CICD_TRIGGER_EVENT_TYPE = [TriggerEventType.HARNESSCD_JOB_RUN, TriggerEventType.HARNESSCI_JOB_RUN, TriggerEventType.CICD_JOB_RUN, TriggerEventType.GITHUB_ACTIONS_JOB_RUN];

export const ISSUE_MANAGEMENT_TRIGGER_TYPE = [TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS];

export class RestVelocityConfigs {
  _id?: string;
  _name?: string;
  _default_config?: boolean;
  _description?: string;
  _created_at?: number;
  _updated_at?: number;
  _pre_development_custom_stages?: RestVelocityConfigStage[];
  _fixed_stages?: RestVelocityConfigStage[];
  _post_development_custom_stages?: RestVelocityConfigStage[];
  _cicd_job_id_name_mappings?: any;
  _starting_event_is_commit_created?: boolean;
  _starting_event_is_generic_event?: boolean;
  _issue_management_integrations?: string[];
  _fixed_stages_enabled?: boolean;
  _release: DORAConfigDefinition;
  _deployment: DORAConfigDefinition;
  _hotfix: DORAConfigDefinition;
  _defect: DORAConfigDefinition;
  is_new: boolean;
  _jira_only?: boolean;

  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._default_config = false;
    this._description = undefined;
    this._created_at = undefined;
    this._updated_at = undefined;
    this._starting_event_is_commit_created = false;
    this._starting_event_is_generic_event = false;
    this._pre_development_custom_stages = [];
    this._fixed_stages = [];
    this._post_development_custom_stages = [];
    this._cicd_job_id_name_mappings = {};
    this._issue_management_integrations = [IssueManagementOptions.JIRA];
    this._fixed_stages_enabled = true;
    this._release = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.release);
    this._deployment = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.deployment);
    this._hotfix = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.hotfix);
    this._defect = new DORAConfigDefinition(null, DEFAULT_METRIC_IDENTIFIERS.defect);
    this.is_new = false;
    this._jira_only = false;

    if (restData) {
      this._id = ((restData || {}) as any)?.id;
      this._name = ((restData || {}) as any)?.name;
      this._description = ((restData || {}) as any)?.description;
      this._default_config = ((restData || {}) as any)?.default_config;
      this._created_at = ((restData || {}) as any)?.created_at;
      this._updated_at = ((restData || {}) as any)?.updated_at;
      this._cicd_job_id_name_mappings = ((restData || {}) as any)?.cicd_job_id_name_mappings || {};
      this._starting_event_is_commit_created = ((restData || {}) as any)?.starting_event_is_commit_created;
      this._starting_event_is_generic_event = ((restData || {}) as any)?.starting_event_is_generic_event;
      this._jira_only = ((restData || {}) as any)?.jira_only;

      this._pre_development_custom_stages = (((restData || {}) as any)?.pre_development_custom_stages || []).map(
        (stage: any) =>
          new RestVelocityConfigStage({
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.PRE_DEVELOPMENT_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {}
          })
      );
      this._fixed_stages = (((restData || {}) as any)?.fixed_stages || []).map(
        (stage: any) =>
          new RestVelocityConfigStage({
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.FIXED_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {}
          })
      );
      this._post_development_custom_stages = (((restData || {}) as any)?.post_development_custom_stages || []).map(
        (stage: any) =>
          new RestVelocityConfigStage({
            ...(stage || {}),
            id: stage.id || uuid(),
            type: VelocityConfigStage.POST_DEVELOPMENT_STAGE,
            cicd_job_id_name_mappings: ((restData || {}) as any).cicd_job_id_name_mappings || {}
          })
      );
      this._issue_management_integrations = ((restData || {}) as any)?.issue_management_integrations || [
        IssueManagementOptions.JIRA
      ];
      const keyPresent = ((restData || {}) as any).hasOwnProperty("fixed_stages_enabled");
      this._fixed_stages_enabled = keyPresent
        ? ((restData || {}) as any).fixed_stages_enabled
        : ((restData || {}) as any).hasOwnProperty("fixed_stages");

      if (((restData || {}) as any).hasOwnProperty("scm_config")) {
        const scm_config = (restData as any).scm_config;
        if ((scm_config || ({} as any)).hasOwnProperty("release")) {
          this._release = new DORAConfigDefinition((scm_config as any).release, DEFAULT_METRIC_IDENTIFIERS.release);
        }
        if ((scm_config || ({} as any)).hasOwnProperty("deployment")) {
          this._deployment = new DORAConfigDefinition(
            (scm_config as any).deployment,
            DEFAULT_METRIC_IDENTIFIERS.deployment
          );
        }
        if ((scm_config || ({} as any)).hasOwnProperty("defect")) {
          this._defect = new DORAConfigDefinition((scm_config as any).defect, DEFAULT_METRIC_IDENTIFIERS.defect);
        }
        if ((scm_config || ({} as any)).hasOwnProperty("hotfix")) {
          this._hotfix = new DORAConfigDefinition((scm_config as any).hotfix, DEFAULT_METRIC_IDENTIFIERS.hotfix);
        }
      }
    }

    this.cloneConfig = this.cloneConfig.bind(this);
    this.addStage = this.addStage.bind(this);
    this.removeStage = this.removeStage.bind(this);
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

  get defaultConfig() {
    return this._default_config;
  }

  set defaultConfig(defaultConfig) {
    this._default_config = defaultConfig;
  }

  get description() {
    return this._description;
  }

  set description(description) {
    this._description = description;
  }

  get created_at() {
    return this._created_at ? this._created_at / 1000 : undefined; // Convert to secs
  }

  get updated_at() {
    return this._updated_at ? this._updated_at / 1000 : undefined; // Convert to secs
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

  get pre_development_custom_stages() {
    return this._pre_development_custom_stages?.sort((a, b) => a?.order - b?.order);
  }

  set pre_development_custom_stages(pre_development_custom_stages) {
    this._pre_development_custom_stages = pre_development_custom_stages?.map((stage, index) => {
      return new RestVelocityConfigStage(stage as any);
    });
  }

  get fixed_stages() {
    return this._fixed_stages;
  }

  set fixed_stages(fixed_stages) {
    this._fixed_stages = fixed_stages?.map((stage, index) => {
      return new RestVelocityConfigStage(stage as any);
    });
  }

  get post_development_custom_stages() {
    return this._post_development_custom_stages;
  }

  set post_development_custom_stages(post_development_custom_stages) {
    this._post_development_custom_stages = post_development_custom_stages?.map((stage, index) => {
      return new RestVelocityConfigStage(stage as any);
    });
  }

  get all_stages() {
    if (this.jira_only) {
      return [...(this.pre_development_custom_stages || [])];
    } else {
      return [
        ...(this.pre_development_custom_stages || []),
        ...(this.fixed_stages || []),
        ...(this.post_development_custom_stages || [])
      ];
    }
  }

  get cicd_job_id_name_mappings() {
    return this._cicd_job_id_name_mappings || {};
  }

  set cicd_job_id_name_mappings(value) {
    this._cicd_job_id_name_mappings = value || {};
  }

  get starting_event_is_commit_created() {
    return this._starting_event_is_commit_created;
  }

  set starting_event_is_commit_created(value) {
    this._starting_event_is_commit_created = value;
  }

  get starting_event_is_generic_event() {
    return this._starting_event_is_generic_event;
  }

  set starting_event_is_generic_event(value) {
    this._starting_event_is_generic_event = value;
  }

  get fixed_stages_enabled() {
    return this._fixed_stages_enabled;
  }

  set fixed_stages_enabled(value) {
    this._fixed_stages_enabled = value;
  }

  get release() {
    return this._release;
  }

  set release(value) {
    this._release = value;
  }

  get deployment() {
    return this._deployment;
  }

  set deployment(value) {
    this._deployment = value;
  }

  get hotfix() {
    return this._hotfix;
  }

  set hotfix(value) {
    this._hotfix = value;
  }

  get defect() {
    return this._defect;
  }

  set defect(value) {
    this._defect = value;
  }

  get jira_only() {
    return this._jira_only;
  }

  set jira_only(jira_only) {
    this._jira_only = jira_only;
  }

  get validate() {
    let disabled = (this._name || "").length > 0;
    (this._pre_development_custom_stages || []).forEach(stage => {
      if (!stage.event?.values?.length && stage.event?.type !== TriggerEventType.JIRA_RELEASE) {
        disabled = false;
      }
    });

    let checkJiraReleaseStage = (this._pre_development_custom_stages || []).filter(
      data => data?.event?.type === TriggerEventType.JIRA_RELEASE && data.enabled
    );
    if (
      checkJiraReleaseStage &&
      checkJiraReleaseStage.length > 0 &&
      (this._pre_development_custom_stages || []).length <= 1
    ) {
      disabled = false;
    }

    let otherCicdJobs: string[] | undefined = undefined;
    (this._post_development_custom_stages || []).forEach(stage => {
      if (stage.event?.type === TriggerEventType.CICD_JOB_RUN) {
        if (!otherCicdJobs) {
          otherCicdJobs = stage.event.values;
        } else if (stage.event.values.some((job: string) => otherCicdJobs?.includes(job))) {
          disabled = false;
        }
      }
      if (!stage.event?.values?.length && stage.event?.selectedJob === '"MANUALLY"') {
        disabled = false;
      }
    });
    return disabled;
  }

  cloneConfig(cloneData: any) {
    this._name = `Copy of ${cloneData.name}`;
    this._description = cloneData?.description;
    this._jira_only = cloneData?.jira_only || false;
    this._default_config = false;
    this._pre_development_custom_stages = ((cloneData || ({} as any))?.pre_development_custom_stages || []).map(
      (stage: any) => new RestVelocityConfigStage(stage || {})
    );
    this._fixed_stages = ((cloneData || ({} as any))?.fixed_stages || []).map(
      (stage: any) => new RestVelocityConfigStage(stage || {})
    );
    this._post_development_custom_stages = ((cloneData || ({} as any))?.post_development_custom_stages || []).map(
      (stage: any) => new RestVelocityConfigStage(stage || {})
    );
    this._issue_management_integrations = cloneData?.issue_management_integrations ?? [IssueManagementOptions.JIRA];
    this._starting_event_is_commit_created = !!cloneData?.starting_event_is_commit_created ?? false;
    this._starting_event_is_generic_event = !!cloneData?.starting_event_is_generic_event ?? false;
    this._cicd_job_id_name_mappings = cloneData?.cicd_job_id_name_mappings;
    this._fixed_stages_enabled = !!cloneData?.fixed_stages_enabled ?? false;
  }

  addStage(stageData: any, type: VelocityConfigStage) {
    const stageId = stageData?.id;

    const existingStage = this[type]?.find(stage => stage.id === stageId);
    if (stageId && existingStage) {
      this[type] = this[type]?.filter(stage => stage.id !== stageId);
      this[type]?.push(new RestVelocityConfigStage(stageData));
    } else {
      this[type]?.push(new RestVelocityConfigStage(stageData));
    }
  }

  removeStage(stageId: any, type: VelocityConfigStage) {
    this[type] = this[type]?.filter(stage => stage.id !== stageId);
  }

  get json() {
    return sanitizeObject({
      id: this._id,
      name: this._name,
      description: this._description,
      default_config: this._default_config,
      created_at: this._created_at,
      updated_at: this._updated_at,
      pre_development_custom_stages: (this._pre_development_custom_stages || []).map(stage => stage.json),
      fixed_stages: (this._fixed_stages || []).map(stage => stage.json),
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.json),
      cicd_job_id_name_mappings: this._cicd_job_id_name_mappings,
      starting_event_is_commit_created: this._starting_event_is_commit_created,
      starting_event_is_generic_event: this._starting_event_is_generic_event,
      issue_management_integrations: this.issue_management_integrations,
      fixed_stages_enabled: this._fixed_stages_enabled,
      scm_config: {
        release: this._release.json,
        deployment: this._deployment.json,
        hotfix: this._hotfix.json,
        defect: this._defect.json
      },
      jira_only: this._jira_only
    });
  }

  get postData() {
    return sanitizeObject({
      id: this._id,
      name: this._name,
      description: this._description,
      default_config: this._default_config,
      pre_development_custom_stages: this._starting_event_is_commit_created
        ? []
        : (this._pre_development_custom_stages || []).filter(stage => stage.enabled).map(stage => stage.postData),
      fixed_stages: this._fixed_stages_enabled
        ? (this._fixed_stages || []).filter(stage => stage.enabled).map(stage => stage.postData)
        : [],
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.postData),
      starting_event_is_commit_created: this._starting_event_is_commit_created,
      starting_event_is_generic_event: this._starting_event_is_generic_event,
      issue_management_integrations: this.issue_management_integrations,
      scm_config: this._jira_only
        ? {}
        : {
            release: this._release.postData,
            deployment: this._deployment.postData,
            hotfix: this._hotfix.postData,
            defect: this._defect.postData
          },
      jira_only: this._jira_only
    });
  }

  get issue_management_integrations() {
    return this._issue_management_integrations;
  }

  set issue_management_integrations(value) {
    this._issue_management_integrations = value;
  }
}

export type sectionSelectedFilterType = {
  key: string;
  param: string;
  value: any;
  metadata?: any;
};
export class RestVelocityConfigStage {
  _id?: string;
  _name?: string;
  _description?: string;
  _order?: number;
  _event?: { type: string; values: any[]; params: any, selectedJob?: string };
  _lower_limit_value?: number;
  _upper_limit_value?: number;
  _lower_limit_unit?: AcceptanceTimeUnit;
  _upper_limit_unit?: AcceptanceTimeUnit;
  _type?: VelocityConfigStage;
  _enabled?: boolean;
  _filter?: sectionSelectedFilterType[];

  constructor(restData = null, stageFlag: any = null, stageOrder: number = 0) {
    this._id = undefined;
    this._name = stageFlag === TriggerEventType.JIRA_RELEASE ? "Release" : undefined;
    this._description = undefined;
    this._order = stageFlag === TriggerEventType.JIRA_RELEASE ? stageOrder : undefined;
    this._event = {
      type: stageFlag === TriggerEventType.JIRA_RELEASE ? TriggerEventType.JIRA_RELEASE : TriggerEventType.JIRA_STATUS,
      values: [],
      params: undefined
    };
    this._lower_limit_value = 4;
    this._upper_limit_value = 11;
    this._lower_limit_unit = AcceptanceTimeUnit.DAYS;
    this._upper_limit_unit = AcceptanceTimeUnit.DAYS;
    this._type = undefined;
    this._enabled = stageFlag === TriggerEventType.JIRA_RELEASE ? false : true;
    this._filter = [];

    if (restData) {
      this._id = ((restData || {}) as any)?.id;
      this._name = ((restData || {}) as any)?.name;
      this._description = ((restData || {}) as any)?.description;
      this._order = ((restData || {}) as any)?.order;
      this._event = ((restData || {}) as any)?.event || {
        type: TriggerEventType.JIRA_STATUS,
        values: [],
        params: undefined
      };
      this._lower_limit_value = ((restData || {}) as any)?.lower_limit_value || 4;
      this._upper_limit_value = ((restData || {}) as any)?.upper_limit_value || 11;
      this._lower_limit_unit = ((restData || {}) as any)?.lower_limit_unit || AcceptanceTimeUnit.DAYS;
      this._upper_limit_unit = ((restData || {}) as any)?.upper_limit_unit || AcceptanceTimeUnit.DAYS;
      this._type = ((restData || {}) as any)?.type;
      const keyPresent = ((restData || {}) as any).hasOwnProperty("enabled");
      this._enabled = keyPresent ? ((restData || {}) as any)?.enabled : true;
      this._filter = ((restData || {}) as any)?.filter || [];
    }
  }

  get id() {
    return this._id;
  }

  set id(value) {
    this._id = value;
  }

  get name() {
    return this._name;
  }

  set name(value) {
    this._name = value;
  }

  get description() {
    return this._description;
  }

  set description(value) {
    this._description = value;
  }

  get order() {
    return this._order || 0;
  }

  set order(value) {
    this._order = value;
  }

  get event() {
    return this._event;
  }

  set event(value) {
    this._event = value;
  }

  get lower_limit_value() {
    return this._lower_limit_value;
  }

  set lower_limit_value(value) {
    this._lower_limit_value = value;
  }

  get upper_limit_value() {
    return this._upper_limit_value;
  }

  set upper_limit_value(value) {
    this._upper_limit_value = value;
  }

  get lower_limit_unit() {
    return this._lower_limit_unit;
  }

  get upper_limit_unit() {
    return this._upper_limit_unit;
  }

  get type() {
    return this._type;
  }

  set type(value) {
    this._type = value;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(value) {
    this._enabled = value;
  }

  get isJira() {
    return this._event?.type === TriggerEventType.JIRA_STATUS;
  }

  get isFixedStage() {
    return this.type === VelocityConfigStage.FIXED_STAGE;
  }

  get isPreStage() {
    return this.type === VelocityConfigStage.PRE_DEVELOPMENT_STAGE;
  }

  get isPostStage() {
    return this.type === VelocityConfigStage.POST_DEVELOPMENT_STAGE;
  }

  get isValid() {
    let valid = !!this.name && this.name.trim().length > 1;

    const params = this.event?.params || {};
    const keys = Object.keys(params);

    if (keys.length) {
      for (let i = 0; i < keys.length; i++) {
        const values = params[keys[i]];
        if (!values.length) {
          return false;
        }
      }
    }

    valid =
      valid &&
      !!this.lower_limit_unit &&
      !!this.upper_limit_unit &&
      !!this.upper_limit_value &&
      !!this.lower_limit_value;

    if (this.type === VelocityConfigStage.FIXED_STAGE) {
      if (
        this.event?.type === StageEndOptions.SCM_PR_LABEL_ADDED ||
        this.event?.type === StageEndOptions.SCM_PR_CREATED
      ) {
        return valid && !!this.event?.type;
      }
    }
    return valid && !!this.event?.type && !!this.event?.values && !!(this.event?.values || []).length;
  }

  get filter() {
    return this._filter;
  }

  set filter(value) {
    this._filter = value;
  }

  get json() {
    return sanitizeObject({
      id: this._id,
      order: this._order,
      name: this._name,
      description: this._description,
      event: this._event,
      type: this._type,
      lower_limit_value: this._lower_limit_value,
      upper_limit_value: this._upper_limit_value,
      lower_limit_unit: this._lower_limit_unit,
      upper_limit_unit: this._upper_limit_unit,
      enabled: this._enabled,
      filter: this._filter
    });
  }

  get postData() {
    return sanitizeObject({
      order: this._order,
      name: this._name,
      description: this._description,
      event: this._event,
      lower_limit_value: this._lower_limit_value,
      upper_limit_value: this._upper_limit_value,
      lower_limit_unit: this._lower_limit_unit,
      upper_limit_unit: this._upper_limit_unit,
      filter: this._filter
    });
  }
}
