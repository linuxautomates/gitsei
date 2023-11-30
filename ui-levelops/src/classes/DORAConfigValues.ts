import { STARTS_WITH } from "dashboard/graph-filters/components/tag-select/TagSelect";
import { sanitizeObject } from "utils/commonUtils";

export class DORAConfigValues {
  _checked: boolean;
  _value: string;
  _key: string;

  constructor(restData: any = null) {
    this._checked = true;
    this._value = "";
    this._key = STARTS_WITH;

    if (restData) {
      if (restData.hasOwnProperty("checked")) {
        this._checked = restData.checked;
        this._key = restData.key;
        this._value = restData.value;
      } else {
        const _restData: any = restData || {};
        this._checked = true;
        if (Object.keys(_restData || {}).length) {
          this._key = Object.keys(_restData)[0];
        }
        if (Object.values(_restData || {}).length) {
          this._value = (Object.values(_restData) as string[][])[0].join(",");
        }
      }
    } else {
      this._checked = false;
    }
  }

  get checked() {
    return this._checked;
  }
  set checked(value: boolean) {
    this._checked = value;
  }

  get key() {
    return this._key;
  }
  set key(value: string) {
    this._key = value;
  }

  get value() {
    return this._value;
  }
  set value(value: string) {
    this._value = value;
  }

  get json() {
    return sanitizeObject({
      checked: this._checked,
      key: this._key,
      value: this._value
    });
  }

  get postData() {
    if (this._checked) {
      return sanitizeObject({
        [this._key]: this._value?.split(",")
      });
    }
    return undefined;
  }
}
