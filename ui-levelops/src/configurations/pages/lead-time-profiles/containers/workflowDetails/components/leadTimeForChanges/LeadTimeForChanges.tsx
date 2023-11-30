import { RestDevelopmentStageConfig, RestStageConfig, VelocityConfigStage } from "classes/RestWorkflowProfile";
import React, { useEffect, useMemo, useState } from "react";
import { WORKFLOW_PROFILE_MENU } from "../constant";
import LeadTimeForChangesContent from "./leadTimeForChangesContent/LeadTimeForChangesContent";
import StageContent from "./stageContent/stageContent";

interface LeadTimeForChangesProps {
  leadTimeConfig: RestDevelopmentStageConfig;
  onChange: (newValue: any) => void;
  title: string;
  description: string;
  setExclamationFlag: (value: boolean) => void;
}

const LeadTimeForChanges: React.FC<LeadTimeForChangesProps> = props => {

  //COMMENTED CODE IS FOR NEW LEAD TIME & MTTR LIKE BEFORE WE MERGE WITH RETRO FIT PART , SO WHEN WE NEED THIS NEW JUST OPEN THE CODE
  // const [selectedStage, setSelectedStage] = useState<RestStageConfig>();
  // const [selectedStageType, setSelectedStageType] = useState<VelocityConfigStage>(VelocityConfigStage.FIXED_STAGE);

  const objectKey = useMemo(
    () =>
      props.title === WORKFLOW_PROFILE_MENU.LEAD_TIME_FOR_CHANGES ? "lead_time_for_changes" : "mean_time_to_restore",
    [props.title]
  );

  // useEffect(() => {
  //   setSelectedStage(undefined);
  // }, [props.title]);

  // useEffect(() => {
  //   if (selectedStage) {
  //     props.onChange({ hide_cancel_save_button: true });
  //   } else {
  //     props.onChange({ hide_cancel_save_button: false });
  //   }
  // }, [selectedStage, props.leadTimeConfig.fixed_stages_enabled]);

  // const onStageChange = (stageDetails: any) => {
  //   props.onChange({
  //     [objectKey]: {
  //       ...props.leadTimeConfig.json,
  //       ...stageDetails
  //     },
  //     hide_cancel_save_button: true
  //   });
  //   setSelectedStage(undefined);
  // };

  // const onCancel = () => setSelectedStage(undefined);

//   return selectedStage ? (
//     <StageContent
//       selectedStage={selectedStage}
//       onCancel={onCancel}
//       leadTimeConfig={props.leadTimeConfig}
//       onChange={onStageChange}
//       type={selectedStageType}
//       title={props.title}
//     />
//   ) : (
//     <LeadTimeForChangesContent
//       {...props}
//       objectKey={objectKey}
//       setSelectedStage={setSelectedStage}
//       setSelectedStageType={setSelectedStageType}
//     />
//   );
// };

  return (
    <LeadTimeForChangesContent
      {...props}
      objectKey={objectKey}
    />
  );
};

export default LeadTimeForChanges;
