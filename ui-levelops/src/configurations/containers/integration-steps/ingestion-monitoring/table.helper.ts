import moment from "moment";

export const getScanTimeRange = (record: { to: number; from: number }) => {
  let to = record.to ? record.to : "-";
  let from = record.from ? record.from : "-";
  if (record.to) {
    to = moment.unix(record.to).format("YYYY/MM/DD HH:mm:ss");
  }
  if (record.from) {
    from = moment.unix(record.from).format("YYYY/MM/DD HH:mm:ss");
  }

  if (to === "-" && from === "-") {
    return null;
  }
  return `${from} to ${to}`;
};

export const getTimeToComplete = (value?: number) => {
  if (!value) {
    return "-";
  }
  if (value > 86400) {
    const days = parseInt((value / 86400).toString());
    const hrs = ((value % 86400) / 3600).toFixed(2);
    return `${days} days ${hrs} hrs`;
  } else if (value > 3600) {
    const hrs = parseInt((value / 3600).toString());
    const mins = ((value % 3600) / 60).toFixed(2);
    return `${hrs} hrs ${mins} mins`;
  } else if (value > 60) {
    const min = parseInt((value / 60).toString());
    const secs = value % 60;
    return `${min} mins ${secs} secs`;
  } else {
    return value + " secs";
  }
};
