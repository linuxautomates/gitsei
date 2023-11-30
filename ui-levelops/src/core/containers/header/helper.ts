import { find, findIndex } from "lodash";
import StringsEn from "../../../locales/StringsEn";

// modify urls based on search queries if needed
export const getHeaderBreadcrumbTo = (
  id: string,
  pathroot: string,
  pathnames: string[],
  index: number,
  location: any
) => {
  let search = "";

  if (id === "propels" && location.search && location.search.includes("propels")) {
    search = "tab=propels";
  }

  if (search) {
    return `/${pathroot}/${pathnames.slice(0, index + 1).join("/")}?${search}`;
  }

  return `/${pathroot}/${pathnames.slice(0, index + 1).join("/")}`;
};

// modify the breadcrum label based on url if needed
export const getHeaderBreadcrumbLabel = (path: string, location: any, label: string) => {
  if (location.search && path === "propels" && location.search.includes("propels")) {
    return StringsEn.propelsMy;
  }

  return label;
};

export const setMenuHeaderActions = (menuItems: any[], key: string, clicked: boolean) => {
  let item = find(menuItems, menu => menu.id === key);
  if (item) {
    item = { ...item, hasClicked: clicked };
    const index = findIndex(menuItems, menu => menu.id === key);
    menuItems[index] = item;
  }
  return menuItems;
};
