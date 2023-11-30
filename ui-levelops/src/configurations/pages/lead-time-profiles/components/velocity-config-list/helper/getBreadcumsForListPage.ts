import { WebRoutes } from "routes/WebRoutes";

export const getBreadcumsForListPage = () => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Workflow Profiles",
      path: WebRoutes.velocity_profile.list()
    }
  ];
};
