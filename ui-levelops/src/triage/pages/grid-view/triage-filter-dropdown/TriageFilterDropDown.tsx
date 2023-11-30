import * as React from "react";
import { debounce, get } from "lodash";
import { notification } from "antd";
import { EditCloneModal, EditCreateCompactModal, InfoDropDown } from "../../../../shared-resources/components";
import { useState, useEffect } from "react";
import { useCallback } from "react";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useDispatch } from "react-redux";
import {
  listTriageFilter,
  deleteTriageFilter,
  createTriageFilter,
  updateTriageFilter
} from "reduxConfigs/actions/restapi/triageFilter.actions";
import { DropDownListItem } from "../../../../model/dropdown/HeaderDropDownItem";
import { TRIAGE_UUID } from "../grid-view-constant";
import moment from "moment";
import { EditCreateModalFormInfo, EditCreateModalPayload } from "../../../../model/modal/EditCreateModal";
import { TriageFilterDropDownList } from "model/triage/triage";
import { getTriageFilterNameFromId, sanitizeTimeFilter } from "./helper";
import { DATE_RANGE_FILTER_FORMAT } from "shared-resources/containers/server-paginated-table/containers/filters/constants";
import {
  TriageFilters,
  TriageFilterResponse,
  TriageFilterPayload
} from "reduxConfigs/actions/restapi/response-types/triageResponseTypes";

interface TriageFilterDropDownProps {
  isDefault?: boolean;
  filterName: string;
  handleEditItem?: (data: any) => void;
  handleItemClick: (value: string, item: DropDownListItem) => void;
  handleCloneItem?: (data: any) => void;
  handleCreateItem?: (id: string, item: any, action: string) => void;
  filterDescription: string;
  handleDefaultChange?: (name: string) => void;
  handleItemDelete?: (name: string, list: TriageFilterDropDownList[]) => void;
  createFilterWithData?: TriageFilters;
  doCreateAction?: () => void;
}

