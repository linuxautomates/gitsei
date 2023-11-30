import { sanitizeObject } from "../utils/commonUtils";
import { defaultSections } from "./trellisProfile.helper";

export const FEATURES_WITH_EFFORT_PROFILE = [
  "NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH",
  "NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH"
];

export class RestTrellisScoreProfile {
  _id?: string;
  _name?: string;
  _isValidName: boolean;
  _default_profile?: boolean;
  _description?: string;
  _sections?: RestTrellisProfileSections[];
  _effort_investment_profile_id?: string;
  _settings?: Record<any, any>;
  _workspace_to_org: Record<string, Array<string>>;
  _predefined_profile: boolean;
  _updated_at?: string;

  constructor(restData: any = null) {
    this._id = undefined;
    this._name = undefined;
    this._default_profile = false;
    this._description = undefined;
    this._sections = defaultSections.map((section: any) => new RestTrellisProfileSections(section));
    this._effort_investment_profile_id = undefined;
    this._settings = {};
    this._isValidName = true;
    this._workspace_to_org = {
      w_: []
    };
    this._predefined_profile = false;
    this._updated_at = undefined;
    if (restData) {
      this._id = (restData || ({} as any))?.id;
      this._name = (restData || ({} as any))?.name;
      this._default_profile = (restData || ({} as any))?.default_config || false;
      this._description = (restData || ({} as any))?.description;
      this._sections = ((restData || ({} as any))?.sections || []).map(
        (section: any) => new RestTrellisProfileSections(section)
      );
      this._effort_investment_profile_id = (restData || ({} as any))?.effort_investment_profile_id;
      this._settings = (restData || ({} as any))?.settings;
      this._isValidName = (restData || ({} as any))?.hasOwnProperty("isValidName") ? restData.isValidName : true;
      this._workspace_to_org = (restData || ({} as any))?.workspace_to_org || { w_: [] };
      this._predefined_profile = (restData || ({} as any))?.predefined_profile;
      this._updated_at = (restData || ({} as any)).updated_at;
    }
  }

  get updated_at() {
    return this._updated_at;
  }

  set updated_at(updatedAt) {
    this._updated_at = updatedAt;
  }

  get predefined_profile() {
    return this._predefined_profile;
  }

  set predefined_profile(predefined_profile) {
    this._predefined_profile = predefined_profile;
  }

  get workspace_to_org() {
    return this._workspace_to_org;
  }

  set workspace_to_org(workspace_to_orgMap) {
    this._workspace_to_org = workspace_to_orgMap;
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

  get default__profile() {
    return this._default_profile;
  }

  set default__profile(default_profile) {
    this._default_profile = default_profile;
  }

  get sections() {
    return (this._sections || [])?.sort(
      (a: RestTrellisProfileSections, b: RestTrellisProfileSections) => a?.order - b?.order
    );
  }

  set sections(sections) {
    this._sections = sections?.map((feature: any) => new RestTrellisProfileSections(feature));
  }

  get effort_investment_profile_id() {
    return this._effort_investment_profile_id;
  }

  set settings(settings) {
    this._settings = settings;
  }

  get settings() {
    return this._settings;
  }

  set effort_investment_profile_id(effort_investment_profile_id) {
    this._effort_investment_profile_id = effort_investment_profile_id;
  }

  get validate() {
    let isValid = !!(this._isValidName && this._name?.length);
    if (this._effort_investment_profile_id) {
      isValid = isValid && this.sections.reduce<boolean>((acc, section) => acc && section.validate, true);
    }
    return isValid;
  }

  get json() {
    let associated_ou_ref_ids: Array<string> = [];
    Object.values(this._workspace_to_org).forEach((orgList: Array<string>) => {
      associated_ou_ref_ids = [...associated_ou_ref_ids, ...orgList];
    });
    return sanitizeObject({
      id: this._id,
      name: this._name,
      description: this._description,
      default_profile: this._default_profile,
      sections: this._sections?.map(section => section.json),
      effort_investment_profile_id: this._effort_investment_profile_id,
      settings: this._settings,
      associated_ou_ref_ids,
      predefined_profile: this._predefined_profile,
      workspace_to_org: this._workspace_to_org
    });
  }
}

export class RestTrellisProfileSections {
  _name?: string;
  _description?: string;
  _order?: number;
  _features?: RestTrellisProfileFeatures[];
  _enabled?: boolean;
  _weight?: number;

