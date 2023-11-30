import envConfig from "env-config";
import ReactGA from "react-ga";
import LocalStoreService from "services/localStoreService";
import { AnalyticsActionType, AnalyticsCategoryType, GA_ANALYTICS_NON_TRACKABLE_EMAILS } from "./analytics.constants";

let GAConfig = {};
const node_env = envConfig.get("NODE_ENV");
if (node_env !== undefined && node_env === "development") {
  GAConfig = {
    debug: false, // Turn this on if you want logs in console for testing/debugging
    gaOptions: {}
  };
}

/**
 * This initializes GA for the application.
 * The script is loaded asynchronously here
 *
 * @param trackerId
 */
export const start = (trackerId = "") => {
  if (!trackerId) {
    trackerId = envConfig.get("GA_TRACKER") || "";
  }

  if (trackerId) {
    ReactGA.initialize(trackerId, GAConfig);
  }
};

export const emitEvent = (
  category: AnalyticsCategoryType,
  action: AnalyticsActionType,
  label?: string,
  value?: number
) => {
  const ls = new LocalStoreService();
  const userEmail = ls.getUserEmail();
  // not tracking GA Analitics for specific user emails
  if (userEmail && GA_ANALYTICS_NON_TRACKABLE_EMAILS.includes(userEmail)) {
    return;
  }
  ReactGA.event({ category, action, label, value });
};

export default { start };
