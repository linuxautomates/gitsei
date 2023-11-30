import { getBaseUrl, CONFIG_TABLE_ROUTES } from "constants/routePaths";
import { dateRangeFilterValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { get } from "lodash";

export const levelopsTableReportOpenReportHelper = (args: {
  widgetMetadata: any;
  dashboardMetadata: any;
  filters: any;
  ouId: string;
}) => {
  const { widgetMetadata, ouId, filters, dashboardMetadata } = args;
  const tableId = get(widgetMetadata, ["tableId"], "");
  let url = `${getBaseUrl()}${CONFIG_TABLE_ROUTES.EDIT}?id=${tableId}`;
  if (!!ouId) {
    filters["ou_id"] = ouId;
  }
  if (Object.keys(filters).length) {
    const dashboardTimeKeysObject = get(widgetMetadata, ["dashBoard_time_keys"], {});
    if (Object.keys(dashboardTimeKeysObject)?.length > 0) {
      Object.keys(dashboardTimeKeysObject).forEach(key => {
        if (!!dashboardTimeKeysObject[key]?.use_dashboard_time) {
          const dashboardTimeRangeValue = get(dashboardMetadata, ["dashboard_time_range_filter"], undefined);
          filters[key] = dateRangeFilterValue(dashboardTimeRangeValue);
        }
      });
    }
    const tableFilters = JSON.stringify(filters);
    url = `${url}&filters=${tableFilters}&searchType=equal`;
  }

  return url;
};

/**
 * It takes a string that looks like a markdown link and returns an object with the title and url
 * @param {string} str - The string to extract the link and title from.
 * @returns An object with two properties: title and url.
 */
export function extractLinkAndTitleFromMarkdownUrl(str: string): { title: string; url: string; isValidUrl: boolean } {
  let isValidUrl = false;
  const lastTitleIndex = str.lastIndexOf("]");
  const title = str.substring(1, lastTitleIndex);
  const urlStartIndex = str.indexOf("(") + 1;
  const urlLastIndex = str.lastIndexOf(")");
  const url = str.substring(urlStartIndex, urlLastIndex);
  try {
    if (new URL(url)) {
      isValidUrl = true;
    }
  } catch (e) {
    isValidUrl = false;
  }
  return { title, url, isValidUrl };
}
