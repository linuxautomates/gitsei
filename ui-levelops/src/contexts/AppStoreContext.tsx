import { createContext, useContext } from "react";
import type { AppStoreContextProps, Scope } from "@harness/microfrontends";

export interface NewModuleAppStoreContextProps extends AppStoreContextProps {
  baseUrl: string;
  scope: Scope;
  isNav2Enabled: boolean
}

export const AppStoreContext = createContext<NewModuleAppStoreContextProps>({
  currentUserInfo: { uuid: "" },
  featureFlags: {},
  updateAppStore: () => void 0,
  baseUrl: "",
  scope: {},
  isNav2Enabled: false
});

export const useAppStore = () => useContext(AppStoreContext);
