import { get } from "lodash";
import { AdvancedSettingButton, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { FC } from "react";
import { AntButton } from "shared-resources/components";

type AdvanceSettingsButton = {
  filterProps: LevelOpsFilter;
  advancedTabState: {
    value: boolean;
    callback: (x: boolean) => void;
  };
};

const DoraAdvanceSettingsButton: FC<AdvanceSettingsButton> = props => {
  const { advancedTabState, filterProps } = props;
  const { filterMetaData } = filterProps;
  const { getLabel, onClick } = filterMetaData as AdvancedSettingButton;
  const value = get(advancedTabState, "value", true);
  const callback = get(advancedTabState, "callback", undefined);
  const label = getLabel?.({ value, callback });

  return <AntButton onClick={() => onClick?.({ value, callback })}>{label}</AntButton>;
};

export default DoraAdvanceSettingsButton;
