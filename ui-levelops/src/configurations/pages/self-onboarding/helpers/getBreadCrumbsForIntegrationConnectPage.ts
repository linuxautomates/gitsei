import { WebRoutes } from "routes/WebRoutes";

export const getBreadcumsForConnectPage = (integration?: string, step?: number) => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Integrations",
      path: WebRoutes.settings.integrations()
    },
    {
      label: "Add Integrations",
      path: WebRoutes.self_onboarding.root(integration, step)
    }
  ];
};
