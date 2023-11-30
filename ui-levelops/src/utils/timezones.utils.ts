import timezoneObj from "timezone/timezones";

export const timezones = timezoneObj.map(obj => {
  return { value: `GMT${obj.offset}`, label: `${obj.value} (${obj.abbr})` };
});
