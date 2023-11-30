import BackendService from "services/backendService";
import { INGESTION } from "../../constants/restUri";
import { INCLUDE_RESULT_FIELD_KEY } from "configurations/containers/integration-steps/ingestion-monitoring/constants";

export class IngestionIntegrationStatusService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(integrationId: string) {
    const url = `${INGESTION}/${integrationId}/status`;
    return this.restInstance.post(url, {}, this.options);
  }
}

export class IngestionIntegrationLogService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(data: { filter: { id: string; [key: string]: any }; [key: string]: any }) {
    const { id, ...filters } = data.filter;
    if (data.filter[INCLUDE_RESULT_FIELD_KEY]) {
      return this.restInstance.post(
        `${INGESTION}/${id}/logs?${[INCLUDE_RESULT_FIELD_KEY]}=${data.filter[INCLUDE_RESULT_FIELD_KEY]}`,
        { ...data, filter: filters },
        this.options
      );
    } else {
      return this.restInstance.post(`${INGESTION}/${id}/logs`, { ...data, filter: filters }, this.options);
    }
  }
}