  constructor(restData: any = null) {
    this._name = undefined;
    this._description = undefined;
    this._order = 0;
    this._features = [];
    this._enabled = undefined;
    this._weight = undefined;

    if (restData) {
      this._name = (restData || ({} as any))?.name;
      this._description = (restData || ({} as any))?.description;
      this._order = (restData || ({} as any))?.order;
      this._enabled = (restData || ({} as any))?.enabled;
      this._weight = (restData || ({} as any))?.weight;
      this._features = ((restData || ({} as any))?.features || []).map(
        (feature: any) => new RestTrellisProfileFeatures(feature)
      );
    }
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get description() {
    return this._description;
  }

  set description(desc) {
    this._description = desc;
  }

  get order() {
    return this._order || 0;
  }

  set order(order) {
    this._order = order;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(enabled) {
    this._enabled = enabled;
  }

  get weight() {
    return this._weight;
  }

  set weight(weight) {
    this._weight = weight;
  }

  get features() {
    return (this._features || [])?.sort(
      (a: RestTrellisProfileFeatures, b: RestTrellisProfileFeatures) => a?.order - b?.order
    );
  }

  set features(features) {
    this._features = features?.map((feature: any) => new RestTrellisProfileFeatures(feature));
  }

  get validate() {
    return this.features.reduce((acc, feature) => acc && feature.validate, true);
  }

  get json() {
    return sanitizeObject({
      name: this._name,
      description: this._description,
      order: this._order,
      weight: this._weight,
      enabled: this._enabled,
      features: this._features?.map(feature => feature.json)
    });
  }
}

export class RestTrellisProfileFeatures {
  _name?: string;
  _description?: string;
  _order?: number;
  _type?: string;
  _lower_limit_percentage?: number;
  _upper_limit_percentage?: number;
  _max_value?: number;
  _slow_to_good_is_ascending?: boolean;
  _enabled?: boolean;
  _feature_max_value_text?: string;
  _feature_unit?: string;
  _ticket_categories?: string[];

  constructor(restData: any = null) {
    this._name = undefined;
    this._description = undefined;
    this._order = 0;
    this._type = undefined;
    this._lower_limit_percentage = undefined;
    this._upper_limit_percentage = undefined;
    this._max_value = undefined;
    this._slow_to_good_is_ascending = undefined;
    this._enabled = undefined;
    this._feature_max_value_text = undefined;
    this._feature_unit = undefined;
    this._ticket_categories = [];

    if (restData) {
      this._name = (restData || ({} as any))?.name;
      this._description = (restData || ({} as any))?.description;
      this._order = (restData || ({} as any))?.order;
      this._type = (restData || ({} as any))?.type;
      this._lower_limit_percentage = (restData || ({} as any))?.lower_limit_percentage;
      this._upper_limit_percentage = (restData || ({} as any))?.upper_limit_percentage;
      this._max_value = (restData || ({} as any))?.max_value;
      this._slow_to_good_is_ascending = (restData || ({} as any))?.slow_to_good_is_ascending;
      this._enabled = (restData || ({} as any))?.enabled;
      this._feature_max_value_text = (restData || ({} as any))?.feature_max_value_text;
      this._feature_unit = (restData || ({} as any))?.feature_unit;
      this._ticket_categories = (restData || ({} as any))?.ticket_categories;
    }
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get description() {
    return this._description;
  }

  set description(desc) {
    this._description = desc;
  }

  get order() {
    return this._order || 0;
  }

  set order(order) {
    this._order = order;
  }

  get type() {
    return this._type;
  }

  set type(type) {
    this._type = type;
  }

  get lower_limit_percentage() {
    return this._lower_limit_percentage;
  }

  set lower_limit_percentage(lower_limit_percentage) {
    this._lower_limit_percentage = lower_limit_percentage;
  }

  get upper_limit_percentage() {
    return this._upper_limit_percentage;
  }

  set upper_limit_percentage(upper_limit_percentage) {
    this._upper_limit_percentage = upper_limit_percentage;
  }

  get max_value() {
    return this._max_value;
  }

  set max_value(max_value) {
    this._max_value = max_value;
  }

  get slow_to_good_is_ascending() {
    return this._slow_to_good_is_ascending;
  }

  set slow_to_good_is_ascending(slow_to_good_is_ascending) {
    this._slow_to_good_is_ascending = slow_to_good_is_ascending;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(enabled) {
    this._enabled = enabled;
  }

  get feature_max_value_text() {
    return this._feature_max_value_text;
  }

  set feature_max_value_text(feature_max_value_text) {
    this._feature_max_value_text = feature_max_value_text;
  }

  get feature_unit() {
    return this._feature_unit;
  }

  set feature_unit(feature_unit) {
    this._feature_unit = feature_unit;
  }

  get ticket_categories() {
    return this._ticket_categories;
  }

  set ticket_categories(ticket_categories) {
    this._ticket_categories = ticket_categories;
  }

  get validate() {
    if (FEATURES_WITH_EFFORT_PROFILE.includes(this._type || "") && this._enabled) {
      if (!(this._ticket_categories || []).length) {
        return false;
      }
    }
    return true;
  }

  get json() {
    return sanitizeObject({
      name: this._name,
      description: this._description,
      order: this._order,
      type: this._type,
      lower_limit_percentage: this.lower_limit_percentage,
      upper_limit_percentage: this._upper_limit_percentage,
      max_value: this._max_value,
      slow_to_good_is_ascending: this._slow_to_good_is_ascending,
      enabled: this._enabled,
      feature_max_value_text: this._feature_max_value_text,
      feature_unit: this._feature_unit,
      ticket_categories: this._ticket_categories
    });
  }
}
