import { tableCell } from "utils/tableUtils";
import { timeBoundFilterKeys } from "../../graph-filters/components/DateOptionConstants";
import { get } from "lodash";
import { checkTimeSecondOrMiliSecond } from "../helper";
import { CUSTOM_CHECKBOX_FIELD_TYPE } from "dashboard/reports/testRails/tests-report/constants";

export const testrailsCsvDrilldownDataTransformer = (data: any) => {

    const { apiData, columns, jsxHeaders, filters } = data;
    let headers = jsxHeaders ?? columns;
    return (apiData || []).map((record: any) => {
        return [...(headers || [])]
            .map((col: any) => {
                let result = record[col.key];

                if (col.key?.includes("custom_")) {
                    const customField: any = get(record, ["custom_case_fields", col.key], undefined);
                    const customFieldType = columns?.find((customKey: { key: string; })=> customKey.key === col.key);

                    if(customFieldType?.filedType === CUSTOM_CHECKBOX_FIELD_TYPE){
                        return customField ? "True" : 'False';
                    }

                    if (Array.isArray(customField)) {
                        if (!customField.length) return "";
                        return `"${customField.join(",")}"`;
                    }

                    if (typeof customField === "string") {
                        if (customField.includes(",")) {
                            return `"${customField}"`;
                        }
                        if (customField.includes("\n")) {
                            return `"${customField.replace(/\n/g, ' ')}"`;
                        }
                    }
                    return customField;
                }

                if (result === undefined) {
                    return result;
                }
                if (Array.isArray(result)) {
                    if (!result.length) return "";
                    return `"${result.join(",")}"`;
                }
                if (typeof result === "string") {
                    if (result.includes(",")) {
                        return `"${result}"`;
                    }
                    return result;
                }

                if (
                    timeBoundFilterKeys.includes(col.key) ||
                    col.key?.includes("created") ||
                    col.key?.includes("updated") ||
                    col.key?.includes("modify")
                ) {
                    if (col.key?.includes("workitem_")) {
                        result = checkTimeSecondOrMiliSecond(result);
                    }

                    if (col.title.includes("UTC")) {
                        return new Date(result).toISOString().replace(/T|Z/g, " ");
                    }
                    return tableCell("created_at", result);
                }

                return result;
            })
            .join(",");
    });
};
