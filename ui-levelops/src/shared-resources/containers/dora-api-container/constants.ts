export const LOADING_ERROR_MESSAGE = "Something went wrong. We were unable to fetch data for this widget.";

export const INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_BEFORE =
  "This Collection is not associated with any workflow profile.";
export const INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_AFTER = "to associate it with a workflow profile.";
export const INCOMPLETE_ASSOCIATE_PROFILE_REASONS = [
  "This is a newly created collection.",
  "The associated profile is deleted."
];

export const INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE = "Invalid configuration for Change Failure Rate";
export const INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_BEFORE =
  "The existing configuration is incorrect for the total deployment calculation. ";
export const INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_AFTER =
  " to configure them to see the data here.";
export const INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL =
  "INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL";

export const INVALID_INT_FILTER_MESSAGE = "The integration/filters added are not applicable.";
export const INVALID_INT_FILTER_REASONS = [
  "Integrations added at the collection level are conflicting with the ones in the workflow profile.",
  "Integrations added at the project level are conflicting with the ones in the workflow profile.",
  "Filters added at the collection level are conflicting with the ones in the workflow profile.",
  "Filters added at the widget level are conflicting with the ones in the workflow profile."
];
