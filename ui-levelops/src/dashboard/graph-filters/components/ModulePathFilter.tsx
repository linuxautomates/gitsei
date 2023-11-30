import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AutoComplete } from "antd";
import FormItem from "antd/lib/form/FormItem";
import { useDispatch } from "react-redux";
import { get } from "lodash";
import { v1 as uuid } from "uuid";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntInput, AntIcon } from "shared-resources/components";
import { isFile } from "shared-resources/charts/grid-view-chart/util";
import { FileReportRootURIs } from "../../constants/helper";
import { stringSortingComparator } from "./sort.helper";

const { Option } = AutoComplete;

interface ModulePathFilterProps {
  uri: string;
  value: any;
  onChange: (value: any) => void;
  method?: string;
  uuid?: string;
  integrationIds?: any;
}

const ModulePathFilter: React.FC<ModulePathFilterProps> = props => {
  const { uri, value, integrationIds, onChange } = props;

  const method = props.method || "list";
  const id = useMemo(() => props.uuid || uuid(), []);

  const extra = "Type / after folder name to traverse inside the selected directory";

  const [searchQuery, setSearchQuery] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState<any[]>([]);
  const [changeDirectory, setChangeDirectory] = useState(false);
  const [focus, setFocus] = useState(false);

  const filterKey = useMemo(() => (uri === FileReportRootURIs.SCM_JIRA_FILES_REPORT ? "scm_module" : "module"), [uri]);
  const defaultPath = useMemo(() => (value ? `${value}/` : ""), [value]);

  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri,
    method,
    uuid: id
  });

  const dispatch = useDispatch();

  const filters = useMemo(
    () => ({
      integration_ids: integrationIds,
      [filterKey]: value
    }),
    [integrationIds, value]
  );

  const filteredOptions = useMemo(() => {
    if (loading) {
      return [];
    }
    return options
      .filter(option => {
        const searchText = searchQuery.startsWith(defaultPath) ? searchQuery.slice(defaultPath.length) : searchQuery;
        return option?.key.includes(searchText);
      })
      .sort(stringSortingComparator("repoId"))
      .map(item => {
        const key = [FileReportRootURIs.SCM_FILES_REPORT, FileReportRootURIs.SCM_JIRA_FILES_REPORT].includes(uri as any)
          ? item.repoId
          : item.key;
        return (
          <Option key={key} value={value ? `${value}/${item.key}` : item.key} disabled={isFile(item.key)}>
            {`${item.repoId}/${defaultPath}${item.key}`}
          </Option>
        );
      });
  }, [uri, loading, options, searchQuery, value]);

  useEffect(() => {
    dispatch(restapiClear(uri, method, id));
    dispatch(genericList(uri, method, { filter: { ...(filters || {}) } }, null, id));
    if (changeDirectory) {
      const searchText = searchQuery?.split("/").pop();
      setSearchQuery(`${defaultPath}${searchText}`);
      setChangeDirectory(false);
    } else {
      setSearchQuery(defaultPath);
    }
    setLoading(true);
  }, [value]);

  useEffect(() => {
    if (loading) {
      const { loading, error } = genericSelector;
      if (!loading && !error && Object.keys(genericSelector?.data || {}).length) {
        const records = get(genericSelector, ["data", "records"], []);
        const options = records.map((item: any) => {
          return {
            key: item.key,
            repoId: item.repo_id
          };
        });
        setOptions(options);
        setLoading(false);
      }
    }
  }, [genericSelector]);

  const handleSelect = useCallback(
    (value, option) => {
      let data: any = {
        module: value
      };

      if ([FileReportRootURIs.SCM_FILES_REPORT, FileReportRootURIs.SCM_JIRA_FILES_REPORT].includes(uri as any)) {
        data = {
          ...data,
          repoId: option.key
        };
      }
      !isFile(value) && onChange(data);
    },
    [onChange]
  );

  const handleSearch = useCallback(
    search => {
      if (search.startsWith(defaultPath)) {
        const paths = search.split("/");
        const searchString = paths.pop();
        const currentPathWithRoot = paths.join("/");
        if (searchString) {
          setSearchQuery(search);
        }
        if (`${currentPathWithRoot}/` !== defaultPath) {
          setChangeDirectory(true);
          setSearchQuery(searchString);
          const currentDirPath = paths;
          onChange({
            module: currentDirPath.length ? currentDirPath.join("/") : "",
            repoId: ""
          });
        } else {
          setSearchQuery(search);
        }
      } else {
        setChangeDirectory(true);
        setSearchQuery(search);
        value &&
          onChange({
            module: "",
            repoId: ""
          });
      }
    },
    [value, searchQuery, onChange]
  );

  const handleBlur = useCallback(() => {
    setSearchQuery(defaultPath);
    setFocus(false);
  }, [value]);

  const handleFocus = useCallback(() => {
    setFocus(true);
  }, []);

  const inputSuffix = useMemo(() => {
    return <AntIcon className="search-icon" type={loading ? "loading" : "search"} />;
  }, [loading, focus]);

  const inputField = useMemo(() => {
    return <AntInput className="search-field" placeholder="Module Paths" suffix={inputSuffix} />;
  }, [loading, focus]);

  return (
    <FormItem key={"module-path-filter"} label={"MODULE PATHS"} extra={extra}>
      <div className="module-path-filter">
        <AutoComplete
          value={searchQuery}
          dropdownMatchSelectWidth
          notFoundContent={loading ? "Loading Data..." : "No Data"}
          dataSource={filteredOptions}
          onSelect={handleSelect}
          onSearch={handleSearch}
          onFocus={handleFocus}
          onBlur={handleBlur}>
          {inputField}
        </AutoComplete>
      </div>
    </FormItem>
  );
};

export default React.memo(ModulePathFilter);
