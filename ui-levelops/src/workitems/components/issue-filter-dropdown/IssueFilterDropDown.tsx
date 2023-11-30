import * as React from "react";
import { debounce, get } from "lodash";
import { notification } from "antd";
import { useState, useEffect } from "react";
import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { getIssueFilterNameFromId, sanitizeTimeFilter } from "./helper";
import { DropDownListItem } from "model/dropdown/HeaderDropDownItem";
import { TriageFilterDropDownList } from "model/triage/triage";
import { EditCreateModalPayload, EditCreateModalFormInfo } from "model/modal/EditCreateModal";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  createIssueFilter,
  deleteIssueFilter,
  listIssueFilter,
  updateIssueFilter
} from "reduxConfigs/actions/restapi/issueFilter.actions";
import { InfoDropDown, EditCreateCompactModal, EditCloneModal } from "shared-resources/components";

interface TriageFilterDropDownProps {
  uuid: string;
  isDefault?: boolean;
  filterName: string;
  handleEditItem?: (data: any) => void;
  handleItemClick: (value: string, item: DropDownListItem) => void;
  handleCloneItem?: (data: any) => void;
  handleCreateItem?: (id: string, item: any, action: string) => void;
  filterDescription: string;
  handleDefaultChange?: (name: string) => void;
  handleItemDelete?: (name: string, list: TriageFilterDropDownList[]) => void;
  createFilterWithData?: any;
  doCreateAction?: () => void;
}

