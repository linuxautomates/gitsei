import { filter, isArray, isNull, isUndefined, pickBy, isNaN, forEach, isString, unset, get, cloneDeep } from "lodash";
import { v1 as uuid } from "uuid";
import { isMoment } from "moment";

// Use it for sanitizing object completely to any depth
export const sanitizeObjectCompletely = (object: any) => {
  if (typeof object !== "object" || Array.isArray(object)) {
    return object;
  }

  let keysToUnset: string[] = [];
  forEach(Object.keys(object), key => {
    let value = object[key];
    if (typeof value === "string" && value === "") keysToUnset.push(key);
    if (typeof value === "object" && !Array.isArray(value)) {
      let newObj = sanitizeObjectCompletely(value);
      if (!Object.keys(newObj).length) {
        keysToUnset.push(key);
      } else {
        object[key] = newObj;
      }
    }

    if (Array.isArray(value)) {
      if (!value.length) {
        keysToUnset.push(key);
      } else if (typeof value[0] === "object" && !isMoment(value[0])) {
        for (let i = 0; i < value.length; i++) {
          value[i] = sanitizeObjectCompletely(value[i]);
        }
        value = value.filter(obj => Object.keys(obj).length);
        if (!value.length) {
          keysToUnset.push(key);
        }
      }
    }
  });

  forEach(keysToUnset, key => {
    unset(object, key);
  });

  return object;
};

export const findSumInArray = (array: any[], key: string) => {
  return array.reduce((acc, curr) => {
    return acc + curr[key];
  }, 0);
};

export const sanitizeObject = (data: any) => {
  return pickBy(data, (v: any) => (isArray(v) ? !!v.length : v !== null && v !== undefined && v !== ""));
};

export const renameKey = (data: { [key: string]: any }, oldKey: string, newKey: string) => ({
  ...(data || {}),
  [oldKey]: undefined,
  [newKey]: (data || {})[oldKey]
});

export const isSanitizedValue = (value: any) => !isNull(value) && !isUndefined(value) && value !== "";

//get truthy values (!null and !undefined) from an array of values
export const getTruthyValues = (values: any[]) => filter(values || [], (value: any) => isSanitizedValue(value));

export const isSanitizedArray = (array: any[]) =>
  (array || []).length > 0 && (array || []).length === getTruthyValues(array).length;

export const stringToNumber = (value: any) => (!isNaN(parseInt(value)) ? parseInt(value) : value);

export const mapStringNumberKeysToNumber = (data: any) => {
  const _data: any = {};
  Object.keys(data || {}).forEach(key => (_data[key] = stringToNumber(data[key])));
  return _data;
};

export const removeEmptyKeys = (filters: any) => {
  return Object.keys(filters).reduce((acc: any, next: any) => {
    // Object.keys({ key: "value" }) => ["key"] // Ok
    // Object.keys("sup") => ["0", "1", "2"] // Ok
    // Object.keys(34) => [] // Bad!!
    // Object.keys(false) => [] // Bad!!
    if (
      filters[next]?.constructor === Object && // Prevents the bad situations above.
      Object.keys(filters[next]).length === 0
    ) {
      delete filters[next];
      return { ...acc };
    }
    return { ...acc, [next]: filters[next] };
  }, {});
};

export const trimStringKeys = (filters: any) => {
  let updatedFilters: any = sanitizeObject(filters);
  forEach(Object.keys(updatedFilters), key => {
    updatedFilters[key] = isString(updatedFilters[key]) ? updatedFilters[key].trim() : updatedFilters[key];
  });
  return updatedFilters;
};

export const getNameWithUUID = (name: string) => `${name}_${uuid()}`;

export const compareFilters = (filter1: any, filter2: any) => {
  if (filter1 === undefined || filter1 === null || filter2 === undefined || filter2 === null) {
    return true;
  }

  let newFilter1 = JSON.parse(JSON.stringify(filter1));
  let newFilter2 = JSON.parse(JSON.stringify(filter2));

  Object.keys(newFilter1).forEach(key => {
    if (!newFilter2.hasOwnProperty(key)) {
      return false;
    }
    if (Array.isArray(newFilter1[key])) {
      let array1 = newFilter1[key];
      let array2 = newFilter2[key];
      if (array1.length !== array2.length) {
        return false;
      }
      array1.forEach((item: any) => {
        if (array2.filter((i: any) => i === item).length === 0) {
          return false;
        }
      });
    } else {
      if (newFilter1[key] !== newFilter2[key]) {
        return false;
      }
    }

    delete newFilter1[key];
    delete newFilter2[key];
  });
  return !(Object.keys(newFilter1).length > 0 || Object.keys(newFilter2).length > 0);
};

export const getFieldValueOrString = (item: any, field: string) => {
  if (typeof item === "string" || typeof item === "number") return item;
  if (isArray(item)) return "";
  return get(item, [field], "");
};

export const makeObjectKeyAsValue = (obj: Record<string, string>) => {
  return Object.keys(obj).reduce((acc: Record<string, string>, current: string) => {
    acc[obj[current]] = current;
    return acc;
  }, {});
};

/** Use this function to unset keys from an object */
export const unsetKeysFromObject = (keys: string[], data: any) => {
  const clonnedData = cloneDeep(data);
  forEach(keys, key => {
    unset(clonnedData, [key]);
  });
  return clonnedData;
};

/***
 * @return total by object kys
 * @param obj Object
 * @param dataKey String key which going to add up for total
 * */
export const objectSumByKey = (obj: Record<string, any>, dataKey: string) => {
  return Object.keys(obj).reduce((acc, key) => {
    if (obj[key]?.value) {
      acc = acc + obj[key]?.[dataKey];
    }
    return acc;
  }, 0);
};