export const TriageFilterDropDown: React.FC<TriageFilterDropDownProps> = ({
  isDefault,
  filterName,
  handleEditItem,
  handleItemClick,
  handleCloneItem,
  handleCreateItem,
  filterDescription,
  handleDefaultChange,
  handleItemDelete,
  createFilterWithData,
  doCreateAction
}) => {
  const dispatch = useDispatch();
  const NAME_SEARCH_ID = "name_search";

  const [loadingFilterList, setLoadingFilterList] = useState<boolean>(true);
  const [searching, setSearching] = useState<boolean>(false);
  const [cloning, setCloning] = useState<boolean>(false);
  const [creating, setCreating] = useState<boolean>(false);
  const [updating, setUpdating] = useState<boolean>(false);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [nameSearching, setNameSearching] = useState(false);
  const [deleting, setDeleting] = useState<boolean>(false);

  const [filtersList, setFiltersList] = useState<any[]>([]);
  const [searchValue, setSearchValue] = useState<string>("");
  const [actionId, setActionId] = useState<string>("");
  const [recordCount, setRecordCount] = useState<number>(0);
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [searchName, setSearchName] = useState<undefined | string>(undefined);
  const [actionData, setActionData] = useState<{ [key: string]: any }>({});
  const [editData, setEditData] = useState<EditCreateModalPayload | undefined>(undefined);
  const [cloneId, setCloneId] = useState<string>("");
  const [updatingDefault, setUpdatingDefault] = useState<boolean>(false);

  const [refresh, setRefresh] = useState<number>(0);

  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [showCloneModal, setShowCloneModal] = useState<boolean>(false);

  const triageFilterListState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "list",
    uuid: "0"
  });

  const triageGridViewList = useParamSelector(getGenericRestAPISelector, {
    uri: "cicd_job_aggs",
    method: "list",
    uuid: TRIAGE_UUID
  });

  const triageFilterDeleteState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "delete",
    uuid: actionId
  });

  const triageFiltersSearchListState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "list",
    uuid: NAME_SEARCH_ID
  });

  const triageFilterCreateCloneState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "create",
    uuid: "0"
  });

  const triageFiltersUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "update",
    uuid: actionId
  });

  const title = getTriageFilterNameFromId(filterName) || "Views";

  const debouncedSearch = useCallback(
    debounce((v: string) => {
      fetchTriageList(v);
      setSearchValue(v);
    }, 250),
    []
  );

  const handleSearchChange = useCallback((v: string) => {
    debouncedSearch(v);
  }, []);

  const fetchTriageList = useCallback((search: string) => {
    dispatch(listTriageFilter({ filter: { partial_match: { name: { $contains: search } } } }));
  }, []);

  useEffect(() => {
    // preventing api call if data is already loaded
    const data = get(triageFilterListState, "data", {});
    if (Object.keys(data).length === 0) {
      fetchTriageList("");
    }
  }, []);

  useEffect(() => {
    // skipping the first render
    if (!searching && refresh !== 0) {
      fetchTriageList(searchValue);
      setRefreshing(true);
    }
  }, [refresh]);

  useEffect(() => {
    const count = get(triageGridViewList, ["data", "_metadata", "total_count"], 0);
    if (recordCount !== count) {
      setRecordCount(count);
    }
  }, [triageGridViewList]);

  useEffect(() => {
    if (
      triageFilterListState &&
      (loadingFilterList ||
        searching ||
        refreshing ||
        get(triageFilterListState, ["data", "records"], []).length !== filtersList.length)
    ) {
      const loading = get(triageFilterListState, "loading", true);
      const error = get(triageFilterListState, "error", true);
      if (!loading && !error) {
        let data = get(triageFilterListState, ["data", "records"], []);
        data = data.map((item: TriageFilterResponse) => ({
          id: item.name,
          name: getTriageFilterNameFromId(item.name),
          public: !!item.public,
          default: item.is_default,
          description: item.description,
          data: sanitizeTimeFilter(item)
        }));
        setFiltersList([...data]);
        loadingFilterList && setLoadingFilterList(false);
        searching && setSearching(false);
        refreshing && setRefreshing(false);
      }
    }
  }, [triageFilterListState]);

  const handleDelete = useCallback((name: string) => {
    setActionId(name);
    setDeleting(true);
    dispatch(deleteTriageFilter(name));
    setRefresh(refresh => refresh + 1);
  }, []);

  useEffect(() => {
    if (deleting) {
      const loading = get(triageFilterDeleteState, "loading", true);
      const error = get(triageFilterDeleteState, "error", true);
      if (!loading && !error) {
        handleItemDelete && handleItemDelete(actionId, filtersList);
        setDeleting(false);
        notification.success({ message: `${getTriageFilterNameFromId(actionId)} deleted` });
        setRefresh((value: number) => value + 1);
        setActionId("");
      }
    }
  }, [triageFilterDeleteState, filtersList]);

  const initialFormValue: EditCreateModalPayload = {
    name: "",
    description: "",
    default: Object.keys(createFilterWithData || {}).length > 0 || (filtersList.length === 0 && !searchValue),
    data: {
      filter: {
        end_time: [moment().format(DATE_RANGE_FILTER_FORMAT)],
        start_time: [moment().subtract(7, "d").format(DATE_RANGE_FILTER_FORMAT)]
      }
    }
  };

  const handleAddButton = useCallback(() => {
    setShowCreateModal(true);
    setEditData(initialFormValue);
  }, [createFilterWithData, filtersList, searchValue]);

  const handleClone = useCallback((name: string, item: DropDownListItem) => {
    setActionId(name);
    setActionData({ ...item, is_default: false });
    setShowCloneModal(true);
  }, []);

  const handleCreateModalCancel = useCallback(() => {
    setShowCreateModal(false);
    setActionId("");
    setEditData(undefined);
  }, []);

  useEffect(() => {
    if (updating) {
      const loading = get(triageFiltersUpdateState, "loading", true);
      const error = get(triageFiltersUpdateState, "error", true);
      if (!loading && !error) {
        let message = `${getTriageFilterNameFromId(actionId)} saved`;
        if (updatingDefault) {
          message = `${getTriageFilterNameFromId(actionId)} set as default`;
          setUpdatingDefault(false);
          handleDefaultChange && handleDefaultChange(actionId);
        } else {
          const data = { ...actionData };
          handleEditItem && handleEditItem(data);
        }
        notification.success({ message });
        setShowCreateModal(false);
        setUpdating(false);
        setActionId("");
        setRefresh(refresh => refresh + 1);
        setEditData(undefined);
        setActionData({});
      }
    }
  }, [triageFiltersUpdateState]);

  const handleCreateModalOk = useCallback(
    (item: EditCreateModalPayload | undefined) => {
      let _item: TriageFilterPayload = {
        is_default: !!item?.default,
        name: item?.name.toLowerCase().replace(/ /g, "-") || "",
        filter: { ...(item?.data?.filter || {}) },
        description: item?.description || ""
      };

      if (actionId) {
        dispatch(updateTriageFilter(actionId, { ..._item }));
        setUpdating(true);
      } else {
        if (createFilterWithData && Object.keys(createFilterWithData || {}).length > 0) {
          _item = { ..._item, filter: createFilterWithData };
          doCreateAction && doCreateAction();
        }
        setActionId(_item.name);
        dispatch(createTriageFilter(_item));
        setCreating(true);
      }
      setActionData(_item);
    },
    [actionId, createFilterWithData]
  );

  const handleEditClick = useCallback((name: string, item: DropDownListItem) => {
    const data: EditCreateModalPayload = {
      name: getTriageFilterNameFromId(item.name),
      description: item?.data?.description,
      default: item.default,
      data: {
        filter: item?.data?.filter
      }
    };
    setActionId(name);
    setEditData({ ...data });
    setShowCreateModal(true);
  }, []);

  const debouncedCloneNameSearch = useCallback(
    debounce((value: string) => {
      dispatch(
        listTriageFilter(
          { filter: { partial_match: { name: { $contains: value?.toLowerCase()?.replace(/ /g, "-") } } } },
          NAME_SEARCH_ID
        )
      );
      setNameSearching(true);
    }, 250),
    []
  );

  const handleCloneNameSearch = useCallback((value: string) => {
    setSearchName(value);
    debouncedCloneNameSearch(value);
  }, []);

  useEffect(() => {
    if (nameSearching) {
      const loading = get(triageFiltersSearchListState, "loading", true);
      const error = get(triageFiltersSearchListState, "error", true);
      if (!loading && !error) {
        setNameSearching(false);
        const data = get(triageFiltersSearchListState, ["data", "records"], []);
        setNameExist(
          !!data?.filter((item: any) => {
            const _searchName = searchName?.toLowerCase().replace(/ /g, "-");
            return item?.name?.toLowerCase() === _searchName;
          })?.length || false
        );
      }
    }
  }, [triageFiltersSearchListState]);

  useEffect(() => {
    if (cloning || creating) {
      const loading = get(triageFilterCreateCloneState, "loading", true);
      const error = get(triageFilterCreateCloneState, "error", true);
      if (!loading && !error) {
        const data = { ...actionData, name: actionId };
        if (cloning) {
          setCloning(false);
          handleCloneItem && handleCloneItem({ ...data, name: cloneId });
          notification.success({
            message: `${getTriageFilterNameFromId(actionId)} cloned as ${getTriageFilterNameFromId(cloneId)}`
          });
          setCloneId("");
        } else {
          handleCreateItem && handleCreateItem(actionId, data, "create");
          setShowCreateModal(false);
          setCreating(false);
          notification.success({ message: `${getTriageFilterNameFromId(actionId)} created` });
          setRefresh(refresh => refresh + 1);
        }
        setActionData({});
        setActionId("");
      }
    }
  }, [triageFilterCreateCloneState]);

  useEffect(() => {
    if (Object.keys(createFilterWithData || {}).length > 0 && filtersList.length === 0) {
      setShowCreateModal(true);
      setEditData(initialFormValue);
    }
  }, [createFilterWithData, filtersList, searchValue]);

  const startFilterClone = (name: string) => {
    const id = name.toLowerCase().replace(/ /g, "-");
    const payload = {
      name: id,
      description: actionData.data?.description,
      is_default: actionData.is_default,
      filter: actionData.data?.filter
    };
    setCloneId(id);
    dispatch(createTriageFilter(payload));
    setCloning(true);
  };

  const handleDefaultClick = useCallback(
    (name: string, item: DropDownListItem) => {
      const payload = {
        description: item?.data.description,
        filter: item?.data.filter,
        is_default: true
      };
      dispatch(updateTriageFilter(name, payload));
      setActionId(name);
      setUpdating(true);
      setUpdatingDefault(true);
    },
    [handleDefaultChange, filterName]
  );

  const formInfo: EditCreateModalFormInfo = {
    namePlaceholder: "Triage Grid View Name",
    descriptionPlaceholder: "Triage Grid View Description",
    disableDefault: Object.keys(createFilterWithData || {}).length > 0 || (filtersList.length === 0 && !searchValue)
  };

  return (
    <>
      <InfoDropDown
        title={title}
        onItemClick={handleItemClick}
        onEditClick={handleEditClick}
        onDeleteClick={handleDelete}
        onDefaultClick={handleDefaultClick}
        onCloneClick={handleClone}
        actionId={actionId}
        isDefault={!!isDefault}
        list={filtersList}
        handleSearchChange={handleSearchChange}
        itemCount={recordCount}
        loadingList={loadingFilterList}
        addButtonText={"Create Triage Grid View"}
        onAddButtonClick={handleAddButton}
        searchingList={searching}
        description={filterDescription}
        listHeight={400}
      />
      <EditCreateCompactModal
        onCancel={handleCreateModalCancel}
        onOk={handleCreateModalOk}
        visible={showCreateModal}
        searchEvent={handleCloneNameSearch}
        nameExists={!!nameExist}
        data={editData as any}
        loading={creating || updating}
        formInfo={formInfo}
        title={actionId ? "Edit Triage Grid View" : "Create Triage Grid View"}
        disableNameEdit={!!actionId}
      />
      <EditCloneModal
        visible={showCloneModal}
        title={"Clone Triage Grid View"}
        onOk={(name: string) => {
          setShowCloneModal(false);
          startFilterClone(name);
        }}
        onCancel={() => {
          setShowCloneModal(false);
          setActionId("");
        }}
        searchEvent={handleCloneNameSearch}
        nameExists={nameExist}
      />
    </>
  );
};
