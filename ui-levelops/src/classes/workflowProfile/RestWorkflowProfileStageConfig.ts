import { DORAConfigDefinition } from "classes/DORAConfigDefinition";
import { AcceptanceTimeUnit, TriggerEventType, VelocityConfigStage } from "classes/RestWorkflowProfile";
import { StageEndOptions } from "classes/StageEndOptions";
import { sanitizeObject } from "utils/commonUtils";
import { v1 as uuid } from "uuid";

export type sectionSelectedFilterType = {
  key: string;
  param: string;
  value: any;
  metadata?: any;
};

export class RestStageConfig {
  _id?: string;
  _name?: string;
  _description?: string;
  _order?: number;
  _event?: { type: string; values: any[]; params: any; scm_filters?: DORAConfigDefinition };
  _lower_limit_value?: number;
  _upper_limit_value?: number;
  _lower_limit_unit?: AcceptanceTimeUnit;
  _upper_limit_unit?: AcceptanceTimeUnit;
  _type?: VelocityConfigStage;
  _enabled?: boolean;
  _filter?: sectionSelectedFilterType[];

  constructor(restData: any = null, defaultValue: string = "", hiddenKeys: string[] = []) {
    this._id = undefined;
    this._name = undefined;
    this._description = undefined;
    this._order = undefined;
    this._event = {
      type: TriggerEventType.JIRA_STATUS,
      values: [],
      params: undefined,
      scm_filters: new DORAConfigDefinition(null, defaultValue, hiddenKeys)
    };
    this._lower_limit_value = 4;
    this._upper_limit_value = 11;
    this._lower_limit_unit = AcceptanceTimeUnit.DAYS;
    this._upper_limit_unit = AcceptanceTimeUnit.DAYS;
    this._type = undefined;
    this._enabled = true;
    this._event = {
      type: TriggerEventType.JIRA_STATUS,
      values: [],
      params: undefined,
      scm_filters:
        this._type === VelocityConfigStage.FIXED_STAGE
          ? new DORAConfigDefinition(null, defaultValue, hiddenKeys)
          : undefined
    };
    this._filter = [];

    if (restData) {
      this._id = ((restData || {}) as any)?.id;
      this._name = ((restData || {}) as any)?.name;
      this._description = ((restData || {}) as any)?.description;
      this._order = ((restData || {}) as any)?.order;
      this._type = ((restData || {}) as any)?.type;
      this._event = ((restData || {}) as any)?.event;
      if (this._event && this._type === VelocityConfigStage.FIXED_STAGE) {
        this._event = {
          ...this._event,
          scm_filters: this._event.scm_filters
            ? new DORAConfigDefinition(this._event.scm_filters)
            : new DORAConfigDefinition(null, defaultValue, hiddenKeys)
        };
      }
      this._lower_limit_value = ((restData || {}) as any)?.lower_limit_value || 4;
      this._upper_limit_value = ((restData || {}) as any)?.upper_limit_value || 11;
      this._lower_limit_unit = ((restData || {}) as any)?.lower_limit_unit || AcceptanceTimeUnit.DAYS;
      this._upper_limit_unit = ((restData || {}) as any)?.upper_limit_unit || AcceptanceTimeUnit.DAYS;
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

  get eventJson() {
    if (this.event?.scm_filters) {
      const scm_filters = this.event.scm_filters?.json;
      return sanitizeObject({
        ...this._event,
        scm_filters
      });
    }
    return sanitizeObject({
      ...this._event,
      scm_filters: undefined
    });
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
      event: this.eventJson,
      type: this._type,
      lower_limit_value: this._lower_limit_value,
      upper_limit_value: this._upper_limit_value,
      lower_limit_unit: this._lower_limit_unit,
      upper_limit_unit: this._upper_limit_unit,
      enabled: this._enabled,
      filter: this._filter
    });
  }

  get eventPostData() {
    if (
      this.event?.type === StageEndOptions.SCM_PR_SOURCE_BRANCH ||
      this.event?.type === StageEndOptions.SCM_PR_MERGED
    ) {
      const scm_filters = this.event?.scm_filters?.postData;
      return sanitizeObject({
        ...this._event,
        scm_filters
      });
    }
    return sanitizeObject({
      ...this._event,
      scm_filters: undefined
    });
  }
  get postData() {
    return sanitizeObject({
      order: this._order,
      name: this._name,
      description: this._description,
      event: this.eventPostData,
      lower_limit_value: this._lower_limit_value,
      upper_limit_value: this._upper_limit_value,
      lower_limit_unit: this._lower_limit_unit,
      upper_limit_unit: this._upper_limit_unit,
      filter: this._filter
    });
  }
}

export class RestDevelopmentStageConfig {
  _issue_management_integrations: string[];
  _pre_development_custom_stages?: RestStageConfig[];
  _fixed_stages?: RestStageConfig[];
  _post_development_custom_stages?: RestStageConfig[];
  _event: "ticket_created" | "commit_created" | "api_event";
  _fixed_stages_enabled?: boolean;
  _integration_id: string;

  constructor(
    restData: any | undefined,
    application?: string[],
    basicStages?: any,
    defaultScmValue: string = "",
    profileStage: string = ""
  ) {
    if (!restData) {
      this._pre_development_custom_stages = [];
      this._fixed_stages = [];
      this._post_development_custom_stages = [];
      this._fixed_stages_enabled = true;
      this._issue_management_integrations = [];
      if (application) {
        this._issue_management_integrations = application;
        this._event = "ticket_created";
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
      this._integration_id = restData?.integration_id;
      this._issue_management_integrations = restData?.issue_management_integrations;
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

  get issue_management_integrations() {
    return this._issue_management_integrations;
  }
  set issue_management_integrations(integrationType) {
    this._issue_management_integrations = integrationType;
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
      issue_management_integrations: this._issue_management_integrations,
      pre_development_custom_stages: (this._pre_development_custom_stages || []).map(stage => stage.json),
      fixed_stages: (this._fixed_stages || []).map(stage => stage.json),
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.json),
      fixed_stages_enabled: this._fixed_stages_enabled,
      event: this._event,
      ...this.convertEvent(this._event)
    });
  }

  get postData() {
    return sanitizeObject({
      integration_id: this._integration_id,
      issue_management_integrations: this._issue_management_integrations,
      pre_development_custom_stages:
        this._event === "commit_created"
          ? []
          : (this._pre_development_custom_stages || []).map(stage => stage.postData),
      fixed_stages: this._fixed_stages_enabled
        ? (this._fixed_stages || []).filter(stage => stage.enabled).map(stage => stage.postData)
        : [],
      post_development_custom_stages: (this._post_development_custom_stages || []).map(stage => stage.postData),
      fixed_stages_enabled: this._fixed_stages_enabled,
      event: this._event,
      ...this.convertEvent(this._event)
    });
  }
}
