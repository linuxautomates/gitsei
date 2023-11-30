import { getTrellisCentralPage } from "constants/routePaths";
import { WebRoutes } from "routes/WebRoutes";

export const getBreadcrumbsForTrellisPage = () => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Trellis Score Profiles",
      path: WebRoutes.trellis_profile.list()
    }
  ];
};

export const getBreadcrumbsForTrellisProfile = (screenType: string) => {
  return [
    ...getBreadcrumbsForTrellisPage(),
    {
      label: screenType
    }
  ];
};

export const getBreadcrumbsForTrellisPageNew = () => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Trellis Template",
      path: getTrellisCentralPage()
    }
  ];
};
