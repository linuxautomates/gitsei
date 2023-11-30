import { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { restApiSelectGenericList } from "reduxConfigs/actions/restapi";

const SUPPORTED_APPLICATIONS = ["jira", "azure_devops", "zendesk"];

// use for getting custom fields list for jira and azure
export function useFieldList(application: string, integrationIds: string[], forceFetch: number = 0) {
  const dispatch = useDispatch();

  const [list, setList] = useState<{ key: string; type: string; name: string }[]>([]);
  const [loading, setLoading] = useState(true);

  const uri = useMemo(() => {
    let _uri = "jira_fields";
    if (application === IntegrationTypes.AZURE) {
      _uri = "issue_management_workItem_Fields_list";
    }
    if (application === IntegrationTypes.ZENDESK) {
      _uri = "zendesk_fields";
    }
    return _uri;
  }, [application]);

  const uuid = `${application}_application_field_list`;

  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid
  });

  useEffect(() => {
    if (!SUPPORTED_APPLICATIONS.includes(application)) {
      setLoading(false);
      return;
    }

    const data = get(genericSelector, "data", undefined);
    if (!data) {
      if (integrationIds.length) {
        dispatch(
          restApiSelectGenericList(true, uri, "list", { filter: { integration_ids: integrationIds } }, null, uuid)
        );
        setLoading(true);
      } else {
        setList([]);
        setLoading(false);
      }
    } else {
      const _data = get(genericSelector, ["data", "records"], []);

      // zendesk_fields provide data in field_id and title
      const mappedData = _data.map(
        (item: { field_key: string; field_type: string; name: string; field_id: number | string; title: string }) => ({
          key: item.field_key || `customfield_${item?.field_id?.toString()}`,
          type: item.field_type,
          name: item.name || item.title
        })
      );
      setList(mappedData);
      setLoading(false);
    }
  }, [integrationIds, forceFetch]);

  useEffect(() => {
    const _loading = get(genericSelector, "loading", true);
    const _error = get(genericSelector, "error", true);

    if (!_loading && !_error) {
      const data = get(genericSelector, ["data", "records"], []);

      // zendesk_fields provide data in field_id and title
      const mappedData = data.map(
        (item: { field_key: string; field_type: string; name: string; field_id: number | string; title: string }) => ({
          key: item.field_key || `customfield_${item?.field_id?.toString()}`,
          type: item.field_type,
          name: item.name || item.title
        })
      );
      setList(mappedData);
      setLoading(false);
    }
  }, [genericSelector]);

  return { loading, list };
}
