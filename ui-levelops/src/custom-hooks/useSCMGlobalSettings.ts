import { DEFAULT_SCM_SETTINGS_OPTIONS } from "dashboard/constants/defaultFilterOptions";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { useMemo } from "react";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";

export const useSCMGlobalSettings = () => {
  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  const scmGlobalSettings = useMemo(() => {
    const SCM_GLOBAL_SETTING = globalSettingsState?.data?.records.find(
      (item: any) => item.name === "SCM_GLOBAL_SETTINGS"
    );
    return SCM_GLOBAL_SETTING
      ? typeof SCM_GLOBAL_SETTING?.value === "string"
        ? JSON.parse(SCM_GLOBAL_SETTING?.value)
        : SCM_GLOBAL_SETTING?.value
      : DEFAULT_SCM_SETTINGS_OPTIONS;
  }, [globalSettingsState]);

  return { settings: scmGlobalSettings };
};
