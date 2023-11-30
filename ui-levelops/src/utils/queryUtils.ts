import queryString from "query-string";

export const buildQueryParamsFromObject = (obj: {
  [key: string]: undefined | string | string[] | number | number[];
}) => {
  const searchParams = {} as any;
  if (obj) {
    Object.keys(obj).map((key: string) => {
      const searchParam = obj[key];
      // 0 and false will be not included
      if (searchParam) {
        searchParams[key] = searchParam;
      }
    });
  }
  return new URLSearchParams(searchParams).toString();
};

export const parseQueryParamsIntoKeys = (search: any, keys: string[] | undefined) => {
  const parsedData = {} as any;
  if (!keys || keys.length === 0) {
    return parsedData;
  }
  let parsedQuery = queryString.parse(search);

  if (parsedQuery.filters) {
    const filters = JSON.parse(parsedQuery.filters as string);
    delete parsedQuery.filters;
    parsedQuery = { ...(parsedQuery || {}), ...filters };
  }

  keys.map((key: string) => {
    const value = parsedQuery[key];
    if (value) {
      const is_object = value?.constructor === Object;
      if (is_object) {
        parsedData[key] = value;
      } else {
        parsedData[key] = value.toString().split(",");
      }
    }
  });

  return parsedData;
};
