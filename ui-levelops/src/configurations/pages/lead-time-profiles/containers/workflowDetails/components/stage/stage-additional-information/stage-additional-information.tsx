import React, { useEffect } from "react";
import { RestStageConfig } from "classes/RestWorkflowProfile";
import SCMConfigurations from "../../scmDefinitions/SCMConfigurations";
import { DORAConfigDefinition } from "classes/DORAConfigDefinition";
import { StageEndOptions } from "classes/StageEndOptions";

interface StageDescriptionProps {
  stage: RestStageConfig;
  onChange: (stage: any) => void;
}
const StageAdditionalInformation: React.FC<StageDescriptionProps> = props => {
  const { stage, onChange } = props;

  useEffect(() => {
    if (stage?.event?.type === "SCM_PR_SOURCE_BRANCH" && stage.event?.scm_filters?.source_branch?.checked) {
      stage.event.scm_filters.source_branch.checked = false;
      onChange(stage.json);
    }
  }, []);

  const onScmChange = (updatedConfig: DORAConfigDefinition) => {
    if (stage.event) {
      stage.event = {
        ...stage.event,
        scm_filters: updatedConfig
      };
    } else {
      stage.event = {
        type: StageEndOptions.SCM_PR_MERGED,
        params: undefined,
        values: [],
        scm_filters: updatedConfig
      };
    }
    onChange(stage.json);
  };

  return null; 
  // (
  //   <SCMConfigurations
  //     // config={stage.event?.scm_filters || new DORAConfigDefinition(null, "release", ["source_branch"])}
  //     calculationType={"Stage"}
  //     // onChange={onScmChange}
  //   />
  // );
};

export default StageAdditionalInformation;
