import BackendService from "./backendService";
import { ISSUE_MANAGEMENT_WORKITEM_FIELDS } from "../../constants/restUri";
import { get, unset } from "lodash";
import { getTransformedCustomFieldData } from "services/helper/effortInvestmentServiceHelper";

export class IssueManagementWorkItemFieldListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  async list(filter = {}) {
    const transformedData = get(filter, ["filter", "transformedCustomFieldData"], false);
    unset(filter, ["filter", "transformedCustomFieldData"]);
    const uri = `${ISSUE_MANAGEMENT_WORKITEM_FIELDS}/list`;
    const apiData = await this.restInstance.post(uri, filter, this.options);
    if (transformedData) {
      return getTransformedCustomFieldData(apiData);
    }
    return apiData;
  }
}
