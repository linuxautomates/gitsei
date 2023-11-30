import { ALL_VELOCITY_PROFILE_REPORTS } from "dashboard/constants/applications/names";
import { get, isArray } from "lodash";
import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { VELOCITY_CONFIG_LIST_ID, VELOCITY_CONFIGS } from "reduxConfigs/selectors/velocityConfigs.selector";

const uri = VELOCITY_CONFIGS;
const method = "list";
const uuid = VELOCITY_CONFIG_LIST_ID;

const useVelocityConfigProfiles = (reportType: string) => {
  const dispatch = useDispatch();

  const velocityConfigProfileState = useParamSelector(getGenericRestAPISelector, {
    uri,
    method,
    uuid
  });

  const [apiData, setApiData] = useState<any>([]);
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const [defaultProfile, setDefaultProfile] = useState<any>(undefined);

  useEffect(() => {
    if (ALL_VELOCITY_PROFILE_REPORTS.includes(reportType as any)) {
      const data = get(velocityConfigProfileState, ["data"], {});
      if (isArray(data?.records) && data?.records?.length) {
        setApiData(data.records);
      } else {
        dispatch(restapiClear(uri, method, "-1"));
        dispatch(genericList(uri, method, { filter: {} }, null, uuid));
        setApiLoading(true);
      }
    } else {
      setApiData([]);
      setApiLoading(false);
    }
  }, [reportType]);

  useEffect(() => {
    if (apiLoading) {
      const { loading, error } = velocityConfigProfileState;
      if (!loading && !error && Object.keys(velocityConfigProfileState?.data || {}).length) {
        const records = get(velocityConfigProfileState, ["data", "records"], []);
        setApiData(records);
        setApiLoading(false);
      }
    }
  }, [velocityConfigProfileState]);

  useEffect(() => {
    if (apiData) {
      setDefaultProfile(apiData.find((item: any) => !!item.default_config));
    } else {
      setDefaultProfile(undefined);
    }
  }, [apiData]);

  return { apiLoading, apiData, defaultProfile };
};

export default useVelocityConfigProfiles;
