import * as actions from "../actionTypes";
import { JENKINS_INTEGRATIONS_URI } from "./uri.constants";

const uri = JENKINS_INTEGRATIONS_URI;
const PAGE_SIZE = 100;

const getJenkinsFilters = filters => {
  const filter = { ...(filters.filter || {}) };
  delete filters.filter;
  return {
    filter: {
      ...filter,
      types: ["jenkins"]
    },
    ...filters,
    page_size: PAGE_SIZE,
    page: 0
  };
};

export const jenkinsIntegrationsList = (isPolling = false, fetchNextPage = false, filters = {}, id = "0") => ({
  type: actions.JENKINS_INTEGRATION_LIST,
  data: getJenkinsFilters(filters),
  uri: uri,
  method: "list",
  id,
  isPolling,
  fetchNextPage
});

export const jenkinsIntegrationsAttach = (data, id) => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri: uri,
  method: "update",
  id: id
});
