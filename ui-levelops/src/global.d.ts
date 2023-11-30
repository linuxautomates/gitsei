declare module "strings/strings.en.yaml" {
  import { StringsMap } from "strings/types";
  export default StringsMap;
}

declare interface Window {
  apiUrl: string;
  getApiBaseUrl: (str: string) => string;
  baseUrl?: string;
  isStandaloneApp: boolean;
  noAuthHeader: boolean;
}
