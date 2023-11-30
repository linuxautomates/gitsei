import moment from "moment";

export const jiraSingleStatDefaultCreatedAt = {
  $lt: moment.utc().unix().toString(),
  $gt: moment.utc().unix().toString()
};
