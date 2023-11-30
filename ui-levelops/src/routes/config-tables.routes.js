import ConfigTableListPage from "../configuration-tables/config-table-list.page";
import ConfigurationTablesEditCreate from "../configuration-tables/ConfigurationTablesEditCreate";
import { USER_ADMIN_ROLES } from "./helper/constants";
import { getBaseUrl } from 'constants/routePaths'
import withHarnessPermission from "hoc/withHarnessPermission";

export const configTableRoutes = () => {
  return [
    {
      name: "Tables",
      path: "/tables",
      layout: getBaseUrl(),
      component: withHarnessPermission(ConfigTableListPage),
      //hide: options.reactAppDashboard,
      collapse: false,
      id: "table-configs",
      label: "Tables",
      fullPath: `${getBaseUrl()}/tables`,
      icon: "signatures",
      actions: {},
      rbac: USER_ADMIN_ROLES,
      settingsDescription: "Import data and create tables which can be used throughout SEI.",
      settingsGroupId: 4,
      items: [
        {
          id: "table-configs",
          label: "Tables",
          path: "",
          description: "Detailed Config Table",
          hasAction: true,
          actionId: "configTable-add",
          dynamicHeader: true
        },
        {
          // Table Edit
          path: "/edit",
          rbac: USER_ADMIN_ROLES,
          description: "Edit Config Table",
          label: "Edit",
          showLabel: true,
          hasAction: false,
          dynamicHeader: true
        },
        {
          // Table Create
          path: "/create",
          rbac: USER_ADMIN_ROLES,
          description: "Create Config Table",
          label: "New",
          showLabel: true,
          hasAction: false,
          dynamicHeader: true
        }
      ]
    },
    {
      name: "Create table",
      path: "/tables/create",
      layout: getBaseUrl(),
      component: withHarnessPermission(ConfigurationTablesEditCreate),
      rbac: USER_ADMIN_ROLES,
      collapse: false
    },
    {
      name: "Edit table",
      path: "/tables/edit",
      layout: getBaseUrl(),
      component: withHarnessPermission(ConfigurationTablesEditCreate),
      rbac: USER_ADMIN_ROLES,
      collapse: false
    }
  ];
};
