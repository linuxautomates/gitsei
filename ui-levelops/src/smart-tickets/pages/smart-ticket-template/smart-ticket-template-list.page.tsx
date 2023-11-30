import { useDispatch, useSelector } from "react-redux";
import ErrorWrapper from "hoc/errorWrapper";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { checkTemplateNameExists } from "../../../configurations/helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR } from "../../../constants/formWarnings";
import { EditCloneModal, TableRowActions } from "shared-resources/components";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";
import { RouteComponentProps } from "react-router-dom";
import { notification } from "antd";
import { v1 as uuid } from "uuid";
import { templateTableColumns } from "./table-config";
import Loader from "components/Loader/Loader";
import { debounce, get } from "lodash";
import { ServerPaginatedTable } from "shared-resources/containers";
import { RestSmartTicketTemplate } from "../../../classes/RestSmartTicketTemplate";
import { SmartTicketSelectorState } from "reduxConfigs/selectors/restapiSelector";
import {
  genericList,
  restapiClear,
  smartTicketTemplatesCreate,
  smartTicketTemplatesDelete,
  smartTicketTemplatesBulkDelete
} from "reduxConfigs/actions/restapi";
import { appendErrorMessagesHelper } from "utils/arrayUtils";

const SmartTicketTemplatesList: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const [checkingName, setCheckingName] = useState<boolean>(false);
  const [openEditCloneModel, setOpenEditCloneModel] = useState<boolean>(false);
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [cloneTemplateLoading, setCloneTemplateLoading] = useState<boolean>(false);
  const [cloningTemplate, setCloningTemplate] = useState<boolean>(false);
  const cloneTempName = useRef<any>("");

  const [checkNameListId, setCheckNameListId] = useState<any>();
  const [dataToClone, setDataToClone] = useState<any>(undefined);
  const [nameExists, setNameExists] = useState<any>(undefined);
  const [deleteTemplateIdState, setDeleteTemplateIdState] = useState<any>(undefined);
  const moreFilters = useRef({});
  const partialFilters = useRef({});
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [rowSelection, setRowSelection] = useState<any>({});
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState<number>(1);

  const debounceCheckName = useCallback(
    // preserve the same instance on all re-renders
    debounce(() => {
      checkTemplateName();
    }, 500),
    []
  );

  const dispatch = useDispatch();
  const apiState = useSelector(state => SmartTicketSelectorState(state));

  useEffect(() => {
    if (checkingName) {
      const loading = apiState?.list[checkNameListId]?.loading;
      const error = apiState?.list[checkNameListId]?.error;
      if (!loading && !error) {
        const data = apiState?.list[checkNameListId]?.data.records;
        const prevName = cloneTempName.current;
        dispatch(restapiClear("ticket_templates", "list", checkNameListId));
        setCheckingName(false);
        setCheckNameListId(undefined);
        checkTemplateNameExists(prevName, data) ? setNameExists(NAME_EXISTS_ERROR) : setNameExists(undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [checkingName, apiState?.list?.[checkNameListId]]);

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = apiState.bulkDelete["0"];
      if (!loading) {
        if (!error) {
          const data = get(apiState, ["bulkDelete", "0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }

          if (errorOccurs) {
            setReload(state => state + 1);
          } else {
            setSelectedIds([]);
            setReload(state => state + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.bulkDelete]);

  useEffect(() => {
    debounceCheckName();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cloneTempName]);

  useEffect(() => {
    if (deleteLoading) {
      const delete_template_id = deleteTemplateIdState;
      const loading = apiState?.delete[delete_template_id]?.loading;
      const error = apiState?.delete[delete_template_id]?.error;
      if (!loading) {
        if (!error) {
          const data = get(apiState, ["delete", delete_template_id, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            setSelectedIds((ids: string[]) => ids.filter(id => id !== delete_template_id));
          }
        }
        setDeleteLoading(false);
        setDeleteTemplateIdState(undefined);
      }
      return;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.delete]);

  useEffect(() => {
    if (cloneTemplateLoading && dataToClone) {
      const { id, questionnaire_templates, ...restData } = dataToClone;

      const newTemplate = new RestSmartTicketTemplate({
        ...restData,
        id: undefined,
        name: cloneTempName.current
      });

      let newTemplateQuestionnaire: any[] = [];

      questionnaire_templates.forEach((questionnaire: any) => {
        newTemplateQuestionnaire = [
          {
            key: questionnaire.questionnaire_template_id,
            label: questionnaire.name
          }
        ];
      });

      newTemplate.questionnaires_select = newTemplateQuestionnaire;
      dispatch(smartTicketTemplatesCreate(newTemplate));
      setCloneTemplateLoading(false);
      setCloningTemplate(true);
      cloneTempName.current = "";
      setDataToClone(undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cloneTemplateLoading, dataToClone]);

  useEffect(() => {
    if (cloningTemplate) {
      const loading = apiState?.create?.loading;
      const error = apiState?.create?.error;
      if (!loading && !error) {
        const newSmartTemplate = apiState?.create?.data;
        notification.success({
          message: "Clone template",
          description: "Template cloned successfully"
        });

        props.history.push(
          `${getBaseUrl()}${TEMPLATE_ROUTES.ISSUE_TEMPLATE.EDIT}?template=${newSmartTemplate.ticket_template_id}`
        );

        return () => {
          setCloningTemplate(false);
        };
      }
      return;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.create]);

  useEffect(() => {
    return () => {
      // @ts-ignore
      dispatch(restapiClear("ticket_templates", "list", "0"));
      // @ts-ignore
      dispatch(restapiClear("ticket_templates", "delete", "-1"));
      // @ts-ignore
      dispatch(restapiClear("ticket_templates", "bulkDelete", "-1"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onRemoveHandler(templateId: any) {
    setDeleteTemplateIdState(templateId);
    setDeleteLoading(true);
    dispatch(smartTicketTemplatesDelete(templateId));
  }

  function buildActionOptions(props: any) {
    const actions =
      props.default !== true
        ? [
            {
              type: "copy",
              description: "Clone",
              id: props.id,
              onClickEvent: dataToCloneHandler
            },
            {
              type: "delete",
              id: props.id,
              onClickEvent: onRemoveHandler
            }
          ]
        : [];
    return <TableRowActions actions={actions} />;
  }

  function onCancelEditCloneModal() {
    setOpenEditCloneModel(false);
    setDataToClone(undefined);
    setCheckNameListId(undefined);
    setNameExists(undefined);
  }

  function onOkEditCloneModal(name: string) {
    notification.success({
      message: "Clone template",
      description: "Starting cloning the template..."
    });
    cloneTempName.current = name;
    setCloneTemplateLoading(true);
    setOpenEditCloneModel(false);
    debounceCheckName();
  }

  function dataToCloneHandler(id: any) {
    const apiData = apiState?.list[0]?.data?.records;

    let cloneData = undefined;

    if (apiData && apiData.length > 0) {
      cloneData = apiData.find((item: any) => item.id === id);
    }
    setDataToClone(cloneData);
    setOpenEditCloneModel(true);
  }

  function checkTemplateName() {
    if (cloneTempName.current === "") {
      return;
    }
    const filters = {
      filter: {
        partial: {
          name: cloneTempName.current
        }
      }
    };
    const checkNameListId1 = uuid();
    dispatch(genericList("ticket_templates", "list", filters, null, checkNameListId1));
    setCheckNameListId(checkNameListId1);
    setCheckingName(true);
  }

  function onSearchEvent(name: string) {
    cloneTempName.current = name;
    debounceCheckName();
  }
  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  useEffect(() => {
    setRowSelection({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange
    });
  }, [selectedIds]);

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const mappedColumns = templateTableColumns().map(column => {
    if (column.key === "id") {
      return {
        ...column,
        render: (item: any, record: any, index: any) => buildActionOptions(record)
      };
    }
    return column;
  });

  const onBulkDelete = () => {
    dispatch(smartTicketTemplatesBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  return deleteLoading ? (
    <Loader />
  ) : (
    <>
      <ServerPaginatedTable
        pageName={"stt_list"}
        uri="ticket_templates"
        method={"list"}
        moreFilters={moreFilters}
        partialFilters={partialFilters}
        columns={mappedColumns}
        hasFilters={false}
        clearSelectedIds={clearSelectedIds}
        rowSelection={rowSelection}
        onBulkDelete={onBulkDelete}
        reload={reload}
        hasDelete={true}
        bulkDeleting={bulkDeleting}
      />
      <EditCloneModal
        visible={openEditCloneModel}
        title={"Clone Template"}
        onOk={onOkEditCloneModal}
        onCancel={onCancelEditCloneModal}
        nameExists={!!nameExists}
        searchEvent={onSearchEvent}
      />
    </>
  );
};

export default ErrorWrapper(SmartTicketTemplatesList);
