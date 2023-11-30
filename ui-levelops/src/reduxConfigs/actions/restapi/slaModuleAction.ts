import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import * as actions from "../actionTypes";
import { SLA_FILTERS_DATA } from "../actionTypes";

const uri = "sla_module_data";
export const fetchSlaData = (id: string = "0", filters: any, complete = null) => ({
  type: SLA_FILTERS_DATA,
  complete,
  filters,
  id,
  uri,
  method: "list"
});
