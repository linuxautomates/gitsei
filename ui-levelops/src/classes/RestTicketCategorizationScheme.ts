import { get } from "lodash";
import { v1 as uuid } from "uuid";
import { currentPrioritiesInitialState } from "configurations/pages/ticket-categorization/constants-new/constants";
import {
  NEW_CATEGORY_ID,
  UNCATEGORIZED_ID_SUFFIX
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import {
  getInitialColorMapping,
  getProfileCategoriesColorMapping
} from "configurations/pages/ticket-categorization/helper/bussinessAlignmentColorPicker.helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { sanitizeObject } from "utils/commonUtils";
import { IntegrationTypes } from "constants/IntegrationTypes";

const DEFAULT_COLOR = "#FC91AA";
export class RestTicketCategorizationScheme {
  _id?: string;
  _name?: string;
  _default_scheme?: boolean;
  _description?: string;
  _created_at?: string;
  _updated_at?: string;
  _categories?: RestTicketCategorizationCategory[];
  _draft?: boolean;
  _issue_management_integration?: string;
  _current_priorities_mapping?: basicMappingType<boolean>;
  _uncategorized_color?: string;
  _goal?: basicMappingType<any>;
  _categoryColorMapping?: basicMappingType<boolean>;

  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._default_scheme = false;
    this._description = undefined;
    this._created_at = undefined;
    this._updated_at = undefined;
    this._categories = [];
    this._draft = false;
    this._issue_management_integration = IntegrationTypes.JIRA;
    this._current_priorities_mapping = currentPrioritiesInitialState;
    this._uncategorized_color = DEFAULT_COLOR;
    this._goal = {};
    this._categoryColorMapping = getProfileCategoriesColorMapping();

    if (restData) {
      this._id = ((restData || {}) as any)?.id;
      this._name = ((restData || {}) as any)?.name;
      this._description = ((restData || {}) as any)?.config?.description;
      this._default_scheme = ((restData || {}) as any)?.default_scheme;
      this._created_at = ((restData || {}) as any)?.created_at;
      this._updated_at = ((restData || {}) as any)?.updated_at;
      this._issue_management_integration = ((restData || {}) as any)?.config?.integration_type || IntegrationTypes.JIRA;
      this._current_priorities_mapping =
        ((restData || {}) as any)?.config?.active_work?.issues || currentPrioritiesInitialState;
      this._categories = Object.keys(((restData || {}) as any)?.config?.categories || {}).map(
        (c: any) => new RestTicketCategorizationCategory(((restData || {}) as any)?.config?.categories?.[c] || {})
      );
      this._uncategorized_color = (
        ((restData || {}) as any)?.config?.uncategorized ?? { color: DEFAULT_COLOR, goal: {} }
      ).color;
      this._goal =
        (((restData || {}) as any)?.config?.uncategorized ?? { color: DEFAULT_COLOR, goals: {} }).goals ?? {};

      const categories = Object.values(((restData || {}) as any)?.config?.categories || {}) ?? [];
      const uncategorized = ((restData || {}) as any)?.config?.uncategorized ?? { color: DEFAULT_COLOR, goal: {} };
      this._categoryColorMapping = getInitialColorMapping([...categories, uncategorized]);
    }

    this.addCategory = this.addCategory.bind(this);
    this.removeCategory = this.removeCategory.bind(this);
    this.getUncategorizedSection = this.getUncategorizedSection.bind(this);
    this.getTopUnusedColor = this.getTopUnusedColor.bind(this);
    this.getSlicedCategoryColors = this.getSlicedCategoryColors.bind(this);
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get draft() {
    return this._draft;
  }

  set draft(value) {
    this._draft = value;
  }

  get name() {
    return this._name;
  }

  get issue_management_integration() {
    return this._issue_management_integration;
  }

  set issue_management_integration(value) {
    this._issue_management_integration = value;
  }

  set name(name) {
    this._name = name;
  }

  get defaultScheme() {
    return this._default_scheme;
  }

  set defaultScheme(defaultScheme) {
    this._default_scheme = defaultScheme;
  }

  get description() {
    return this._description;
  }

  set description(description) {
    this._description = description;
  }

  get created_at() {
    return this._created_at;
  }

  get updated_at() {
    return this._updated_at;
  }

  get current_priorities_mapping() {
    return this._current_priorities_mapping ?? currentPrioritiesInitialState;
  }

  set current_priorities_mapping(priorityMapping: basicMappingType<boolean>) {
    this._current_priorities_mapping = priorityMapping;
  }

  get categories() {
    return this._categories;
  }

  set categories(categories) {
    this._categories = (categories || []).map((category, index) => {
      category.index = index + 1;
      return new RestTicketCategorizationCategory(category as any);
    });
  }

  get validate() {
    return (this._name || "").length > 0;
  }

  get uncategorized_color() {
    return this._uncategorized_color ?? DEFAULT_COLOR;
  }

  set uncategorized_color(color: string) {
    this.updateCategoryColorMapping(this._uncategorized_color ?? "", color);
    this._uncategorized_color = color;
  }

  get goal() {
    return this._goal ?? {};
  }

  addCategory(categoryData: any) {
    const categoryId = categoryData?.id;
    const existingCategory = this.categories?.find(category => category.id === categoryId);
    if (categoryId && existingCategory) {
      this._categories = this.categories?.filter(category => category.id !== categoryId);
      this.categories?.push(new RestTicketCategorizationCategory(categoryData));
    } else {
      this.categories?.push(new RestTicketCategorizationCategory(categoryData));
    }
  }

  removeCategory(categoryId: any) {
    this._categories = this.categories?.filter(category => category.id !== categoryId);
  }

  getUncategorizedSection() {
    const uncategorizedCategory = (this.categories || []).find(category => category?.id === UNCATEGORIZED_ID_SUFFIX);
    return sanitizeObject({
      color: this.uncategorized_color ?? DEFAULT_COLOR,
      goals: get((uncategorizedCategory || {})?.json, ["goals"], this.goal)
    });
  }

  get categoriesMapping() {
    return this._categoryColorMapping;
  }

  getTopUnusedColor() {
    let topUnusedColor = undefined;
    const colors = Object.keys(this.categoriesMapping ?? {});
    for (let i = 0; i < colors?.length; i++) {
      if ((this.categoriesMapping ?? {})[colors[i]]) {
        topUnusedColor = colors[i];
        break;
      }
    }
    return topUnusedColor;
  }

  getSlicedCategoryColors() {
    let allAvailableColors = Object.keys(this.categoriesMapping ?? {}).filter(
      color => (this.categoriesMapping ?? {})[color]
    );
    return allAvailableColors.slice(0, 49);
  }

  updateCategoryColorMapping(prevColor: string, curColor: string) {
    (this._categoryColorMapping ?? {})[prevColor] = true;
    (this._categoryColorMapping ?? {})[curColor] = false;
  }

  get json() {
    return sanitizeObject({
      id: this._id,
      name: this._name,
      default_scheme: this._default_scheme,
      config: {
        description: this._description,
        integration_type: this._issue_management_integration,
        active_work: { issues: this._current_priorities_mapping },
        categories: (this._categories || []).reduce((acc, category) => {
          if (category.index !== undefined) {
            acc = { ...acc, [category.index?.toString()]: category.json };
          }
          return acc;
        }, {}),
        categoryColorMapping: this.categoriesMapping,
        uncategorized: this.getUncategorizedSection()
      }
    });
  }
}

export class RestTicketCategorizationCategory {
  _id?: string;
  _name?: string;
  _description?: string;
  _index?: number;
  _filter?: any;
  _background_color?: string;
  _ideal_range?: string[];
  _enabled?: boolean;
  _metadata?: any;

  constructor(restData: any) {
    this._id = undefined;
    this._name = undefined;
    this._description = undefined;
    this._index = undefined;
    this._filter = {};
    this._background_color = DEFAULT_COLOR;
    this._ideal_range = undefined;
    this._enabled = true;
    this._metadata = {};

    if (restData) {
      this._id = ((restData || {}) as any)?.id;
      this._name = ((restData || {}) as any)?.name;
      this._description = ((restData || {}) as any)?.description;
      this._index = ((restData || {}) as any)?.index;
      this._filter = ((restData || {}) as any)?.filter ?? {};
      this._background_color = ((restData || {}) as any)?.color;
      this._ideal_range = [
        get(restData || ({} as any), ["goals", "ideal_range", "min"]),
        get(restData || ({} as any), ["goals", "ideal_range", "max"])
      ];
      this._enabled = get(restData || ({} as any), ["goals", "enabled"]) ?? true;
      this._metadata = restData?.metadata;
    }
    this.getAcceptableRange = this.getAcceptableRange.bind(this);
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

  set background_color(value: string) {
    this._background_color = value;
  }

  get background_color() {
    return this._background_color || DEFAULT_COLOR;
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

  get index() {
    return this._index;
  }

  set index(value) {
    this._index = value;
  }

  get filter() {
    return this._filter ?? {};
  }

  set filter(value) {
    this._filter = value;
  }

  get ideal_range() {
    return this._ideal_range;
  }

  set ideal_range(value) {
    this._ideal_range = value;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(value) {
    this._enabled = value;
  }

  get metadata() {
    return this._metadata;
  }

  set metadata(value) {
    this._metadata = value;
  }

  getAcceptableRange() {
    if (
      !this.ideal_range ||
      !this.ideal_range?.length ||
      isNaN(parseInt(this.ideal_range[0])) ||
      isNaN(parseInt(this.ideal_range[1]))
    )
      return undefined;
    let min = this.ideal_range[0],
      max = this.ideal_range[1];
    let lengthOfRange = Math.abs(parseInt(max) - parseInt(min));
    return {
      min: Math.max(0, parseInt(min) - lengthOfRange).toString(),
      max: Math.min(100, parseInt(max) + lengthOfRange).toString()
    };
  }

  get json() {
    return sanitizeObject({
      id: [NEW_CATEGORY_ID, undefined, "undefined"].includes(this._id) ? uuid() : this._id,
      index: this._index,
      name: this._name,
      description: this._description,
      filter: this._filter ?? {},
      color: this.background_color,
      goals: {
        enabled: this._enabled,
        ideal_range: {
          min: this.ideal_range?.[0],
          max: this.ideal_range?.[1]
        },
        acceptable_range: this.getAcceptableRange()
      },
      metadata: this._metadata
    });
  }
}
