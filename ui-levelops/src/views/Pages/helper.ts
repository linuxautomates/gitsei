import queryString from "query-string";

export const getLocationObject = (subsequent_url: string) => {
  const pathname = queryString.parseUrl(subsequent_url)?.url || "";
  const newSearch = subsequent_url?.split("?")?.[1];
  const locationObject = { pathname: pathname, search: newSearch ? `?${newSearch}` : "" };
  return locationObject;
};
