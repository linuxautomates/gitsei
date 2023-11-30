import { ProjectPathProps } from "classes/routeInterface";
import {
  DASHBOARD_ROUTES,
  getDashboardsPage,
  getIntegrationPage,
  getBaseUrl,
  Organization_Routes,
  ORGANIZATION_USERS_ROUTES,
  SELF_ONBOARDING_ROUTES,
  getSettingsPage,
  TICKET_CATEGORIZATION_SCHEMES_ROUTES,
  TRELLIS_SCORE_PROFILE_ROUTES,
  TRIAGE_ROUTES,
  VELOCITY_CONFIGS_ROUTES,
  WORKSPACE_PATH
} from "constants/routePaths";
import { projectPathPropsDef } from "utils/routeUtils";

export const WebRoutes = {
  dashboard: {
    demo_root: () => `${getBaseUrl()}/demo-dashboard`,
    drilldown: (params: ProjectPathProps) => `${getBaseUrl(params)}${DASHBOARD_ROUTES.DRILL_DOWN}`,
    create: (params: ProjectPathProps, ou_category_id?: string) =>
      ou_category_id
        ? `${getBaseUrl(params)}${DASHBOARD_ROUTES.CREATE}?ou_category_id=${ou_category_id}`
        : `${getBaseUrl(params)}${DASHBOARD_ROUTES.CREATE}`,
    banner: () => `${getIntegrationPage()}?tab=available_integrations`,
    list: (params: ProjectPathProps) => `${getBaseUrl(params)}${DASHBOARD_ROUTES.LIST}`,
    details: (params: ProjectPathProps, dashboardId = ":dashboardId", search?: string) =>
      `${getDashboardsPage(params)}/${dashboardId}${search ? search : ""}`,
    scorecard: (
      params: ProjectPathProps,
      user_id: string,
      dateInterval: string | null = "",
      user_id_type: "ou_user_ids" | "integration_user_ids" = "ou_user_ids",
      OU?: string | undefined
    ) =>
      `${getDashboardsPage(params)}/scorecard?user_id=${user_id}&user_id_type=${user_id_type}${
        dateInterval ? `&interval=${dateInterval}` : ""
      }${OU ? `&OU=${OU}` : ""}`,
    demoScoreCard: (
      user_id: string,
      id: any,
      dashboardId: string,
      index: Number,
      ou_uuid: Array<string>,
      ou_id: Array<string>,
      dateInterval: string | null = ""
    ) =>
      `${getDashboardsPage(
        projectPathPropsDef
      )}/demo_scorecard?ou_id=${ou_id}&user_id=${user_id}&dashboardId=${dashboardId}&index=${index}&ou_uuid=${ou_uuid}${
        dateInterval ? `&interval=${dateInterval}` : ""
      }&id=${id}`,
    scorecardDrillDown: (
      params: ProjectPathProps,
      user_id: string,
      user_id_type: string,
      selected_feature: string,
      dashboard_time_gt_value: string,
      dashboard_time_lt_value: string,
      trellis_profile_id?: string,
      interval?: string,
      OU?: string
    ) =>
      `${getDashboardsPage(
        params
      )}/scorecard/drilldown?user_id=${user_id}&user_id_type=${user_id_type}&selected_feature=${selected_feature}&dashboard_time_gt_value=${dashboard_time_gt_value}&dashboard_time_lt_value=${dashboard_time_lt_value}&interval=${interval}${
        OU ? `&OU=${OU}` : ""
      }`,
    devProductivityDashboard: (
      params: ProjectPathProps,
      ou_id: string,
      ou_uuid: string,
      dateInterval: string | null = ""
    ) =>
      `${getDashboardsPage(params)}/dev_productivity?ou_id=${ou_id}&ou_uuid=${ou_uuid}${
        dateInterval ? `&interval=${dateInterval}` : ""
      }`,
    demoDevProductivityDashboard: (
      id: any,
      ou_id: string,
      dashboardId: string,
      index: Number,
      ou_uuid: Array<string>,
      dateInterval: string | null = ""
    ) =>
      `${getDashboardsPage(
        projectPathPropsDef
      )}/demo_dev_productivity?ou_id=${ou_id}&dashboardId=${dashboardId}&index=${index}${
        dateInterval ? `&interval=${dateInterval}` : ""
      }&id=${id}&ou_uuid=${[ou_uuid]}`,
    widgets: {
      details: (params: ProjectPathProps, dashboardId = ":dashboardId", widgetId = ":widgetId") =>
        `${getDashboardsPage(params)}/${dashboardId}/widgets/${widgetId}`,
      create: (params: ProjectPathProps, dashboardId = ":dashboardId", widgetId = ":widgetId") =>
        `${getDashboardsPage(params)}/${dashboardId}/widgets/${widgetId}/new`,
      widgetsExplorer: (params: ProjectPathProps, dashboardId = ":dashboardId", search?: string) =>
        `${getDashboardsPage(params)}/${dashboardId}/widgets/explorer${search ? search : ""}`,
      widgetsExploreByCategory: (
        params: ProjectPathProps,
        dashboardId = ":dashboardId",
        type = ":typeId",
        search?: string
      ) => `${getDashboardsPage(params)}/${dashboardId}/widgets/explorer/${type}${search ? search : ""}`,
      widgetsExploreByCustomCategory: (
        params: ProjectPathProps,
        dashboardId = ":dashboardId",
        type = ":typeId",
        search?: string
      ) => `${getDashboardsPage(params)}/${dashboardId}/widgets/explorer/custom/${type}${search ? search : ""}`,
      widgetsRearrange: (params: ProjectPathProps, dashboardId = ":dashboardId", search?: string) =>
        `${getDashboardsPage(params)}/${dashboardId}/widgets/modify-layout${search ? search : ""}`,
      widgetsRearrangeWithCopyInProgress: (
        params: ProjectPathProps,
        destinationDashboardId = ":dashboardId",
        copyParentDashboardId: string,
        search?: string
      ) =>
        `${getDashboardsPage(params)}/${destinationDashboardId}/widgets/modify-layout${
          search ? `${search}` : ""
        }&copy_from=${copyParentDashboardId}`
    },
    dashboard_ou: {
      root: (params: ProjectPathProps, dashboardId: string, ouId: string, rootOUId: string) =>
        `${getDashboardsPage(params)}/${dashboardId}?OU=${ouId}`
    }
  },
  settings: {
    root: getSettingsPage,
    integrations: () => `${getSettingsPage()}/integrations`,
    integration_edit: (id: string) => `${getSettingsPage()}/integrations/edit?id=${id}`,
    global_settings: (query?: string) =>
      query?.length ? `${getSettingsPage()}/global?${query}` : `${getSettingsPage()}/global`
  },
  self_onboarding: {
    root: (integration?: string, step?: number, concateSuffix = "") => {
      const staticURL = !!concateSuffix
        ? `${SELF_ONBOARDING_ROUTES._ROOT}/${concateSuffix}`
        : SELF_ONBOARDING_ROUTES._ROOT;
      let route = Number.isInteger(step) ? `${getBaseUrl()}${staticURL}/${step}` : `${getBaseUrl()}${staticURL}`;
      if (integration) {
        route = route.concat(`?integration=${integration}`);
      }
      return route;
    },
    edit: (integration_id: string, integration?: string, step = 1) => {
      return `${getBaseUrl()}${SELF_ONBOARDING_ROUTES._EDIT}/${step}?id=${integration_id}&integration=${integration}`;
    }
  },
  ticket_categorization: {
    list: () => `${getBaseUrl()}${TICKET_CATEGORIZATION_SCHEMES_ROUTES._ROOT}`,
    scheme: {
      details: (schemeId = ":id") => `${getBaseUrl()}${TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE}/${schemeId}`,
      edit: (schemeId = ":id", tabKey?: string) => {
        if (!tabKey) return `${getBaseUrl()}${TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE}/${schemeId}?edit=true`;
        return `${getBaseUrl()}${
          TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE
        }/${schemeId}?edit=true&tab_key=${tabKey}`;
      },
      category: {
        details: (schemeId = ":id", categoryId = ":categoryId") =>
          `${getBaseUrl()}${TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE}/${schemeId}/categories/${categoryId}`
      }
    }
  },
  velocity_profile: {
    list: () => `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`,
    scheme: {
      edit: (configId = ":id") => `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=${configId}`,
      editTab: (queryParam: string) => `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?${queryParam}`
    }
  },
  trellis_profile: {
    list: () => `${getBaseUrl()}${TRELLIS_SCORE_PROFILE_ROUTES._ROOT}`,
    scheme: {
      edit: (configId = ":id") => `${getBaseUrl()}${TRELLIS_SCORE_PROFILE_ROUTES.EDIT}/${configId}`
    }
  },
  integration: {
    list: () => getIntegrationPage(),
    types: () => `${getIntegrationPage()}/types`,
    newIntegration: () => `${getIntegrationPage()}/new-add-integration-page`
  },
  table: {
    create: `${getBaseUrl()}/tables/create`
  },
  triage: {
    root: () => `${getBaseUrl()}${TRIAGE_ROUTES._ROOT}?tab=triage_grid_view`,
    results: () => `${getBaseUrl()}${TRIAGE_ROUTES.RESULTS}`
  },
  organization_page: {
    root: (params: ProjectPathProps, workspaceId?: string, tab?: string) =>
      tab
        ? `${getBaseUrl(params)}${Organization_Routes._ROOT}?ou_workspace_id=${workspaceId}&ou_category_tab=${tab}`
        : `${getBaseUrl(params)}${Organization_Routes._ROOT}`,
    create_org_unit: (params: ProjectPathProps, workspaceId?: string, tab?: string) =>
      tab
        ? `${getBaseUrl(params)}${
            Organization_Routes.CREATE_ORG_UNIT
          }?ou_workspace_id=${workspaceId}&ou_category_tab=${tab}`
        : `${getBaseUrl(params)}${Organization_Routes.CREATE_ORG_UNIT}?`,
    edit: (
      params: ProjectPathProps,
      orgUnitId = ":id",
      workspaceId?: string,
      ou_category_tab?: string,
      tab?: string
    ) => {
      let url = `${getBaseUrl(params)}${Organization_Routes.CREATE_ORG_UNIT}/${orgUnitId}`;
      if (ou_category_tab) {
        url = url + `?ou_workspace_id=${workspaceId}&ou_category_tab=${ou_category_tab}`;
      }
      if (tab) {
        url = url + `&tab=${tab}`;
      }
      return url;
    }
  },
  organization_users_page: {
    root: (version?: string) => {
      let route = `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`;
      if (version) {
        route = `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}?version=${version}`;
      }
      return route;
    }
  },
  workspace: {
    root: () => `${getBaseUrl()}${WORKSPACE_PATH}`,
    edit: (workspaceId?: String) => {
      let baseURL = `${getBaseUrl()}${WORKSPACE_PATH}`;
      if (workspaceId) {
        return `${baseURL}?workspace_id=${workspaceId}`;
      }
      return baseURL;
    }
  },
  no_dashboard: {
    details: (search: any) => `${getBaseUrl()}/no-ou-dash${search ? search : ""}`
  },
  home: {
    home_page: getBaseUrl
  }
};
