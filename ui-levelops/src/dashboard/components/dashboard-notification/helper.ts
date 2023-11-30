import { capitalize, get } from "lodash";
import { AddDashboardActionMenuType, DashboardBannerState, DashboardNotificationType } from "./constant";
import { WebRoutes } from "../../../routes/WebRoutes";
import { ONBOARDING_TEXT, DORA_DASHBOARD_CREATED } from "./constant";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";

export interface tenantItem {
  id: string;
  state: string;
  created_at: number;
}
export interface TenantState {
  records: Array<tenantItem>;
}

export const secondaryFilterExist = (metaData: any) => {
  if (
    metaData?.effort_investment_profile ||
    metaData?.effort_investment_unit ||
    isDashboardTimerangeEnabled(metaData || {})
  ) {
    return true;
  }
  return false;
};

export const getAddDashboardActionMenuLabel = (key: AddDashboardActionMenuType) => {
  const splitKeys = key.split("_");
  return `${capitalize(splitKeys[0])} ${capitalize(splitKeys[1])}`;
};

export const getDashboardBannerState = (
  dashboard: any,
  isTrialUser: boolean,
  tenantState: TenantState,
  tenantIntegrations: any[]
) => {
  let isDoraDashboardReady = false,
    dashboards = {} || undefined;

  const integrationIds = tenantIntegrations;
  let bannerState = DashboardBannerState.NONE;
  if (tenantState?.records?.length) {
    dashboards = tenantState?.records?.find(item => {
      return item.state === DORA_DASHBOARD_CREATED;
    });
    if (dashboards && Object.keys(dashboards).length) {
      isDoraDashboardReady = true;
    }
  }
  if (isTrialUser) {
    if (integrationIds?.length === 0) {
      bannerState = DashboardBannerState.PRE_INTEGRATION;
    } else if (integrationIds?.length >= 1 && !isDoraDashboardReady) {
      bannerState = DashboardBannerState.PRE_INGESTION;
    } else if (integrationIds?.length >= 1 && isDoraDashboardReady) {
      bannerState = DashboardBannerState.POST_INGESTION;
    }
  }
  return bannerState;
};

export const TYPE_ONBOARDING_DEMO_DASHBOARD = "ONBOARDING_DEMO_DASHBOARD";
export const TYPE_ONBOARDING_INTEGRATION_INSTALLED = "ONBOARDING_INTEGRATION_INSTALLED";
export const TYPE_ONBOARDING_DASHBOARD_READY = "ONBOARDING_DASHBOARD_READY";

export const NOTIFICATION_TYPES = [
  TYPE_ONBOARDING_DEMO_DASHBOARD,
  TYPE_ONBOARDING_INTEGRATION_INSTALLED,
  TYPE_ONBOARDING_DASHBOARD_READY
];

export const getDashboardBannerNotification = (
  type: DashboardNotificationType,
  dashboard: any,
  isTrialUser: boolean,
  tenantState: any,
  tenantIntegrations: any[]
) => {
  // For now, we are removing all onboarding related banner notifications
  return [];

  let banner = [];
  switch (type) {
    case DashboardNotificationType.ONBOARDING: {
      let bannerState = getDashboardBannerState(dashboard, isTrialUser, tenantState, tenantIntegrations);
      switch (bannerState) {
        case DashboardBannerState.PRE_INTEGRATION: {
          banner.push({
            type: TYPE_ONBOARDING_DEMO_DASHBOARD,
            message: "You're viewing demo data. View your own data by adding an integration.",
            actionText: "Add Integration",
            actionUrl: WebRoutes?.dashboard?.banner(),
            badge: {
              icon: ONBOARDING_TEXT
            }
          });
          return banner;
        }
        case DashboardBannerState.PRE_INGESTION: {
          banner.push({
            type: TYPE_ONBOARDING_INTEGRATION_INSTALLED,
            message: "Your integration was successful. While we create your insight, check out the product.",
            actionUrl: ``, //new tour page
            badge: {
              icon: ONBOARDING_TEXT
            }
          });
          return banner;
        }
        case DashboardBannerState.POST_INGESTION: {
          banner.push({
            type: TYPE_ONBOARDING_DASHBOARD_READY,
            message:
              "Your insight is ready. Explore it and if you have any questions, schedule time with a success architect.",
            actionText: "Talk to an Excellence Architect",
            actionUrl: `https://calendly.com/d/dhr-5xs-x2w/talk-to-an-excellence-architect`,
            badge: {
              icon: ONBOARDING_TEXT
            }
          });
          return banner;
        }
        default: {
          return [];
        }
      }
    }
    default: {
      return [];
    }
  }
};
