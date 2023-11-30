import { DORA_METRIC_CONFIGURABLE_DEFINITIONS } from "configurations/pages/lead-time-profiles/helpers/constants";
import { STARTS_WITH } from "dashboard/graph-filters/components/tag-select/TagSelect";
import { sanitizeObject } from "utils/commonUtils";
import { DORAConfigValues } from "./DORAConfigValues";

export class DORAConfigDefinition {
  _source_branch: DORAConfigValues;
  _target_branch: DORAConfigValues;
  _commit_branch: DORAConfigValues;
  _tags: DORAConfigValues;
  _labels: DORAConfigValues;
  _defaultValue: string;
  _requiredFields: string[]

  constructor(restData: any = null, defaultValue: string = "", hiddenKeys: string[] = [], requiredFields: string[] = []) {
    this._defaultValue = defaultValue;
    this._requiredFields = requiredFields;
    const checkedDefaultValue = { [STARTS_WITH]: [defaultValue] };
    const unCheckedDefaultValue = { checked: false, key: STARTS_WITH, value: defaultValue };
    if (!restData) {
      this._source_branch = hiddenKeys.includes("source_branch")
        ? new DORAConfigValues(unCheckedDefaultValue)
        : new DORAConfigValues(checkedDefaultValue);
      this._target_branch = hiddenKeys.includes("target_branch")
        ? new DORAConfigValues(unCheckedDefaultValue)
        : new DORAConfigValues(checkedDefaultValue);
      this._commit_branch = hiddenKeys.includes("commit_branch")
        ? new DORAConfigValues(unCheckedDefaultValue)
        : new DORAConfigValues(checkedDefaultValue);
      this._tags = hiddenKeys.includes("tags")
        ? new DORAConfigValues(unCheckedDefaultValue)
        : new DORAConfigValues(checkedDefaultValue);
      this._labels = hiddenKeys.includes("labels")
        ? new DORAConfigValues(unCheckedDefaultValue)
        : new DORAConfigValues(checkedDefaultValue);
    } else {
      this._source_branch = restData?.hasOwnProperty("source_branch")
        ? new DORAConfigValues(restData.source_branch)
        : new DORAConfigValues(unCheckedDefaultValue);
      this._target_branch = restData?.hasOwnProperty("target_branch")
        ? new DORAConfigValues(restData.target_branch)
        : new DORAConfigValues(unCheckedDefaultValue);
      this._commit_branch = restData?.hasOwnProperty("commit_branch")
        ? new DORAConfigValues(restData.commit_branch)
        : new DORAConfigValues(unCheckedDefaultValue);
      this._tags = restData?.hasOwnProperty("tags")
        ? new DORAConfigValues(restData.tags)
        : new DORAConfigValues(unCheckedDefaultValue);
      this._labels = restData?.hasOwnProperty("labels")
        ? new DORAConfigValues(restData.labels)
        : new DORAConfigValues(unCheckedDefaultValue);
    }
  }

  get source_branch() {
    return this._source_branch;
  }
  set source_branch(value: any) {
    const newObj = new DORAConfigValues({
      checked: value.checked,
      value: value.value,
      key: value.key
    });
    this._source_branch = newObj;
  }

  get target_branch() {
    return this._target_branch;
  }
  set target_branch(value: any) {
    const newObj = new DORAConfigValues({
      checked: value.checked,
      value: value.value,
      key: value.key
    });
    this._target_branch = newObj;
  }

  get commit_branch() {
    return this._commit_branch;
  }
  set commit_branch(value: any) {
    const newObj = new DORAConfigValues({
      checked: value.checked,
      value: value.value,
      key: value.key
    });
    this._commit_branch = newObj;
  }

  get tags() {
    return this._tags;
  }
  set tags(value: any) {
    const newObj = new DORAConfigValues({
      checked: value.checked,
      value: value.value,
      key: value.key
    });
    this._tags = newObj;
  }

  get labels() {
    return this._labels;
  }
  set labels(value: any) {
    const newObj = new DORAConfigValues({
      checked: value.checked,
      value: value.value,
      key: value.key
    });
    this._labels = newObj;
  }

  get json() {
    return sanitizeObject({
      source_branch: this._source_branch.json,
      target_branch: this._target_branch.json,
      commit_branch: this._commit_branch.json,
      tags: this._tags.json,
      labels: this._labels.json
    });
  }

  get postData() {
    return sanitizeObject({
      source_branch: this._source_branch.postData,
      target_branch: this._target_branch.postData,
      commit_branch: this._commit_branch.postData,
      tags: this._tags.postData,
      labels: this._labels.postData
    });
  }

  get hasError() {
    const defintion = this.json;
    let error = true;
    Object.keys(DORA_METRIC_CONFIGURABLE_DEFINITIONS).forEach(key => {
      if (defintion[key].checked && defintion[key].value) {
        error = false;
      }
    });
    if (!error && this._requiredFields && this._requiredFields.length > 0) {
      error = this._requiredFields.some((requiredField: string) => !defintion[requiredField].checked || !defintion[requiredField].value);
    }
    return error;
  }
}
