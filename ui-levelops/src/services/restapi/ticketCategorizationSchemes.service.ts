import { TICKET_CATEGORIZATION_SCHEMES } from "constants/restUri";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import BackendService from "services/backendService";

export class TicketCategorizationSchemesService extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
  }

  create(scheme: basicMappingType<any>) {
    return this.restInstance.post(TICKET_CATEGORIZATION_SCHEMES, scheme, this.options);
  }

  update(id: string, scheme: basicMappingType<any>) {
    const url = `${TICKET_CATEGORIZATION_SCHEMES}/${id}`;
    return this.restInstance.put(url, scheme, this.options);
  }

  get(id: string): any {
    const url = `${TICKET_CATEGORIZATION_SCHEMES}/${id}`;
    return this.restInstance.get(url, this.options);
  }

  list(filter = {}): any {
    const url = `${TICKET_CATEGORIZATION_SCHEMES}/list`;
    return this.restInstance.post(url, filter, this.options);
  }

  delete(id: string): any {
    const url = `${TICKET_CATEGORIZATION_SCHEMES}/${id}`;
    return this.restInstance.delete(url, this.options);
  }
}
