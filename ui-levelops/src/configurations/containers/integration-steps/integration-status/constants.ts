import { IngestionStatus } from "reduxConfigs/actions/restapi/response-types/ingestionResponseType";

export const STATUS_NOT_AVAILABLE = "Status not available";

const IntegrationStatusMessage = {
  [IngestionStatus.HEALTHY]: {
    "*": "There was no job failure in the past one week."
  },
  [IngestionStatus.WARNING]: {
    "*": "There was no job executed in the past one week."
  },
  [IngestionStatus.UNKNOWN]: {
    custom: STATUS_NOT_AVAILABLE,
    jenkins: STATUS_NOT_AVAILABLE,
    "*": "We are trying to establish a connection with this integration. This should be updated once we have completed at least one ingestion job."
  },
  [IngestionStatus.FAILED]: {
    "*": "At least one job failed in the past one week due to which your integration has failed."
  }
};

export default IntegrationStatusMessage;
