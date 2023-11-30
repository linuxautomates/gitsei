import moment from "moment";

export const getInstanceStatus = (instance: any) => {
  if (!instance.last_hb_at) {
    return "down";
  }

  // taking default interval 30 mins
  const lastAlive = moment().subtract(30, "minutes").toDate().getTime();

  if (instance.last_hb_at > lastAlive) {
    return "operational";
  }

  return "down";
};
