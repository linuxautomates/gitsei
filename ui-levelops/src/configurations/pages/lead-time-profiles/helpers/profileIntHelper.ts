import { RestWorkflowProfile } from "classes/RestWorkflowProfile";

export const getSelectedIntegrationIds = (profile: RestWorkflowProfile, LTFCAndMTTRSupport: boolean) => {
  let selectedInt: number[] = [];
  if (profile) {
    if (LTFCAndMTTRSupport) {
      profile.lead_time_for_changes &&
        profile.lead_time_for_changes.integration_id &&
        selectedInt.push(+profile.lead_time_for_changes.integration_id);
      profile.mean_time_to_restore &&
        profile.mean_time_to_restore.integration_id &&
        selectedInt.push(+profile.mean_time_to_restore.integration_id);
    }
    if (profile.change_failure_rate &&
      profile.change_failure_rate.integration_ids)
      selectedInt = [...selectedInt, ...profile.change_failure_rate.integration_ids.map((intId: string) => +intId)];
    if (profile.deployment_frequency &&
      profile.deployment_frequency.integration_ids)
      selectedInt = [...selectedInt, ...profile.deployment_frequency.integration_ids.map((intId: string) => +intId)];
  }
  return selectedInt;
}