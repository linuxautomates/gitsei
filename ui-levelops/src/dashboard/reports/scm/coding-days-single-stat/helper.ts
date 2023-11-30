import { get, set } from "lodash";
import moment from "moment";

export const widgetValidationFunction = (payload: any) => {
  const { query } = payload;
  const committed_at = get(query, ["committed_at"], undefined);
  return committed_at ? true : false;
};

export const prevQueryTransformer = (widget: any) => {
  const { query } = widget;
  if (!query?.committed_at) {
    query.committed_at = {
      $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
      $lt: moment.utc().unix().toString()
    };
  }
  set(widget, ["query", "agg_type"], "average");
};