export const IssueFilterDropDown: React.FC<TriageFilterDropDownProps> = ({
  uuid,
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

  const issueFilterListState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "list",
    uuid: "0"
  });

  const issuesList = useParamSelector(getGenericRestAPISelector, {
    uri: "workitem",
    method: "list",
    uuid: uuid
  });

  const issueFilterDeleteState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "delete",
    uuid: actionId
  });

  const issueFiltersSearchListState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "list",
    uuid: NAME_SEARCH_ID
  });

  const issueFilterCreateCloneState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "create",
    uuid: "0"
  });

  const issueFiltersUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "update",
    uuid: actionId
  });

  const title = getIssueFilterNameFromId(filterName) || "Issues";

  const debouncedSearch = useCallback(
    debounce((v: string) => {
      fetchIssueList(v);
      setSearchValue(v);
    }, 250),
    []
  );

  const handleSearchChange = useCallback((v: string) => {
    debouncedSearch(v);
  }, []);

  const fetchIssueList = useCallback((search: string) => {
    dispatch(listIssueFilter({ filter: { partial_match: { name: { $contains: search } } } }));
  }, []);

  useEffect(() => {
    // preventing api call if data is already loaded
    const data = get(issueFilterListState, "data", {});
    if (Object.keys(data).length === 0) {
      fetchIssueList("");
    }
  }, []);

  useEffect(() => {
    // skipping the first render
    if (!searching && refresh !== 0) {
      fetchIssueList(searchValue);
      setRefreshing(true);
    }
  }, [refresh]);

  useEffect(() => {
    const count = get(issuesList, ["data", "_metadata", "total_count"], 0);
    if (recordCount !== count) {
      setRecordCount(count);
    }
  }, [issuesList]);

  useEffect(() => {
    if (
      issueFilterListState &&
      (loadingFilterList ||
        searching ||
        refreshing ||
        get(issueFilterListState, ["data", "records"], []).length !== filtersList.length)
    ) {
      const loading = get(issueFilterListState, "loading", true);
      const error = get(issueFilterListState, "error", true);
      if (!loading && !error) {
        let data = get(issueFilterListState, ["data", "records"], []);
        data = data.map((item: any) => ({
          id: item.name,
          name: getIssueFilterNameFromId(item.name),
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
  }, [issueFilterListState]);

  const handleDelete = useCallback((name: string) => {
    setActionId(name);
    setDeleting(true);
    dispatch(deleteIssueFilter(name));
    setRefresh(refresh => refresh + 1);
  }, []);

  useEffect(() => {
    if (deleting) {
      const loading = get(issueFilterDeleteState, "loading", true);
      const error = get(issueFilterDeleteState, "error", true);
      if (!loading && !error) {
        handleItemDelete && handleItemDelete(actionId, filtersList);
        setDeleting(false);
        notification.success({ message: `${getIssueFilterNameFromId(actionId)} deleted` });
        setRefresh((value: number) => value + 1);
        setActionId("");
      }
    }
  }, [issueFilterDeleteState, filtersList]);

  const initialFormValue: EditCreateModalPayload = {
    name: "",
    description: "",
    default: Object.keys(createFilterWithData || {}).length > 0 || (filtersList.length === 0 && !searchValue),
    data: {
      filter: {}
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
      const loading = get(issueFiltersUpdateState, "loading", true);
      const error = get(issueFiltersUpdateState, "error", true);
      if (!loading && !error) {
        let message = `${getIssueFilterNameFromId(actionId)} saved`;
        if (updatingDefault) {
          message = `${getIssueFilterNameFromId(actionId)} set as default`;
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
  }, [issueFiltersUpdateState]);

  const handleCreateModalOk = useCallback(
    (item: EditCreateModalPayload | undefined) => {
      let _item: any = {
        is_default: !!item?.default,
        name: item?.name.toLowerCase().replace(/ /g, "-") || "",
        filter: { ...(item?.data?.filter || {}) },
        description: item?.description || ""
      };

      if (actionId) {
        dispatch(updateIssueFilter(actionId, { ..._item }));
        setUpdating(true);
      } else {
        if (createFilterWithData && Object.keys(createFilterWithData || {}).length > 0) {
          _item = { ..._item, filter: createFilterWithData };
          doCreateAction && doCreateAction();
        }
        setActionId(_item.name);
        dispatch(createIssueFilter(_item));
        setCreating(true);
      }
      setActionData(_item);
    },
    [actionId, createFilterWithData]
  );

  const handleEditClick = useCallback((name: string, item: DropDownListItem) => {
    const data: EditCreateModalPayload = {
      name: getIssueFilterNameFromId(item.name),
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
        listIssueFilter(
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
      const loading = get(issueFiltersSearchListState, "loading", true);
      const error = get(issueFiltersSearchListState, "error", true);
      if (!loading && !error) {
        setNameSearching(false);
        const data = get(issueFiltersSearchListState, ["data", "records"], []);
        setNameExist(
          !!data?.filter((item: any) => {
            const _searchName = searchName?.toLowerCase().replace(/ /g, "-");
            return item?.name?.toLowerCase() === _searchName;
          })?.length || false
        );
      }
    }
  }, [issueFiltersSearchListState]);

  useEffect(() => {
    if (cloning || creating) {
      const loading = get(issueFilterCreateCloneState, "loading", true);
      const error = get(issueFilterCreateCloneState, "error", true);
      if (!loading && !error) {
        const data = { ...actionData, name: actionId };
        if (cloning) {
          setCloning(false);
          handleCloneItem && handleCloneItem({ ...data, name: cloneId });
          notification.success({
            message: `${getIssueFilterNameFromId(actionId)} cloned as ${getIssueFilterNameFromId(cloneId)}`
          });
          setCloneId("");
        } else {
          handleCreateItem && handleCreateItem(actionId, data, "create");
          setShowCreateModal(false);
          setCreating(false);
          notification.success({ message: `${getIssueFilterNameFromId(actionId)} created` });
          setRefresh(refresh => refresh + 1);
        }
        setActionData({});
        setActionId("");
      }
    }
  }, [issueFilterCreateCloneState]);

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
    dispatch(createIssueFilter(payload));
    setCloning(true);
  };

  const handleDefaultClick = useCallback(
    (name: string, item: DropDownListItem) => {
      const payload = {
        description: item?.data.description,
        filter: item?.data.filter,
        is_default: true
      };
      dispatch(updateIssueFilter(name, payload));
      setActionId(name);
      setUpdating(true);
      setUpdatingDefault(true);
    },
    [handleDefaultChange, filterName]
  );

  const formInfo: EditCreateModalFormInfo = {
    namePlaceholder: "Issue Filter Name",
    descriptionPlaceholder: "Issue Filter Description",
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
        addButtonText={"Create Issue Filter"}
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
        title={actionId ? "Edit Issue Filter" : "Create Issue Filter"}
        disableNameEdit={!!actionId}
      />
      <EditCloneModal
        visible={showCloneModal}
        title={"Clone Issue Filter"}
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
