import moment from "moment";
import { get, uniqBy, unset } from "lodash";
import {
  CACHED_GENERIC_REST_API_APPEND,
  CACHED_GENERIC_REST_API_CLEAR,
  CACHED_GENERIC_REST_API_ERROR,
  CACHED_GENERIC_REST_API_INVALIDATE,
  CACHED_GENERIC_REST_API_LOADING,
  CACHED_GENERIC_REST_API_SET
} from "reduxConfigs/actions/actionTypes";
import { CachedStateActionType } from "reduxConfigs/actions/cachedState.action";
import { isSanitizedValue } from "utils/commonUtils";

const INITIAL_STATE = {};

export default function cachedStateReducer(
  state = INITIAL_STATE,
  action: CachedStateActionType = {} as CachedStateActionType
) {
  const { uri, method, id, data, loading, error, expires_in, uniqueByKey } = action.payload ?? {};
  if (!isSanitizedValue(action.payload)) {
    return state;
  }
  switch (action.type) {
    case CACHED_GENERIC_REST_API_ERROR:
      return {
        ...state,
        [uri]: {
          ...get(state, uri, {}),
          [method]: {
            ...get(state, [uri, method], {}),
            [id]: {
              ...get(state, [uri, method, id], {}),
              loading: !isSanitizedValue(error),
              error
            }
          }
        }
      };
    case CACHED_GENERIC_REST_API_LOADING:
      return {
        ...state,
        [uri]: {
          ...get(state, uri, {}),
          [method]: {
            ...get(state, [uri, method], {}),
            [id]: {
              ...get(state, [uri, method, id], {}),
              loading,
              error: undefined
            }
          }
        }
      };
    case CACHED_GENERIC_REST_API_SET:
      return {
        ...state,
        [uri]: {
          ...get(state, uri, {}),
          [method]: {
            ...get(state, [uri, method], {}),
            [id]: {
              ...get(state, [uri, method, id], {}),
              data,
              loading: undefined,
              error: undefined,
              expires_in: expires_in ?? 3600, // seconds
              cached_at: moment().unix()
            }
          }
        }
      };
    case CACHED_GENERIC_REST_API_APPEND:
      const newRecords = [...get(state, [uri, method, id, "data", "records"], []), ...get(data, ["records"], [])];
      return {
        ...state,
        [uri]: {
          ...get(state, uri, {}),
          [method]: {
            ...get(state, [uri, method], {}),
            [id]: {
              ...get(state, [uri, method, id], {}),
              data: {
                ...get(state, [uri, method, id, "data"], {}),
                records: uniqueByKey ? uniqBy(newRecords, uniqueByKey) : newRecords
              },
              loading: undefined,
              error: undefined,
              expires_in: expires_in ?? 3600, // seconds
              cached_at: moment().unix()
            }
          }
        }
      };
    case CACHED_GENERIC_REST_API_CLEAR:
      unset(state, [uri, method, id]);
      return state;
    case CACHED_GENERIC_REST_API_INVALIDATE:
      const _data = get(state, [uri, method, id]);
      if (_data) {
        const expiresIn = _data.expires_in ?? 3600;
        const cachedAt = _data.cached_at;
        const now = moment().unix();
        const isDataExpired = expiresIn + cachedAt < now;
        if (isDataExpired) {
          unset(state, [uri, method, id]);
          return state;
        }
        return state;
      }
      return state;
    default:
      return state;
  }
}
