import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getId = createParameterSelector((params: any) => (params as any).id);

export const testrailsCustomFiledSelector = createSelector(restapiState, (data: any) => {
    return get(data, "testrails_fields", undefined);
});

export const testrailsCustomFiledData = createSelector(testrailsCustomFiledSelector, (data: any) => {
    return get(data, ["list", "testrails_application_field_list", "data", "records"], []);
});

export const testrailsCustomFiledDataArray = createSelector(testrailsCustomFiledData, (data: any) => {
    return data.map((data: {
        type: any; label: any; system_name: any;
    }) => {
        return { "name": data.label, "key": data.system_name, "type": data.type.toLowerCase() }
    })
});

const _testrailsFieldsListSelector = createSelector(restapiState, apis => {
    return get(apis, ["testrails_fields", "list"], {});
});

export const testrailsFieldsListSelector = createSelector(_testrailsFieldsListSelector, getId, (liststate: any, id: string) => {
    return get(liststate, [id, "data"], { loading: true, error: false });
});