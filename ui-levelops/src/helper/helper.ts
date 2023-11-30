import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const hasAccessFromHarness = () => {
  // We'll just return this for now
  return !window.isStandaloneApp;
};

export const getIsStandaloneApp = (): boolean => {
  return Boolean(window.isStandaloneApp);
};

export const getBaseStaticUrl = (): string => (getIsStandaloneApp() ? "/" : "/sei/");

export const withBaseStaticUrl = (url: string): string => getBaseStaticUrl().concat(url);
