import { isArray, isObject, unset } from "lodash";

export function buildLink(item: any, url: string, application: string) {
  switch (application) {
    case "jira":
      return `${url}/browse/${item}`;
    case "github":
      return `${url}/${item.repo_cloud_id}/commit/${item.commit_sha}`;
    default:
  }
}

export function removeEmptyIntegrations(data: any) {
  const finalData: any = {};
  for (const key of Object.keys(data)) {
    // updating condition to add support for false values
    if (data?.[key] !== undefined && data?.[key] !== null && (data[key] + "")?.length) {
      finalData[key] = data[key];
    }
  }
  return finalData;
}

export function removeEmptyObject(data: any) {
  for (const key of Object.keys(data)) {
    if (
      isObject(data?.[key]) &&
      ((isArray(data?.key) && data?.[key]?.length === 0) || Object.keys(data?.[key] || {})?.length === 0)
    ) {
      unset(data, key);
    }
  }
  return data;
}
