import queryString from "query-string";
import { get } from "lodash";

// THIS WILL USE IN OPEN REPORT
// FOR GET OU ID FROM URL
export const getParamOuArray = (link: string) => {
    try {
        const queryOUFilters = get(queryString.parse(link), "OUFilter") ? JSON.parse(get(queryString.parse(link), "OUFilter", "") as string) : {};
        const queryParamOUArray = get(queryOUFilters, ["ou_ids"], undefined);

        return queryParamOUArray;
    } catch (err) { }
    return false;
};