import { get } from "lodash";

export function getErrorMessage(errorObj?: any): string | undefined {
  if (get(errorObj, "data")) {
    return (
      get(errorObj, "data.detailedMessage") ||
      get(errorObj, "data.message") ||
      JSON.stringify(get(errorObj, "data"), null, "\t")
    );
  }
  return get(errorObj, "message");
}
