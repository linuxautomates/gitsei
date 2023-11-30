import { createLocaleStrings } from "@harness/uicore";
import type { StringsMap } from "./types";

const { useLocaleStrings, LocaleString, StringsContextProvider } = createLocaleStrings<StringsMap>();

export { useLocaleStrings, useLocaleStrings as useStrings, LocaleString, StringsContextProvider };

export type StringKeys = keyof StringsMap;

export type GetStringFn = ReturnType<typeof useLocaleStrings>["getString"];
